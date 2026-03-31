package com.example.SWP391_SPRING2026.Service;

import com.example.SWP391_SPRING2026.Entity.*;
import com.example.SWP391_SPRING2026.Enum.*;
import com.example.SWP391_SPRING2026.Exception.BadRequestException;
import com.example.SWP391_SPRING2026.Repository.PreOrderCampaignRepository;
import com.example.SWP391_SPRING2026.Repository.PreOrderRepository;
import com.example.SWP391_SPRING2026.Repository.ProductVariantRepository;


import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Ref;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import com.example.SWP391_SPRING2026.Utility.VariantAvailabilityResolver;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class PreOrderService {

    private final PreOrderRepository preOrderRepository;
    private final ProductVariantRepository variantRepository;
    private final PreOrderCampaignRepository preOrderCampaignRepository;


    /*
        CHECK AVAILABILITY
     */
    public VariantAvailabilityStatus resolveAvailability(ProductVariant variant) {
        if (variant == null) {
            return VariantAvailabilityStatus.OUT_OF_STOCK;
        }

        boolean pendingQueue = hasPendingStockQueue(variant.getId());
        if (pendingQueue) {
            return VariantAvailabilityStatus.PRE_ORDER;
        }

        int stock = variant.getStockQuantity() == null ? 0 : variant.getStockQuantity();
        if (stock > 0) {
            return VariantAvailabilityStatus.IN_STOCK;
        }

        boolean hasActiveCampaign = !preOrderCampaignRepository
                .findActiveCampaignsForVariant(variant.getId(), LocalDate.now())
                .isEmpty();

        if (hasActiveCampaign) {
            return VariantAvailabilityStatus.PRE_ORDER;
        }

        return VariantAvailabilityResolver.resolve(variant);
    }

    /*
        RESERVE PREORDER SLOT
     */
    @Transactional
    public void reserve(Order order, OrderItems item, Long variantId, int quantity) {
        ProductVariant variant = variantRepository.lockById(variantId)
                .orElseThrow(() -> new BadRequestException("Variant not found"));

        if (resolveAvailability(variant) != VariantAvailabilityStatus.PRE_ORDER) {
            throw new BadRequestException("Pre-order is not available");
        }

        int current = variant.getCurrentPreorders() == null ? 0 : variant.getCurrentPreorders();
        Integer limitValue = variant.getPreorderLimit();

        int remainingSlots = (limitValue == null)
                ? Integer.MAX_VALUE
                : Math.max(0, limitValue - current);

        if (quantity > remainingSlots) {
            throw new BadRequestException("Not enough preorder slots");
        }

        variant.setCurrentPreorders(current + quantity);

        PreOrder po = new PreOrder();
        po.setOrder(order);
        po.setOrderItem(item);
        po.setProductVariant(variant);
        po.setQuantity(quantity);
        po.setExpectedReleaseDate(variant.getPreorderFulfillmentDate());
        po.setPreorderStatus(PreOrderStatus.RESERVED);
        po.setReservedAt(LocalDateTime.now());
        po.setAllocatedStock(false);
        po.setSlotReleased(false);

        preOrderRepository.save(po);
    }

    /*
        AFTER DEPOSIT PAYMENT
     */
    public void markInitialPaymentSuccess(Long orderId) {

        List<PreOrder> lines = preOrderRepository.findByOrder_Id(orderId);

        for (PreOrder line : lines) {

            if (line.getPreorderStatus() == PreOrderStatus.RESERVED) {

                line.setPreorderStatus(PreOrderStatus.AWAITING_STOCK);
            }
        }
    }

    /*
        STOCK ARRIVED
     */
    @Transactional
    public void markStockArrived(Long variantId, int quantity) {
        ProductVariant variant = variantRepository.lockById(variantId)
                .orElseThrow(() -> new BadRequestException("Variant not found"));

        int stock = variant.getStockQuantity() == null ? 0 : variant.getStockQuantity();
        variant.setStockQuantity(stock + quantity);

        allocateAvailableStock(variantId);
        convertToInStockIfReady(variantId);
    }

    /*
        FIFO ALLOCATION
     */
    @Transactional
    public void allocateAvailableStock(Long variantId) {

        ProductVariant variant = variantRepository.lockById(variantId)
                .orElseThrow(() -> new BadRequestException("Variant not found"));

        int stock = variant.getStockQuantity() == null ? 0 : variant.getStockQuantity();

        List<PreOrder> queue = preOrderRepository.lockQueueByVariant(
                variantId,
                EnumSet.of(PreOrderStatus.AWAITING_STOCK)
        );

        for (PreOrder line : queue) {

            if (stock < line.getQuantity()) break;

            stock -= line.getQuantity();
            variant.setStockQuantity(stock);

            // 🔥 giảm current preorder
            int current = variant.getCurrentPreorders() == null ? 0 : variant.getCurrentPreorders();
            variant.setCurrentPreorders(Math.max(0, current - line.getQuantity()));

            line.setAllocatedStock(true);

            if (line.getOrder().getRemainingAmount() > 0) {
                line.setPreorderStatus(PreOrderStatus.AWAITING_REMAINING_PAYMENT);
            } else {
                line.setPreorderStatus(PreOrderStatus.READY_FOR_PROCESSING);
            }

            preOrderRepository.save(line);
        }

        variantRepository.save(variant);
    }

    /*
        CUSTOMER PAY REMAINING
     */
    public void markRemainingPaid(Long orderId) {
        List<PreOrder> lines = preOrderRepository.findByOrder_Id(orderId);

        if (lines == null || lines.isEmpty()) {
            throw new RuntimeException("No preorder lines found for order " + orderId);
        }

        Order order = lines.get(0).getOrder();

        for (PreOrder line : lines) {
            if (line.getPreorderStatus() == PreOrderStatus.AWAITING_REMAINING_PAYMENT) {
                line.setPreorderStatus(PreOrderStatus.READY_FOR_PROCESSING);
            }
        }

        refreshPreOrderOrderStatus(order);
    }

    /*
        VALIDATE READY TO SHIP
     */
    public void validateReadyToShip(Order order) {
        List<PreOrder> lines = preOrderRepository.findByOrder_Id(order.getId());

        if (lines == null || lines.isEmpty()) {
            throw new BadRequestException("No preorder lines found for order " + order.getId());
        }

        for (PreOrder line : lines) {
            if (line.getPreorderStatus() != PreOrderStatus.READY_FOR_PROCESSING) {
                throw new BadRequestException("Preorder not ready for shipping");
            }
        }
    }

    /*
        OPERATION READY
     */
    @Transactional
    public void markReadyToShip(Order order) {
        List<PreOrder> lines = preOrderRepository.findByOrder_Id(order.getId());

        if (lines == null || lines.isEmpty()) {
            throw new RuntimeException("No preorder lines found for order " + order.getId());
        }

        for (PreOrder line : lines) {
            line.setPreorderStatus(PreOrderStatus.READY_TO_SHIP);
        }
    }

    /*
        DELIVERY SUCCESS
     */
    public void markFulfilled(Order order) {

        List<PreOrder> lines = preOrderRepository.findByOrder_Id(order.getId());

        for (PreOrder line : lines) {

            line.setPreorderStatus(PreOrderStatus.FULFILLED);
        }

        order.setOrderStatus(OrderStatus.COMPLETED);
    }

    public boolean isRemainingPaymentOpened(Long orderId) {

        List<PreOrder> lines = preOrderRepository.findByOrder_Id(orderId);

        if (lines == null || lines.isEmpty()) {
            return false;
        }

        Order order = lines.get(0).getOrder();
        long remaining = order.getRemainingAmount() == null ? 0L : order.getRemainingAmount();

        if (remaining <= 0) {
            return false;
        }

        return lines.stream()
                .anyMatch(p -> p.getPreorderStatus() ==
                        PreOrderStatus.AWAITING_REMAINING_PAYMENT);
    }

    public void releaseReservations(Order order) {

        List<PreOrder> lines = preOrderRepository.findByOrder_Id(order.getId());

        for (PreOrder line : lines) {

            if (line.getPreorderStatus() == PreOrderStatus.RESERVED
                    || line.getPreorderStatus() == PreOrderStatus.AWAITING_STOCK) {

                ProductVariant variant = line.getProductVariant();

                int current = variant.getCurrentPreorders() == null ? 0 : variant.getCurrentPreorders();

                int newValue = Math.max(0, current - line.getQuantity());

                variant.setCurrentPreorders(newValue);

                line.setPreorderStatus(PreOrderStatus.CANCELLED);

                line.setSlotReleased(true);
            }
        }
    }

    public boolean isFullRefundEligible(Order order) {

        List<PreOrder> lines = preOrderRepository.findByOrder_Id(order.getId());

        if (lines.isEmpty()) {
            return false;
        }

        LocalDate today = LocalDate.now();

        return lines.stream().allMatch(line -> {
            ProductVariant variant = line.getProductVariant();

            if (variant == null || variant.getPreorderEndDate() == null) {
                return false;
            }

            LocalDate deadline = variant.getPreorderEndDate().minusDays(2);
            return !today.isAfter(deadline);
        });
    }

    @Transactional
    public void cancelPreOrderOrder(Order order) {

        List<PreOrder> lines = preOrderRepository.findByOrder_Id(order.getId());

        if(lines == null || lines.isEmpty()) {
            throw new BadRequestException("No preorder lines found for order " + order.getId());
        }

        LocalDate today = LocalDate.now();

        for (PreOrder line : lines) {

            if (line.getPreorderStatus() == PreOrderStatus.CANCELLED
                    || line.getPreorderStatus() == PreOrderStatus.FULFILLED) {
                continue;
            }

            ProductVariant variant = variantRepository.lockById(line.getProductVariant().getId())
                    .orElseThrow(() -> new BadRequestException("Variant not found"));

            int quantity = line.getQuantity();

            /*
            CASE 1: CHƯA ALLOCATE STOCK → TRẢ SLOT
         */
            if (line.getPreorderStatus() == PreOrderStatus.RESERVED
                    || line.getPreorderStatus() == PreOrderStatus.AWAITING_STOCK) {

                int current = variant.getCurrentPreorders() == null ? 0 : variant.getCurrentPreorders();
                variant.setCurrentPreorders(Math.max(0, current - quantity));

                line.setSlotReleased(true);
            }

            /*
            CASE 2: ĐÃ ALLOCATE → TRẢ STOCK
         */


            else if (line.getPreorderStatus() == PreOrderStatus.AWAITING_REMAINING_PAYMENT
                    || line.getPreorderStatus() == PreOrderStatus.READY_FOR_PROCESSING
                    || line.getPreorderStatus() == PreOrderStatus.READY_TO_SHIP) {

                int stock = variant.getStockQuantity() == null ? 0 : variant.getStockQuantity();
                variant.setStockQuantity(stock + line.getQuantity());
            }
            /*
            REFUND LOGIC
         */

            RefundPolicy refundPolicy = resolveRefundPolicy(line, today);

            line.setRefundPolicy(refundPolicy);

            if(refundPolicy == RefundPolicy.FULL_REFUND){
                line.setRefundAmount(line.getOrderItem().getPaidAmount());
            }else{
                line.setRefundAmount(0L);
            }

            line.setPreorderStatus(PreOrderStatus.CANCELLED);

            variantRepository.save(variant);
            preOrderRepository.save(line);
        }
        refreshPreOrderOrderStatus(order);
    }

    @Transactional
    public void cancelBySupport(Order order,
                                Long staffId,
                                String reason,
                                boolean forceFullRefund) {

        List<PreOrder> lines = preOrderRepository.findByOrder_Id(order.getId());

        if (lines == null || lines.isEmpty()) {
            throw new BadRequestException("No preorder lines found");
        }

        for (PreOrder line : lines) {

            if (line.getPreorderStatus() == PreOrderStatus.CANCELLED
                    || line.getPreorderStatus() == PreOrderStatus.FULFILLED) {
                continue;
            }

            ProductVariant variant = variantRepository.lockById(
                    line.getProductVariant().getId()
            ).orElseThrow(() -> new BadRequestException("Variant not found"));

            int qty = line.getQuantity();

        /*
            1. TRẢ SLOT nếu chưa allocate
         */
            if (line.getPreorderStatus() == PreOrderStatus.RESERVED
                    || line.getPreorderStatus() == PreOrderStatus.AWAITING_STOCK) {

                int current = variant.getCurrentPreorders() == null ? 0 : variant.getCurrentPreorders();
                variant.setCurrentPreorders(Math.max(0, current - qty));

                line.setSlotReleased(true);
            }

        /*
            2. TRẢ STOCK nếu đã allocate
         */
            else {
                int stock = variant.getStockQuantity() == null ? 0 : variant.getStockQuantity();
                variant.setStockQuantity(stock + qty);
            }

        /*
            3. REFUND (SUPPORT LUÔN CÓ QUYỀN OVERRIDE)
         */
            if (forceFullRefund) {
                line.setRefundPolicy(RefundPolicy.FULL_REFUND);
                line.setRefundAmount(line.getOrderItem().getPaidAmount());
            } else {
                RefundPolicy policy = resolveRefundPolicy(line, LocalDate.now());
                line.setRefundPolicy(policy);

                if (policy == RefundPolicy.FULL_REFUND) {
                    long paidAmount = line.getOrderItem().getPaidAmount() == null
                            ? 0L
                            : line.getOrderItem().getPaidAmount();

                    line.setRefundAmount(paidAmount);
                } else {
                    line.setRefundAmount(0L);
                }
            }

        /*
            4. AUDIT
         */
            line.setCancelledAt(LocalDateTime.now());


        /*
            5. STATUS
         */
            line.setPreorderStatus(PreOrderStatus.CANCELLED);

            variantRepository.save(variant);
            preOrderRepository.save(line);
        }
    }

    public RefundPolicy resolveRefundPolicy(PreOrder line, LocalDate today) {

        Order order = line.getOrder();

        // 🔥 FIX QUAN TRỌNG
        boolean isFullyPaid = order.getRemainingAmount() == null
                || order.getRemainingAmount() == 0;

        if (isFullyPaid) {
            return RefundPolicy.FULL_REFUND;
        }

        ProductVariant variant = line.getProductVariant();

        if (variant == null || variant.getPreorderEndDate() == null) {
            return RefundPolicy.DEPOSIT_FORFEIT;
        }

        LocalDate deadline = variant.getPreorderEndDate().minusDays(2);

        switch (line.getPreorderStatus()) {

            case RESERVED:
                return RefundPolicy.FULL_REFUND;

            case AWAITING_STOCK:
                return !today.isAfter(deadline)
                        ? RefundPolicy.FULL_REFUND
                        : RefundPolicy.DEPOSIT_FORFEIT;

            case AWAITING_REMAINING_PAYMENT:
                return RefundPolicy.DEPOSIT_FORFEIT;

            default:
                return RefundPolicy.DEPOSIT_FORFEIT;
        }
    }

    public void refreshPreOrderOrderStatus(Order order) {
        List<PreOrder> lines = preOrderRepository.findByOrder_Id(order.getId());

        if (lines == null || lines.isEmpty()) {
            return;
        }

        long remaining = order.getRemainingAmount() == null ? 0L : order.getRemainingAmount();

        boolean anyAwaitingRemaining = lines.stream()
                .anyMatch(line -> line.getPreorderStatus() == PreOrderStatus.AWAITING_REMAINING_PAYMENT);

        boolean anyAwaitingStock = lines.stream()
                .anyMatch(line ->
                        line.getPreorderStatus() == PreOrderStatus.RESERVED
                                || line.getPreorderStatus() == PreOrderStatus.AWAITING_STOCK);

        boolean allReadyForProcessing = lines.stream()
                .allMatch(line ->
                        line.getPreorderStatus() == PreOrderStatus.READY_FOR_PROCESSING
                                || line.getPreorderStatus() == PreOrderStatus.READY_TO_SHIP
                                || line.getPreorderStatus() == PreOrderStatus.FULFILLED);

        if (remaining <= 0) {
            if (order.getApprovalStatus() == ApprovalStatus.SUPPORT_APPROVED) {
                order.setOrderStatus(OrderStatus.SUPPORT_CONFIRMED);
            } else {
                order.setOrderStatus(OrderStatus.CONFIRMED);
            }
            return;
        }

        if (anyAwaitingRemaining) {
            order.setOrderStatus(OrderStatus.PENDING_PAYMENT);
            return;
        }

        if (allReadyForProcessing) {
            if (order.getApprovalStatus() == ApprovalStatus.SUPPORT_APPROVED) {
                order.setOrderStatus(OrderStatus.SUPPORT_CONFIRMED);
            } else {
                order.setOrderStatus(OrderStatus.CONFIRMED);
            }
            return;
        }

        if (anyAwaitingStock) {
            order.setOrderStatus(OrderStatus.PAID);
        }
    }

    public int getAvailablePreorder(ProductVariant v) {
        Integer limitValue = v.getPreorderLimit();
        int current = v.getCurrentPreorders() == null ? 0 : v.getCurrentPreorders();

        if (limitValue == null) {
            return Integer.MAX_VALUE;
        }

        return Math.max(0, limitValue - current);
    }

    public boolean isReadyForOperation(Order order) {
        List<PreOrder> lines = preOrderRepository.findByOrder_Id(order.getId());

        if (lines == null || lines.isEmpty()) {
            return false;
        }

        return lines.stream().allMatch(line ->
                line.getPreorderStatus() == PreOrderStatus.READY_FOR_PROCESSING
                        || line.getPreorderStatus() == PreOrderStatus.READY_TO_SHIP
                        || line.getPreorderStatus() == PreOrderStatus.FULFILLED
        );
    }

    @Transactional
    public void convertToInStock(Long variantId) {

        ProductVariant variant = variantRepository.lockById(variantId)
                .orElseThrow(() -> new BadRequestException("Variant not found"));

        int stock = variant.getStockQuantity() == null ? 0 : variant.getStockQuantity();
        int current = variant.getCurrentPreorders() == null ? 0 : variant.getCurrentPreorders();

        // 🔥 LOGIC CHUẨN
        int realStock = stock - current;
        
        variant.setCurrentPreorders(0);
        variant.setSaleType(SaleType.IN_STOCK);

        variantRepository.save(variant);
    }
    private boolean hasPendingStockQueue(Long variantId) {
        return preOrderRepository.existsByProductVariant_IdAndPreorderStatusIn(
                variantId,
                EnumSet.of(PreOrderStatus.RESERVED, PreOrderStatus.AWAITING_STOCK)
        );
    }

    @Transactional
    public void convertToInStockIfReady(Long variantId) {
        ProductVariant variant = variantRepository.lockById(variantId)
                .orElseThrow(() -> new BadRequestException("Variant not found"));

        boolean pendingQueue = hasPendingStockQueue(variantId);
        int stock = variant.getStockQuantity() == null ? 0 : variant.getStockQuantity();

        if (variant.getSaleType() == SaleType.PRE_ORDER
                && stock > 0
                && !pendingQueue) {

            variant.setSaleType(SaleType.IN_STOCK);
            variant.setAllowPreorder(false);

            variantRepository.save(variant);
        }
    }

}
