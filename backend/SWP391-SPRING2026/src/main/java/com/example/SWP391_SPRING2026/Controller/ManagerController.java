package com.example.SWP391_SPRING2026.Controller;

import com.example.SWP391_SPRING2026.DTO.Request.*;
import com.example.SWP391_SPRING2026.DTO.Response.*;
import com.example.SWP391_SPRING2026.Entity.ProductCombo;
import com.example.SWP391_SPRING2026.Service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manager")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGER')")
public class ManagerController {

    private final ProductService productService;
    private final ProductVariantService productVariantService;
    private final VariantAttributeService variantAttributeService;
    private final VariantAttributeImageService  variantAttributeImageService;
    private final ProductComboService comboService;
    private final PreOrderService preOrderService;
    // ===================== PRODUCT =====================

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponseDTO createProduct(@RequestBody ProductRequestDTO dto) {
        return productService.createProduct(dto);
    }

    @PutMapping("/products/{id}")
    @ResponseStatus(HttpStatus.CREATED)
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

    @GetMapping("/products")
    @ResponseStatus(HttpStatus.OK)
    public List<ProductResponseDTO> getAllProducts() {
        return productService.getProducts();
    }

    @GetMapping("/products/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ProductDetailResponseDTO getProduct(@PathVariable Long id) {
        return productService.getProductDetails(id);
    }

    @PutMapping("/variants/{variantId}")
    @ResponseStatus(HttpStatus.CREATED)
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
    public VariantAttributeResponseSimpleDTO addAttribute(
            @PathVariable Long variantId,
            @RequestBody VariantAttributeRequestDTO dto) {

        return variantAttributeService.addAttribute(variantId, dto);
    }

    @GetMapping("/variants")
    @ResponseStatus(HttpStatus.OK)
    public List<ProductVariantResponseDTO> getAllVariants(){
        return productVariantService.getAllVariants();
    }

    @PutMapping("/attributes/{attributeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
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

    @PostMapping("/attributes/{attributeId}/images")
    @ResponseStatus(HttpStatus.CREATED)
    public List<VariantAttributeImageResponseDTO> addAttributeImages(
            @PathVariable Long attributeId,
            @RequestBody List<VariantAttributeImageRequestDTO> images){

        return variantAttributeImageService.addImages(attributeId, images);
    }

    @PutMapping("/attributes/images/{imageId}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<?> updateImage(
            @PathVariable Long imageId,
            @RequestBody VariantAttributeImageRequestDTO dto){

        variantAttributeImageService.updateImage(imageId, dto);
        return ResponseEntity.ok("Image updated successfully");
    }


    @DeleteMapping("/attributes/images/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAttributeImages(@PathVariable Long imageId) {
        variantAttributeImageService.deleteImage(imageId);
    }

    @PostMapping("/combos")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ProductComboResponseDTO> createCombo(@RequestBody CreateComboRequestDTO dto){
        return ResponseEntity.ok(comboService.createCombo(dto));
    }

    @GetMapping("/combos/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ProductComboResponseDTO getCombo(@PathVariable Long id){
        return comboService.getComboById(id);
    }

    @DeleteMapping("/combos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactive(@PathVariable Long id){
        comboService.deactivateCombo(id);
    }

    @PutMapping("/combos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<ProductComboResponseDTO> updateCombo(@PathVariable(name = "id")Long id, @RequestBody CreateComboRequestDTO dto){
        return ResponseEntity.ok(comboService.updateCombo(id, dto));
    }

    @GetMapping("/combos")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<Page<ProductComboResponseDTO>> getAllCombos(@RequestParam(defaultValue = "0")int page,
                                                                      @RequestParam(defaultValue = "10")int size){
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(comboService.getAllActiveCombos(pageable));
    }
    @PostMapping("/preorders/variants/{variantId}/stock-arrived")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> markPreOrderStockArrived(
            @PathVariable Long variantId,
            @RequestBody StockArrivalRequestDTO dto) {

        preOrderService.markStockArrived(variantId, dto.getArrivedQuantity());
        return ResponseEntity.ok("Pre-order stock arrival processed");
    }

    @PostMapping("/preorders/variants/{variantId}/allocate-current-stock")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> allocateCurrentStockForPreorders(@PathVariable Long variantId) {
        preOrderService.allocateAvailableStock(variantId);
        return ResponseEntity.ok("Current stock allocated to preorder queue");
    }
}