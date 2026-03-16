package com.example.SWP391_SPRING2026.Service;

import com.example.SWP391_SPRING2026.Entity.*;
import com.example.SWP391_SPRING2026.Enum.*;
import com.example.SWP391_SPRING2026.Exception.BadRequestException;
import com.example.SWP391_SPRING2026.Repository.PreOrderRepository;
import com.example.SWP391_SPRING2026.Repository.ProductVariantRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

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

        int stock = variant.getStockQuantity() == null ? 0 : variant.getStockQuantity();

        if (variant.getSaleType() == SaleType.IN_STOCK) {

            return stock > 0 ?
                    VariantAvailabilityStatus.IN_STOCK :
                    VariantAvailabilityStatus.OUT_OF_STOCK;
        }

        if (variant.getSaleType() == SaleType.PRE_ORDER) {

            boolean allow = Boolean.TRUE.equals(variant.getAllowPreorder());

            int limit = variant.getPreorderLimit() == null ? 0 : variant.getPreorderLimit();
            int current = variant.getCurrentPreorders() == null ? 0 : variant.getCurrentPreorders();

            if (!allow || limit <= 0 || current >= limit) {
                return VariantAvailabilityStatus.OUT_OF_STOCK;
            }

            return VariantAvailabilityStatus.PRE_ORDER;
        }

        return VariantAvailabilityStatus.OUT_OF_STOCK;
    }

    /*
        RESERVE PREORDER SLOT
     */
    public void reserve(Order order, OrderItems item, ProductVariant variant, int quantity) {

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

        for (PreOrder line : queue) {

            if (stock < line.getQuantity()) {
                break;
            }

            stock -= line.getQuantity();
            variant.setStockQuantity(stock);

            int current = variant.getCurrentPreorders() == null ? 0 : variant.getCurrentPreorders();
            variant.setCurrentPreorders(current - line.getQuantity());

            line.setAllocatedStock(true);

            Order order = line.getOrder();

            long remaining = order.getRemainingAmount() == null ? 0 : order.getRemainingAmount();

            if (remaining > 0) {

                line.setPreorderStatus(PreOrderStatus.AWAITING_REMAINING_PAYMENT);
                order.setOrderStatus(OrderStatus.PENDING_PAYMENT);

            } else {

                line.setPreorderStatus(PreOrderStatus.READY_FOR_PROCESSING);
                order.setOrderStatus(OrderStatus.CONFIRMED);
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

        for (PreOrder line : lines) {

            if (line.getPreorderStatus() == PreOrderStatus.AWAITING_REMAINING_PAYMENT) {

                line.setPreorderStatus(PreOrderStatus.READY_FOR_PROCESSING);

                line.getOrder().setOrderStatus(OrderStatus.CONFIRMED);
            }
        }
    }

    /*
        VALIDATE READY TO SHIP
     */
    public void validateReadyToShip(Order order) {

        List<PreOrder> lines = preOrderRepository.findByOrder_Id(order.getId());

        for (PreOrder line : lines) {

            if (line.getPreorderStatus() != PreOrderStatus.READY_FOR_PROCESSING) {
                throw new BadRequestException("Preorder not ready for shipping");
            }
        }
    }

    /*
        OPERATION READY
     */
    public void markReadyToShip(Order order) {

        List<PreOrder> lines = preOrderRepository.findByOrder_Id(order.getId());

        for (PreOrder line : lines) {

            line.setPreorderStatus(PreOrderStatus.READY_TO_SHIP);
        }

        order.setOrderStatus(OrderStatus.OPERATION_CONFIRMED);
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
}