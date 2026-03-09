package com.example.SWP391_SPRING2026.Service;

import com.example.SWP391_SPRING2026.Entity.Order;
import com.example.SWP391_SPRING2026.Entity.OrderItems;
import com.example.SWP391_SPRING2026.Entity.PreOrder;
import com.example.SWP391_SPRING2026.Entity.ProductVariant;
import com.example.SWP391_SPRING2026.Enum.PreOrderStatus;
import com.example.SWP391_SPRING2026.Enum.RefundRequestStatus;
import com.example.SWP391_SPRING2026.Enum.VariantAvailabilityStatus;
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
    private final ProductVariantRepository productVariantRepository;
    private final EmailService emailService;

    public VariantAvailabilityStatus resolveAvailability(ProductVariant variant) {
        int stock = variant.getStockQuantity() == null ? 0 : variant.getStockQuantity();
        if (stock > 0) return VariantAvailabilityStatus.IN_STOCK;

        boolean allow = Boolean.TRUE.equals(variant.getAllowPreorder());
        int limit = variant.getPreorderLimit() == null ? 0 : variant.getPreorderLimit();
        int current = variant.getCurrentPreorders() == null ? 0 : variant.getCurrentPreorders();

        if (!allow) return VariantAvailabilityStatus.OUT_OF_STOCK;
        if (limit <= 0) return VariantAvailabilityStatus.OUT_OF_STOCK;
        if (current >= limit) return VariantAvailabilityStatus.OUT_OF_STOCK;
        if (variant.getPreorderFulfillmentDate() == null) return VariantAvailabilityStatus.OUT_OF_STOCK;

        return VariantAvailabilityStatus.PRE_ORDER;
    }

    public void reserve(Order order, OrderItems orderItem, ProductVariant variant, int quantity) {
        VariantAvailabilityStatus availability = resolveAvailability(variant);
        if (availability != VariantAvailabilityStatus.PRE_ORDER) {
            throw new BadRequestException("Variant is not available for preorder");
        }

        int current = variant.getCurrentPreorders() == null ? 0 : variant.getCurrentPreorders();
        int limit = variant.getPreorderLimit() == null ? 0 : variant.getPreorderLimit();

        if (current + quantity > limit) {
            throw new BadRequestException("Preorder limit exceeded");
        }

        variant.setCurrentPreorders(current + quantity);

        PreOrder preOrder = new PreOrder();
        preOrder.setOrder(order);
        preOrder.setOrderItem(orderItem);
        preOrder.setProductVariant(variant);
        preOrder.setQuantity(quantity);
        preOrder.setExpectedReleaseDate(variant.getPreorderFulfillmentDate());
        preOrder.setPreorderStatus(PreOrderStatus.RESERVED);
        preOrder.setReservedAt(LocalDateTime.now());
        preOrder.setSlotReleased(false);
        preOrder.setAllocatedStock(false);

        preOrderRepository.save(preOrder);
    }

    public void markInitialPaymentSuccess(Long orderId) {
        List<PreOrder> lines = preOrderRepository.findByOrder_Id(orderId);
        for (PreOrder line : lines) {
            if (line.getPreorderStatus() == PreOrderStatus.RESERVED) {
                line.setPreorderStatus(PreOrderStatus.AWAITING_STOCK);
            }
        }
    }

    public boolean isRemainingPaymentOpened(Long orderId) {
        List<PreOrder> lines = preOrderRepository.findByOrderIdAndStatuses(
                orderId,
                EnumSet.of(PreOrderStatus.AWAITING_REMAINING_PAYMENT)
        );
        return !lines.isEmpty();
    }

    public void markRemainingPaid(Long orderId) {
        List<PreOrder> lines = preOrderRepository.findByOrder_Id(orderId);
        for (PreOrder line : lines) {
            if (line.getPreorderStatus() == PreOrderStatus.AWAITING_REMAINING_PAYMENT) {
                line.setPreorderStatus(PreOrderStatus.READY_FOR_PROCESSING);
            }
        }
    }

    public void validateReadyToShip(Order order) {
        List<PreOrder> lines = preOrderRepository.findByOrder_Id(order.getId());
        for (PreOrder line : lines) {
            if (line.getPreorderStatus() != PreOrderStatus.READY_FOR_PROCESSING
                    && line.getPreorderStatus() != PreOrderStatus.READY_TO_SHIP) {
                throw new BadRequestException("Preorder is not ready to ship");
            }
        }
    }

    public void markReadyToShip(Order order) {
        List<PreOrder> lines = preOrderRepository.findByOrder_Id(order.getId());
        for (PreOrder line : lines) {
            if (line.getPreorderStatus() == PreOrderStatus.READY_FOR_PROCESSING) {
                line.setPreorderStatus(PreOrderStatus.READY_TO_SHIP);
            }
        }
    }

    public void markFulfilled(Order order) {
        List<PreOrder> lines = preOrderRepository.findByOrder_Id(order.getId());
        for (PreOrder line : lines) {
            line.setPreorderStatus(PreOrderStatus.FULFILLED);
        }
    }

    public void releaseReservations(Order order) {
        List<PreOrder> lines = preOrderRepository.findByOrder_Id(order.getId());

        for (PreOrder line : lines) {
            ProductVariant variant = productVariantRepository.lockById(line.getProductVariant().getId())
                    .orElseThrow(() -> new BadRequestException("Variant not found"));

            if (!Boolean.TRUE.equals(line.getSlotReleased())) {
                int current = variant.getCurrentPreorders() == null ? 0 : variant.getCurrentPreorders();
                variant.setCurrentPreorders(Math.max(0, current - line.getQuantity()));
                line.setSlotReleased(true);
            }

            if (Boolean.TRUE.equals(line.getAllocatedStock())) {
                int stock = variant.getStockQuantity() == null ? 0 : variant.getStockQuantity();
                variant.setStockQuantity(stock + line.getQuantity());
                line.setAllocatedStock(false);
            }

            line.setPreorderStatus(PreOrderStatus.CANCELLED);
        }
    }

    public void markStockArrived(Long variantId, int arrivedQty) {
        if (arrivedQty <= 0) {
            throw new BadRequestException("arrivedQty must be > 0");
        }

        ProductVariant variant = productVariantRepository.lockById(variantId)
                .orElseThrow(() -> new BadRequestException("Variant not found"));

        int stock = variant.getStockQuantity() == null ? 0 : variant.getStockQuantity();
        variant.setStockQuantity(stock + arrivedQty);

        List<PreOrder> queue = preOrderRepository.lockQueueByVariant(
                variantId,
                EnumSet.of(PreOrderStatus.AWAITING_STOCK)
        );

        for (PreOrder line : queue) {
            int available = variant.getStockQuantity() == null ? 0 : variant.getStockQuantity();
            if (available < line.getQuantity()) {
                break;
            }

            // allocate stock for this preorder line
            variant.setStockQuantity(available - line.getQuantity());
            line.setAllocatedStock(true);

            Order order = line.getOrder();
            long remaining = order.getRemainingAmount() == null ? 0L : order.getRemainingAmount();

            if (remaining > 0) {
                line.setPreorderStatus(PreOrderStatus.AWAITING_REMAINING_PAYMENT);

                String email = order.getAddress().getUser().getEmail();
                emailService.sendPreOrderRemainingPaymentEmail(
                        email,
                        order.getOrderCode(),
                        remaining,
                        line.getExpectedReleaseDate()
                );
            } else {
                line.setPreorderStatus(PreOrderStatus.READY_FOR_PROCESSING);
            }
        }
    }
}