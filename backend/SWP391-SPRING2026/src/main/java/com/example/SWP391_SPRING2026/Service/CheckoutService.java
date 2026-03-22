package com.example.SWP391_SPRING2026.Service;

import com.example.SWP391_SPRING2026.DTO.Request.CheckoutRequestDTO;
import com.example.SWP391_SPRING2026.DTO.Response.CheckoutResponseDTO;
import com.example.SWP391_SPRING2026.Entity.*;
import com.example.SWP391_SPRING2026.Enum.*;
import com.example.SWP391_SPRING2026.Exception.BadRequestException;
import com.example.SWP391_SPRING2026.Repository.*;
import com.example.SWP391_SPRING2026.Utility.DepositPolicy;
import com.example.SWP391_SPRING2026.Utility.PreOrderRule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
    private final ShipmentRepository shipmentRepository;
    private final PaymentRepository paymentRepository;
    private final VNPayService vnPayService;
    private final PreOrderService preOrderService;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final PreOrderRepository preOrderRepository;

    public CheckoutResponseDTO checkout(Long userId,
                                        CheckoutRequestDTO dto,
                                        HttpServletRequest request) {

        Cart cart = cartRepository
                .findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElseThrow(() -> new BadRequestException("Cart not found"));

        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Cart items is empty");
        }

        Address address = resolveAddress(userId, dto.getAddressId());

        SaleType saleType = null;

        for (CartItem item : cart.getItems()) {

            if (item.getProductVariant() != null) {

                ProductVariant variant = productVariantRepository
                        .lockById(item.getProductVariant().getId())
                        .orElseThrow(() -> new BadRequestException("Variant not found"));

                VariantAvailabilityStatus availability =
                        preOrderService.resolveAvailability(variant);

                if (availability == VariantAvailabilityStatus.OUT_OF_STOCK) {
                    throw new BadRequestException("Variant is out of stock");
                }

                SaleType itemType =
                        (availability == VariantAvailabilityStatus.PRE_ORDER)
                                ? SaleType.PRE_ORDER
                                : SaleType.IN_STOCK;

                if (saleType == null) saleType = itemType;
                else if (saleType != itemType) {
                    throw new BadRequestException("Cannot checkout mixed products");
                }
            }

            else if (item.getProductCombo() != null) {

                ProductCombo combo = item.getProductCombo();

                if (!Boolean.TRUE.equals(combo.getActive())) {
                    throw new BadRequestException("Combo is not available");
                }

                if (saleType == null) saleType = SaleType.IN_STOCK;
                else if (saleType != SaleType.IN_STOCK) {
                    throw new BadRequestException("Cannot checkout mixed products");
                }
            }
        }

        Order order = new Order();
        order.setOrderCode("ORD-" + System.currentTimeMillis());
        order.setOrderType(
                saleType == SaleType.PRE_ORDER
                        ? OrderType.PRE_ORDER
                        : OrderType.IN_STOCK
        );
        order.setOrderStatus(OrderStatus.WAITING_CONFIRM);
        order.setAddress(address);
        order.setUser(user);
        order.setCreatedAt(LocalDateTime.now());
        order.setApprovalStatus(ApprovalStatus.PENDING_SUPPORT);

        orderRepository.save(order);

        Long totalAmount = 0L;

        for (CartItem cartItem : cart.getItems()) {

            int quantity = cartItem.getQuantity();

            // ================= VARIANT =================

            if (cartItem.getProductVariant() != null) {

                ProductVariant variant = productVariantRepository
                        .lockById(cartItem.getProductVariant().getId())
                        .orElseThrow(() -> new BadRequestException("Variant not found"));

                VariantAvailabilityStatus availability =
                        preOrderService.resolveAvailability(variant);

                BigDecimal price = variant.getPrice();

                if (availability == VariantAvailabilityStatus.IN_STOCK) {

                    if (variant.getStockQuantity() < quantity) {
                        throw new BadRequestException("Insufficient stock");
                    }

                    variant.setStockQuantity(
                            variant.getStockQuantity() - quantity
                    );
                }

                OrderItems orderItem = new OrderItems();
                orderItem.setOrder(order);
                orderItem.setProductVariant(variant);
                orderItem.setQuantity(quantity);
                orderItem.setPrice(price.longValue());
                orderItem.setIsCombo(false);

                orderItemRepository.save(orderItem);

                if (availability == VariantAvailabilityStatus.PRE_ORDER) {
                    int userExistingQty = preOrderRepository.sumUserPreorderQuantityByVariant(
                            user.getId(),
                            variant.getId(),
                            java.util.EnumSet.of(
                                    PreOrderStatus.RESERVED,
                                    PreOrderStatus.AWAITING_STOCK,
                                    PreOrderStatus.AWAITING_REMAINING_PAYMENT,
                                    PreOrderStatus.READY_FOR_PROCESSING,
                                    PreOrderStatus.READY_TO_SHIP
                            )
                    );

                    if (userExistingQty + quantity > PreOrderRule.MAX_PER_USER_PER_VARIANT) {
                        throw new BadRequestException("Each customer can pre-order at most 2 units for this variant");
                    }
                    preOrderService.reserve(order, orderItem, variant.getId(), quantity);
                }

                totalAmount += price.longValue() * quantity;
            }

            // ================= COMBO =================

            else if (cartItem.getProductCombo() != null) {

                ProductCombo combo = cartItem.getProductCombo();

                if (!Boolean.TRUE.equals(combo.getActive())) {
                    throw new BadRequestException("Combo is not available");
                }

                validateComboStock(combo, quantity);

                Long comboPrice = combo.getComboPrice();

                OrderItems orderItem = new OrderItems();
                orderItem.setOrder(order);
                orderItem.setProductCombo(combo);
                orderItem.setQuantity(quantity);
                orderItem.setPrice(comboPrice);
                orderItem.setIsCombo(true);

                orderItemRepository.save(orderItem);

                totalAmount += comboPrice * quantity;

                for (ComboItem comboItem : combo.getItems()) {

                    ProductVariant variant = productVariantRepository
                            .lockById(comboItem.getProductVariant().getId())
                            .orElseThrow(() -> new BadRequestException("Variant not found"));

                    int required =
                            comboItem.getQuantity() * quantity;

                    if (variant.getStockQuantity() < required) {
                        throw new BadRequestException(
                                "Variant " + variant.getId() + " not enough stock"
                        );
                    }

                    variant.setStockQuantity(
                            variant.getStockQuantity() - required
                    );
                }
            }
        }

        order.setTotalAmount(totalAmount);

        long payableAmount;

        if (saleType == SaleType.PRE_ORDER) {

            long minDeposit = DepositPolicy.minDeposit(totalAmount);

            Long requestedDeposit = dto.getDepositAmount();

            long depositToPay =
                    (requestedDeposit == null)
                            ? minDeposit
                            : requestedDeposit;

            try {
                DepositPolicy.validate(totalAmount, depositToPay);
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException(ex.getMessage());
            }

            if (dto.getPaymentMethod() != PaymentMethod.VNPAY) {
                throw new BadRequestException(
                        "PRE_ORDER must be paid online by VNPAY"
                );
            }

            order.setDeposit(depositToPay);
            order.setRemainingAmount(totalAmount - depositToPay);

            order.setRemainingPaymentMethod(
                    order.getRemainingAmount() > 0
                            ? PaymentMethod.VNPAY
                            : null
            );

            payableAmount = depositToPay;
        }

        else {

            if (dto.getDepositAmount() != null) {
                throw new BadRequestException(
                        "depositAmount is only applicable for PRE_ORDER"
                );
            }

            payableAmount = totalAmount;
        }

        PaymentStage stage =
                (saleType == SaleType.IN_STOCK)
                        ? PaymentStage.FULL
                        : (payableAmount == totalAmount
                        ? PaymentStage.FULL
                        : PaymentStage.DEPOSIT);

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setStage(stage);
        payment.setMethod(dto.getPaymentMethod());
        payment.setAmount(payableAmount);
        payment.setCreatedAt(LocalDateTime.now());

        if (dto.getPaymentMethod() == PaymentMethod.COD) {

            payment.setStatus(PaymentStatus.UNPAID);

        } else {

            payment.setStatus(PaymentStatus.PENDING);
            payment.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        }

        paymentRepository.save(payment);

        Shipment shipment = new Shipment();
        shipment.setOrder(order);
        shipment.setStatus(ShipmentStatus.WAITING_CONFIRM);

        shipmentRepository.save(shipment);

        order.setShipment(shipment);

        cart.setStatus(CartStatus.CHECKED_OUT);

        if (dto.getPaymentMethod() == PaymentMethod.COD) {

            emailService.sendOrderPlacedEmail(
                    address.getUser().getEmail(),
                    order.getOrderCode()
            );
        }

        String paymentUrl = null;

        if (dto.getPaymentMethod() == PaymentMethod.VNPAY) {

            String ipAddress = request.getRemoteAddr();

            if (ipAddress == null ||
                    ipAddress.equals("0:0:0:0:0:0:0:1")) {
                ipAddress = "127.0.0.1";
            }

            try {

                paymentUrl = vnPayService.createVNPayUrl(
                        payment.getId().toString(),
                        payableAmount,
                        ipAddress
                );

            } catch (Exception e) {

                throw new RuntimeException("Cannot create VNPay URL");
            }
        }

        return new CheckoutResponseDTO(
                order.getId(),
                order.getOrderCode(),
                payableAmount,
                dto.getPaymentMethod(),
                paymentUrl,
                payment.getStatus()
        );
    }

    private Address resolveAddress(Long userId, Long addressId) {

        if (addressId != null) {

            return addressRepository
                    .findByIdAndUser_Id(addressId, userId)
                    .orElseThrow(() -> new BadRequestException("Address not found"));
        }

        return addressRepository
                .findFirstByUser_IdAndIsDefaultTrue(userId)
                .orElseThrow(() -> new BadRequestException("No default address"));
    }

    private void validateComboStock(ProductCombo combo, int quantityCombo) {

        for (ComboItem comboItem : combo.getItems()) {

            ProductVariant variant = productVariantRepository
                    .lockById(comboItem.getProductVariant().getId())
                    .orElseThrow(() -> new BadRequestException("Variant not found"));

            int required =
                    comboItem.getQuantity() * quantityCombo;

            int stock =
                    variant.getStockQuantity() == null
                            ? 0
                            : variant.getStockQuantity();

            if (required > stock) {

                throw new BadRequestException(
                        "Variant " + variant.getId()
                                + " only has "
                                + stock
                                + " but combo requires "
                                + required
                );
            }
        }
    }
}