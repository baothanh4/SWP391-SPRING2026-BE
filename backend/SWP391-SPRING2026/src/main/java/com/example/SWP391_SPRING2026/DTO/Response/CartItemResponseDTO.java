package com.example.SWP391_SPRING2026.DTO.Response;

import com.example.SWP391_SPRING2026.DTO.Request.CartVariantAttributeDTO;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CartItemResponseDTO {
    private Long productId;
    private String productName;
    private String productImage;

    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal totalPrice;

    private List<CartVariantAttributeDTO> attributes;
}
