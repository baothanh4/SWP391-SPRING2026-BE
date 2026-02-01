package com.example.SWP391_SPRING2026.Controller;

import com.example.SWP391_SPRING2026.DTO.Request.ProductRequestDTO;
import com.example.SWP391_SPRING2026.DTO.Request.ProductVariantRequestDTO;
import com.example.SWP391_SPRING2026.DTO.Request.VariantAttributeRequestDTO;
import com.example.SWP391_SPRING2026.DTO.Response.ProductResponseDTO;
import com.example.SWP391_SPRING2026.DTO.Response.ProductVariantResponseDTO;
import com.example.SWP391_SPRING2026.Service.ProductService;
import com.example.SWP391_SPRING2026.Service.ProductVariantService;
import com.example.SWP391_SPRING2026.Service.VariantAttributeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/manager")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGER')")
public class ManagerController {

    private final ProductService productService;
    private final ProductVariantService productVariantService;
    private final VariantAttributeService variantAttributeService;

    // ===================== PRODUCT =====================

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponseDTO createProduct(@RequestBody ProductRequestDTO dto) {
        return productService.createProduct(dto);
    }

    @PutMapping("/products/{id}")
    public ProductResponseDTO updateProduct(
            @PathVariable Long id,
            @RequestBody ProductRequestDTO dto) {
        return productService.updateProduct(id, dto);
    }

    @DeleteMapping("/products/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateProduct(@PathVariable Long id) {
        productService.deactivateProduct(id);
    }

    // ===================== VARIANT =====================

    @PostMapping("/products/{productId}/variants")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductVariantResponseDTO createVariant(
            @PathVariable Long productId,
            @RequestBody ProductVariantRequestDTO dto) {
        return productVariantService.create(productId, dto);
    }

    @PutMapping("/variants/{variantId}")
    public ProductVariantResponseDTO updateVariant(
            @PathVariable Long variantId,
            @RequestBody ProductVariantRequestDTO dto) {
        return productVariantService.update(variantId, dto);
    }

    @DeleteMapping("/variants/{variantId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteVariant(@PathVariable Long variantId) {
        productVariantService.delete(variantId);
    }

    // ===================== VARIANT ATTRIBUTE =====================

    @PostMapping("/variants/{variantId}/attributes")
    @ResponseStatus(HttpStatus.CREATED)
    public void addAttribute(
            @PathVariable Long variantId,
            @RequestBody VariantAttributeRequestDTO dto) {
        variantAttributeService.addAttribute(variantId, dto);
    }

    @PutMapping("/attributes/{attributeId}")
    public void updateAttribute(
            @PathVariable Long attributeId,
            @RequestBody VariantAttributeRequestDTO dto) {
        variantAttributeService.updateAttribute(attributeId, dto);
    }

    @DeleteMapping("/attributes/{attributeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAttribute(@PathVariable Long attributeId) {
        variantAttributeService.deleteAttribute(attributeId);
    }
}