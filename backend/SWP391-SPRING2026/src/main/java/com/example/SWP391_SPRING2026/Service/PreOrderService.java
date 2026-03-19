package com.example.SWP391_SPRING2026.Service;

import com.example.SWP391_SPRING2026.Entity.*;
import com.example.SWP391_SPRING2026.Enum.*;
import com.example.SWP391_SPRING2026.Exception.BadRequestException;
import com.example.SWP391_SPRING2026.Repository.PreOrderRepository;
import com.example.SWP391_SPRING2026.Repository.ProductVariantRepository;


import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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


    /*
        CHECK AVAILABILITY
     */
    public VariantAvailabilityStatus resolveAvailability(ProductVariant variant) {
        return VariantAvailabilityResolver.resolve(variant);
    }

    /*
        RESERVE PREORDER SLOT
     */
    public void reserve(Order order, OrderItems item, ProductVariant variant, int quantity) {

        if (resolveAvailability(variant) != VariantAvailabilityStatus.PRE_ORDER) {
            throw new BadRequestException("Pre-order is not available");
        }

        int current = variant.getCurrentPreorders() == null ? 0 : variant.getCurrentPreorders();
        int limit = variant.getPreorderLimit() == null ? 0 : variant.getPreorderLimit();

        if (current + quantity > limit) {
            throw new BadRequestException("Preorder limit exceeded");
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
    public void markStockArrived(Long variantId, int quantity) {

        ProductVariant variant = variantRepository.lockById(variantId)
                .orElseThrow(() -> new BadRequestException("Variant not found"));

        int stock = variant.getStockQuantity() == null ? 0 : variant.getStockQuantity();

        variant.setStockQuantity(stock + quantity);

        allocateAvailableStock(variantId);
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

        Map<Long, Order> affectedOrders = new LinkedHashMap<>();

        for (PreOrder line : queue) {

            if (stock < line.getQuantity()) {
                break;
            }

            stock -= line.getQuantity();
            variant.setStockQuantity(stock);

            int current = variant.getCurrentPreorders() == null ? 0 : variant.getCurrentPreorders();
            variant.setCurrentPreorders(Math.max(0, current - line.getQuantity()));

            line.setAllocatedStock(true);

            Order order = line.getOrder();
            affectedOrders.put(order.getId(), order);

            long remaining = order.getRemainingAmount() == null ? 0 : order.getRemainingAmount();

            if (remaining > 0) {
                line.setPreorderStatus(PreOrderStatus.AWAITING_REMAINING_PAYMENT);
            } else {
                line.setPreorderStatus(PreOrderStatus.READY_FOR_PROCESSING);
            }

            preOrderRepository.save(line);
        }

        variantRepository.save(variant);

        for (Order affectedOrder : affectedOrders.values()) {
            refreshPreOrderOrderStatus(affectedOrder);
        }
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
    public void cancelPreOrderOrder(Order order) {

        List<PreOrder> lines = preOrderRepository.findByOrder_Id(order.getId());

        for (PreOrder line : lines) {

            if (line.getPreorderStatus() == PreOrderStatus.CANCELLED
                    || line.getPreorderStatus() == PreOrderStatus.FULFILLED) {
                continue;
            }

            ProductVariant variant = variantRepository.lockById(line.getProductVariant().getId())
                    .orElseThrow(() -> new BadRequestException("Variant not found"));

            if (line.getPreorderStatus() == PreOrderStatus.RESERVED
                    || line.getPreorderStatus() == PreOrderStatus.AWAITING_STOCK) {

                int current = variant.getCurrentPreorders() == null ? 0 : variant.getCurrentPreorders();
                variant.setCurrentPreorders(Math.max(0, current - line.getQuantity()));

                line.setSlotReleased(true);
                line.setPreorderStatus(PreOrderStatus.CANCELLED);
                continue;
            }

            if (line.getPreorderStatus() == PreOrderStatus.AWAITING_REMAINING_PAYMENT
                    || line.getPreorderStatus() == PreOrderStatus.READY_FOR_PROCESSING
                    || line.getPreorderStatus() == PreOrderStatus.READY_TO_SHIP) {

                int stock = variant.getStockQuantity() == null ? 0 : variant.getStockQuantity();
                variant.setStockQuantity(stock + line.getQuantity());

                line.setPreorderStatus(PreOrderStatus.CANCELLED);
            }
        }
    }

    public void refreshPreOrderOrderStatus(Order order) {
        List<PreOrder> lines = preOrderRepository.findByOrder_Id(order.getId());

        if (lines == null || lines.isEmpty()) {
            return;
        }

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
    public boolean isReadyForOperation(Order order) {
        List<PreOrder> lines = preOrderRepository.findByOrder_Id(order.getId());

        if (lines == null || lines.isEmpty()) {
            return false;
        }

        return lines.stream()
                .allMatch(line -> line.getPreorderStatus() == PreOrderStatus.READY_FOR_PROCESSING);
    }



}