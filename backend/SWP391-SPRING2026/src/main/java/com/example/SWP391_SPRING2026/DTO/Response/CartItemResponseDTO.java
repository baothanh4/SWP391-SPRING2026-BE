package com.example.SWP391_SPRING2026.DTO.Response;

import com.example.SWP391_SPRING2026.DTO.Request.CartVariantAttributeDTO;
import com.example.SWP391_SPRING2026.Enum.SaleType;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CartItemResponseDTO {
    private Long cartItemId;

    // phân biệt combo / variant
    private Boolean isCombo;

    // variant
    private Long productId;
    private String productName;
    private String productImage;
    private SaleType saleType;

    // combo
    private Long comboId;
    private String comboName;
    private String comboImage;
    private List<ComboItemDTO> comboItems;

    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal totalPrice;

    private List<CartVariantAttributeDTO> attributes;
}
