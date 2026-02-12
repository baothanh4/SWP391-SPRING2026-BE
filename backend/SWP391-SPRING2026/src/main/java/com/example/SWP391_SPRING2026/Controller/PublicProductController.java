package com.example.SWP391_SPRING2026.Controller;

import com.example.SWP391_SPRING2026.DTO.Response.ProductDetailResponseDTO;
import com.example.SWP391_SPRING2026.DTO.Response.ProductResponseDTO;
import com.example.SWP391_SPRING2026.DTO.Response.ProductSearchItemDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import com.example.SWP391_SPRING2026.Service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/public/products")
@RequiredArgsConstructor

public class PublicProductController {

    private final ProductService productService;

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


    @GetMapping("/search")
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

    @GetMapping("/{id:\\d+}")
    public ProductDetailResponseDTO getProductDetail(@PathVariable Long id) {
        return productService.getPublicProductDetails(id);
    }

    @GetMapping
    public ResponseEntity<List<ProductResponseDTO>> getProducts(){
        return ResponseEntity.ok(productService.getProducts());
    }

    @GetMapping("/brands")
    public List<String> getBrands() {
        return productService.getPublicBrands();
    }

}
