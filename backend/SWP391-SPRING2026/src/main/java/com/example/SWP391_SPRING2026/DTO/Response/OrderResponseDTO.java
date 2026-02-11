package com.example.SWP391_SPRING2026.DTO.Response;

import com.example.SWP391_SPRING2026.Enum.OrderStatus;
import com.example.SWP391_SPRING2026.Enum.OrderType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDTO {
    private Long id;
    private String orderCode;
    private OrderType orderType;
    private OrderStatus orderStatus;
    private Long totalAmount;
    private Long deposit;
    private Long remainingAmount;
    private AddressResponseDTO  address;
}
