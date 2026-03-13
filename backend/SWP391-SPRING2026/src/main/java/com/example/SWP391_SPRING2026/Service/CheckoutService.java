package com.example.SWP391_SPRING2026.Service;

import com.example.SWP391_SPRING2026.DTO.Request.CheckoutRequestDTO;
import com.example.SWP391_SPRING2026.DTO.Response.CheckoutResponseDTO;
import com.example.SWP391_SPRING2026.Entity.*;
import com.example.SWP391_SPRING2026.Enum.*;
import com.example.SWP391_SPRING2026.Exception.BadRequestException;
import com.example.SWP391_SPRING2026.Repository.*;
import com.example.SWP391_SPRING2026.Utility.DepositPolicy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

                VariantAvailabilityStatus availability = preOrderService.resolveAvailability(variant);

                if (availability == VariantAvailabilityStatus.OUT_OF_STOCK) {
                    throw new BadRequestException("Variant is out of stock");
                }

                SaleType itemType = (availability == VariantAvailabilityStatus.PRE_ORDER)
                        ? SaleType.PRE_ORDER
                        : SaleType.IN_STOCK;

                if (saleType == null) saleType = itemType;
                else if (saleType != itemType) {
                    throw new BadRequestException("Cannot checkout mixed products");
                }
            } else if (item.getProductCombo() != null) {
                if (saleType == null) saleType = SaleType.IN_STOCK;
                else if (saleType != SaleType.IN_STOCK) {
                    throw new BadRequestException("Cannot checkout mixed products");
                }
            }
        }

        Order order = new Order();
        order.setOrderCode("ORD-" + System.currentTimeMillis());
        order.setOrderType(saleType == SaleType.PRE_ORDER ? OrderType.PRE_ORDER : OrderType.IN_STOCK);
        order.setOrderStatus(OrderStatus.WAITING_CONFIRM);
        order.setAddress(address);
        order.setUser(user);
        order.setCreatedAt(LocalDateTime.now());
        orderRepository.save(order);

        long totalAmount = 0L;

        for (CartItem cartItem : cart.getItems()) {
            int quantity = cartItem.getQuantity();

            if (cartItem.getProductVariant() != null) {
                ProductVariant variant = productVariantRepository
                        .lockById(cartItem.getProductVariant().getId())
                        .orElseThrow(() -> new BadRequestException("Variant not found"));

                VariantAvailabilityStatus availability = preOrderService.resolveAvailability(variant);
                long price = variant.getPrice().longValue();

                OrderItems orderItem = new OrderItems();
                orderItem.setOrder(order);
                orderItem.setProductVariant(variant);
                orderItem.setQuantity(quantity);
                orderItem.setPrice(price);
                orderItem.setIsCombo(false);

                if (availability == VariantAvailabilityStatus.IN_STOCK) {
                    if (variant.getStockQuantity() < quantity) {
                        throw new BadRequestException("Insufficient stock");
                    }
                    // giữ nguyên behavior cũ của bạn
                    // note: BR-02 muốn soft-lock, còn code này vẫn hard-decrement
                    variant.setStockQuantity(variant.getStockQuantity() - quantity);
                }

                orderItemRepository.save(orderItem);

                if (availability == VariantAvailabilityStatus.PRE_ORDER) {
                    preOrderService.reserve(order, orderItem, variant, quantity);
                }

                totalAmount += price * quantity;
            }

            else if (cartItem.getProductCombo() != null) {
                ProductCombo combo = cartItem.getProductCombo();
                long comboPrice = combo.getComboPrice().longValue();

                OrderItems orderItem = new OrderItems();
                orderItem.setOrder(order);
                orderItem.setProductCombo(combo);
                orderItem.setQuantity(quantity);
                orderItem.setPrice(comboPrice);
                orderItem.setIsCombo(true);
                orderItemRepository.save(orderItem);

                totalAmount += comboPrice * quantity;

                validateComboStock(combo,quantity);

                for (ComboItem comboItem : combo.getItems()) {
                    ProductVariant variant = productVariantRepository
                            .lockById(comboItem.getProductVariant().getId())
                            .orElseThrow(() -> new BadRequestException("Variant not found"));

                    int required = comboItem.getQuantity() * quantity;

                    variant.setStockQuantity(variant.getStockQuantity() - required);
                }
            }
        }

        order.setTotalAmount(totalAmount);

        long payableAmount;
        if (saleType == SaleType.PRE_ORDER) {
            long minDeposit = DepositPolicy.minDeposit(totalAmount);
            Long requestedDeposit = dto.getDepositAmount();
            long depositToPay = (requestedDeposit == null) ? minDeposit : requestedDeposit;

            try {
                DepositPolicy.validate(totalAmount, depositToPay);
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException(ex.getMessage());
            }

            // BR-07 => PRE_ORDER không cho COD
            if (dto.getPaymentMethod() != PaymentMethod.VNPAY) {
                throw new BadRequestException("PRE_ORDER must be paid online by VNPAY");
            }

            order.setDeposit(depositToPay);
            order.setRemainingAmount(totalAmount - depositToPay);
            order.setRemainingPaymentMethod(
                    (order.getRemainingAmount() != null && order.getRemainingAmount() > 0)
                            ? PaymentMethod.VNPAY
                            : null
            );

            payableAmount = depositToPay;
        } else {
            if (dto.getDepositAmount() != null) {
                throw new BadRequestException("depositAmount is only applicable for PRE_ORDER");
            }
            payableAmount = totalAmount;
        }

        PaymentStage initialStage =
                (saleType == SaleType.IN_STOCK)
                        ? PaymentStage.FULL
                        : (payableAmount == totalAmount ? PaymentStage.FULL : PaymentStage.DEPOSIT);

        Payment initialPayment = new Payment();
        initialPayment.setOrder(order);
        initialPayment.setStage(initialStage);
        initialPayment.setMethod(dto.getPaymentMethod());
        initialPayment.setAmount(payableAmount);
        initialPayment.setCreatedAt(LocalDateTime.now());

        if (dto.getPaymentMethod() == PaymentMethod.COD) {
            initialPayment.setStatus(PaymentStatus.UNPAID);
        } else {
            initialPayment.setStatus(PaymentStatus.PENDING);
            initialPayment.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        }

        paymentRepository.save(initialPayment);

        Shipment shipment = new Shipment();
        shipment.setOrder(order);
        shipment.setStatus(ShipmentStatus.WAITING_CONFIRM);
        shipmentRepository.save(shipment);
        order.setShipment(shipment);

        cart.setStatus(CartStatus.CHECKED_OUT);

        if (dto.getPaymentMethod() == PaymentMethod.COD) {

            String email = address.getUser().getEmail();

            emailService.sendOrderPlacedEmail(
                    email,
                    order.getOrderCode()
            );
        }

        String paymentUrl = null;
        if (dto.getPaymentMethod() == PaymentMethod.VNPAY) {
            String ipAddress = request.getRemoteAddr();
            if (ipAddress == null || ipAddress.equals("0:0:0:0:0:0:0:1")) {
                ipAddress = "127.0.0.1";
            }

            try {
                paymentUrl = vnPayService.createVNPayUrl(
                        initialPayment.getId().toString(),
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
                paymentUrl
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

    private void validateComboStock(ProductCombo combo, int quantityCombo){
        for(ComboItem comboItem : combo.getItems()) {
            ProductVariant variants = productVariantRepository.lockById(comboItem.getProductVariant().getId()).orElseThrow(() -> new BadRequestException("Variant not found"));

            int required = comboItem.getQuantity() * quantityCombo;

            int stock = variants.getStockQuantity() == null ? 0 : variants.getStockQuantity();

            if(required > stock){
                throw new BadRequestException("Variant " + variants.getId() + " only has " + stock + " but combo requires " + required);
            }
        }
    }
}