package com.example.SWP391_SPRING2026.mapper;

import com.example.SWP391_SPRING2026.DTO.Response.AddressResponseDTO;
import com.example.SWP391_SPRING2026.DTO.Response.OrderResponseDTO;
import com.example.SWP391_SPRING2026.Entity.Address;
import com.example.SWP391_SPRING2026.Entity.Order;

public class OrderMapper {
    public static OrderResponseDTO toResponse(Order order){
        Address a = order.getAddress();

        return new OrderResponseDTO(
                order.getId(),
                order.getOrderCode(),
                order.getOrderType(),
                order.getOrderStatus(),
                order.getTotalAmount(),
                order.getDeposit(),
                order.getRemainingAmount(),
                new AddressResponseDTO(
                        a.getId(),
                        a.getReceiverName(),
                        a.getPhone(),
                        a.getAddressLine(),
                        a.getWard(),
                        a.getDistrict(),
                        a.getProvince(),
                        a.getIsDefault()
                )
        );
    }
}
