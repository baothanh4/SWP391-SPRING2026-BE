package com.example.SWP391_SPRING2026.Controller;

import com.example.SWP391_SPRING2026.DTO.Request.*;
import com.example.SWP391_SPRING2026.DTO.Response.*;
import com.example.SWP391_SPRING2026.Entity.Order;
import com.example.SWP391_SPRING2026.Entity.ProductCombo;
import com.example.SWP391_SPRING2026.Service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private final DashboardService dashboardService;
    private final PreOrderCampaignService preOrderCampaignService;
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

    @GetMapping("/dashboard")
    @ResponseStatus(HttpStatus.OK)
    public DashboardResponseDTO getDashboard(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to
    ) {
        return dashboardService.getDashboard(from, to);
    }

    @GetMapping("/dashboard/revenue-detail")
    public List<RevenueTimeDTO> getRevenueDetail(
            @RequestParam String type,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to
    ) {
        return dashboardService.getRevenueDetail(type, from, to);
    }

    @GetMapping("/dashboard/order-detail")
    public Page<OrderResponseDTO> getOrderDetail(
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            Pageable pageable
    ) {
        return dashboardService.getOrderDetail(status, from, to, pageable);
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


    public Page<ProductSearchItemDTO> browseProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean inStock,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return productService.browsePublicProducts(keyword, brand, minPrice, maxPrice, inStock, pageable);
    }


    @GetMapping("/products/search")
    @ResponseStatus(HttpStatus.OK)
    public Page<ProductSearchItemDTO> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean inStock,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return browseProducts(keyword, brand, minPrice, maxPrice, inStock, pageable);
    }

    @PostMapping("/preorder-campaigns")
    @ResponseStatus(HttpStatus.CREATED)
    public PreOrderCampaignResponseDTO createPreOrderCampaign(@Valid @RequestBody PreOrderCampaignRequestDTO dto) {
        return preOrderCampaignService.create(dto);
    }

    @GetMapping("/preorder-campaigns")
    @ResponseStatus(HttpStatus.OK)
    public List<PreOrderCampaignResponseDTO> getPreOrderCampaigns() {
        return preOrderCampaignService.getAll();
    }

    @GetMapping("/preorder-campaigns/{campaignId}")
    @ResponseStatus(HttpStatus.OK)
    public PreOrderCampaignResponseDTO getPreOrderCampaign(@PathVariable Long campaignId) {
        return preOrderCampaignService.getById(campaignId);
    }

    @PutMapping("/preorder-campaigns/{campaignId}")
    @ResponseStatus(HttpStatus.OK)
    public PreOrderCampaignResponseDTO updatePreOrderCampaign(
            @PathVariable Long campaignId,
            @Valid @RequestBody PreOrderCampaignRequestDTO dto) {
        return preOrderCampaignService.update(campaignId, dto);
    }

    @DeleteMapping("/preorder-campaigns/{campaignId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePreOrderCampaign(@PathVariable Long campaignId) {
        preOrderCampaignService.delete(campaignId);
    }

    @PatchMapping("/preorder-campaigns/{campaignId}/activate")
    @ResponseStatus(HttpStatus.OK)
    public PreOrderCampaignResponseDTO activatePreOrderCampaign(@PathVariable Long campaignId) {
        return preOrderCampaignService.activate(campaignId);
    }

    @PatchMapping("/preorder-campaigns/{campaignId}/deactivate")
    @ResponseStatus(HttpStatus.OK)
    public PreOrderCampaignResponseDTO deactivatePreOrderCampaign(@PathVariable Long campaignId) {
        return preOrderCampaignService.deactivate(campaignId);
    }
}