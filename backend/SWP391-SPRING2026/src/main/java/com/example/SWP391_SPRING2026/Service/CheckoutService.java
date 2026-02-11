package com.example.SWP391_SPRING2026.Service;

import com.example.SWP391_SPRING2026.DTO.Request.CheckoutRequestDTO;
import com.example.SWP391_SPRING2026.Entity.*;
import com.example.SWP391_SPRING2026.Enum.*;
import com.example.SWP391_SPRING2026.Exception.BadRequestException;
import com.example.SWP391_SPRING2026.Repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class CheckoutService {

    private final CartRepository cartRepository;
    private final AddressRepository addressRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final PreOrderRepository preOrderRepository;
    private final ShipmentRepository shipmentRepository;
    private final PaymentRepository paymentRepository;

    public Order checkout(Long userId, CheckoutRequestDTO dto) {

        Cart cart = cartRepository
                .findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElseThrow(() -> new BadRequestException("Cart not found"));

        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Cart items is empty");
        }

        Address address = resolveAddress(userId, dto.getAddressId());

        // ===== XÁC ĐỊNH SALE TYPE =====
        SaleType saleType = null;
        for (CartItem item : cart.getItems()) {
            SaleType itemType = item.getProductVariant().getSaleType();
            if (saleType == null) saleType = itemType;
            else if (saleType != itemType) {
                throw new BadRequestException("Cannot checkout mixed IN_STOCK and PRE_ORDER products");
            }
        }

        // ===== CREATE ORDER =====
        Order order = new Order();
        order.setOrderCode("ORD-" + System.currentTimeMillis());
        order.setOrderType(saleType == SaleType.IN_STOCK ? OrderType.IN_STOCK : OrderType.PRE_ORDER);
        order.setOrderStatus(OrderStatus.WAITING_CONFIRM); // ✅ chờ staff confirm
        order.setAddress(address);
        order.setCreatedAt(LocalDateTime.now());

        orderRepository.save(order);

        long totalAmount = 0;

        // ===== ORDER ITEMS =====
        for (CartItem cartItem : cart.getItems()) {

            ProductVariant variant = productVariantRepository
                    .lockById(cartItem.getProductVariant().getId())
                    .orElseThrow(() -> new BadRequestException("Product variant not found"));

            int quantity = cartItem.getQuantity();
            long price = variant.getPrice().longValue();

            if (saleType == SaleType.IN_STOCK) {
                if (variant.getStockQuantity() < quantity) {
                    throw new BadRequestException("Insufficient stock");
                }
                variant.setStockQuantity(variant.getStockQuantity() - quantity);
            }

            OrderItems orderItem = new OrderItems();
            orderItem.setOrder(order);
            orderItem.setProductVariant(variant);
            orderItem.setQuantity(quantity);
            orderItem.setPrice(price);
            orderItem.setIsCombo(false);

            orderItemRepository.save(orderItem);

            totalAmount += price * quantity;

            // ===== PREORDER =====
            if (saleType == SaleType.PRE_ORDER) {
                PreOrder preOrder = new PreOrder();
                preOrder.setOrder(order);
                preOrder.setProductVariant(variant);
                preOrder.setExpectedReleaseDate(LocalDate.now().plusMonths(1));
                preOrder.setDepositAmount(price * quantity * 30 / 100);
                preOrder.setPreorderStatus(PreOrderStatus.WAITING);

                preOrderRepository.save(preOrder);
            }
        }

        order.setTotalAmount(totalAmount);

        if (saleType == SaleType.PRE_ORDER) {
            long deposit = totalAmount * 30 / 100;
            order.setDeposit(deposit);
            order.setRemainingAmount(totalAmount - deposit);
        }

        // ===== PAYMENT =====
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setMethod(dto.getPaymentMethod());
        payment.setAmount(
                saleType == SaleType.PRE_ORDER
                        ? order.getDeposit()
                        : totalAmount
        );

        if (dto.getPaymentMethod() == PaymentMethod.COD) {
            payment.setStatus(PaymentStatus.UNPAID); // ✅ chưa trả tiền
            payment.setPaidAt(null);
        } else {
            payment.setStatus(PaymentStatus.PENDING); // chờ cổng thanh toán
        }

        paymentRepository.save(payment);
        order.setPayment(payment); // ✅ set 2 chiều

        // ===== SHIPMENT =====
        Shipment shipment = new Shipment();
        shipment.setOrder(order);
        shipment.setStatus(ShipmentStatus.WAITING_CONFIRM);
        shipmentRepository.save(shipment);
        order.setShipment(shipment); // ✅ set 2 chiều

        // ===== CART =====
        cart.setStatus(CartStatus.CHECKED_OUT);

        return order;
    }

    private Address resolveAddress(Long userId, Long addressId) {
        if (addressId != null) {
            return addressRepository
                    .findByIdAndUser_Id(addressId, userId)
                    .orElseThrow(() -> new BadRequestException("Address not found"));
        }

        return addressRepository
                .findFirstByUser_IdAndIsDefaultTrue(userId)
                .orElseThrow(() -> new BadRequestException("No default address found"));
    }
}
