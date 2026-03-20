package com.example.SWP391_SPRING2026.mapper;

import com.example.SWP391_SPRING2026.DTO.Response.AddressResponseDTO;
import com.example.SWP391_SPRING2026.DTO.Response.OrderResponseDTO;
import com.example.SWP391_SPRING2026.Entity.Address;
import com.example.SWP391_SPRING2026.Entity.Order;
import com.example.SWP391_SPRING2026.Entity.Payment;
import com.example.SWP391_SPRING2026.Enum.PaymentMethod;
import com.example.SWP391_SPRING2026.Enum.PaymentStatus;
import com.example.SWP391_SPRING2026.Enum.ShipmentStatus;

import java.util.Comparator;

public class OrderMapper {

    public static OrderResponseDTO toResponse(Order order) {

        Address a = order.getAddress();

        AddressResponseDTO addressDTO = null;

        if (a != null) {
            addressDTO = new AddressResponseDTO(
                    a.getId(),
                    a.getReceiverName(),
                    a.getPhone(),
                    a.getAddressLine(),
                    a.getWard(),
                    a.getDistrict(),
                    a.getProvince(),
                    a.getIsDefault()
            );
        }

        // ================= PAYMENT =================
        PaymentMethod paymentMethod = null;
        PaymentStatus paymentStatus = null;

        if (order.getPayments() != null && !order.getPayments().isEmpty()) {

            Payment latestPayment = order.getPayments()
                    .stream()
                    .max(Comparator.comparing(Payment::getCreatedAt))
                    .orElse(null);

            if (latestPayment != null) {
                paymentMethod = latestPayment.getMethod();
                paymentStatus = latestPayment.getStatus();
            }
        }

        // ================= SHIPMENT =================
        String ghnCode = null;
        ShipmentStatus status = null;
        if (order.getShipment() != null) {
            ghnCode = order.getShipment().getGhnOrderCode();
            status = order.getShipment().getStatus();
        }

        return new OrderResponseDTO(
                order.getId(),
                order.getOrderCode(),
                order.getOrderType(),
                order.getOrderStatus(),
                order.getTotalAmount(),
                order.getDeposit(),
                order.getRemainingAmount(),
                addressDTO,
                order.getCreatedAt(),
                paymentMethod,
                paymentStatus,
                ghnCode,
                status,
                order.getApprovalStatus(),
                order.getSupportApprovedAt(),
                order.getOperationConfirmedAt()
        );
    }
}