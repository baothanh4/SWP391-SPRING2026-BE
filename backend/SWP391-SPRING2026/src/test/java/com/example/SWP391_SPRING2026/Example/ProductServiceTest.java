package com.example.SWP391_SPRING2026.Example;

import com.example.SWP391_SPRING2026.DTO.Request.ChangePasswordDTO;
import com.example.SWP391_SPRING2026.DTO.Request.CheckoutRequestDTO;
import com.example.SWP391_SPRING2026.DTO.Request.ProductRequestDTO;
import com.example.SWP391_SPRING2026.DTO.Response.ProductResponseDTO;
import com.example.SWP391_SPRING2026.Entity.Cart;
import com.example.SWP391_SPRING2026.Entity.Product;
import com.example.SWP391_SPRING2026.Entity.Users;
import com.example.SWP391_SPRING2026.Enum.CartStatus;
import com.example.SWP391_SPRING2026.Enum.ProductStatus;
import com.example.SWP391_SPRING2026.Exception.BadRequestException;
import com.example.SWP391_SPRING2026.Repository.*;
import com.example.SWP391_SPRING2026.Service.CheckoutService;
import com.example.SWP391_SPRING2026.Service.CustomerService;
import com.example.SWP391_SPRING2026.Service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void createProduct_success() {

        ProductRequestDTO dto = new ProductRequestDTO();
        dto.setName("Nike Shirt");
        dto.setBrandName("Nike");

        Product savedProduct = new Product();
        savedProduct.setId(1L);
        savedProduct.setName("Nike Shirt");
        savedProduct.setBrandName("Nike");

        Mockito.when(productRepository.save(Mockito.any(Product.class)))
                .thenReturn(savedProduct);

        ProductResponseDTO result = productService.createProduct(dto);

        assertEquals("Nike Shirt", result.getName());
        assertEquals("Nike", result.getBrandName());
    }

    @Test
    void searchProducts_invalidPriceRange() {

        assertThrows(BadRequestException.class, () -> {

            productService.searchPublicProducts(
                    "nike",
                    null,
                    ProductStatus.ACTIVE,
                    new BigDecimal("200"),
                    new BigDecimal("100"),
                    true,
                    Pageable.unpaged()
            );

        });
    }
}


@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void changePassword_success() {

        Users user = new Users();
        user.setId(1L);
        user.setPassword("encoded");

        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setOldPassword("123");
        dto.setNewPassword("456");

        Mockito.when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        Mockito.when(passwordEncoder.matches("123", "encoded"))
                .thenReturn(true);

        Mockito.when(passwordEncoder.encode("456"))
                .thenReturn("encoded456");

        customerService.changePassword(1L, dto);

        assertEquals("encoded456", user.getPassword());
    }
}

@ExtendWith(MockitoExtension.class)
class CheckoutServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @InjectMocks
    private CheckoutService checkoutService;

    @Test
    void checkout_cartEmpty_throwException() {

        Cart cart = new Cart();
        cart.setItems(List.of());

        Mockito.when(cartRepository.findByUserIdAndStatus(
                1L,
                CartStatus.ACTIVE
        )).thenReturn(Optional.of(cart));

        CheckoutRequestDTO dto = new CheckoutRequestDTO();

        assertThrows(BadRequestException.class, () -> {

            checkoutService.checkout(
                    1L,
                    dto,
                    Mockito.mock(HttpServletRequest.class)
            );

        });
    }
}


