package com.example.SWP391_SPRING2026.Controller;

import com.example.SWP391_SPRING2026.DTO.Request.AddressRequestDTO;
import com.example.SWP391_SPRING2026.DTO.Request.AddressUpdateDTO;
import com.example.SWP391_SPRING2026.DTO.Request.ChangePasswordDTO;
import com.example.SWP391_SPRING2026.DTO.Request.CustomerAccountResponseDTO;
import com.example.SWP391_SPRING2026.DTO.Response.AddressResponseDTO;
import com.example.SWP391_SPRING2026.DTO.Response.CustomerAccountUpdateDTO;
import com.example.SWP391_SPRING2026.Entity.UserPrincipal;
import com.example.SWP391_SPRING2026.Service.AddressService;
import com.example.SWP391_SPRING2026.Service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerController {

    private final AddressService addressService;
    private final CustomerService customerService;
    @PostMapping("/addresses")
    public AddressResponseDTO create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AddressRequestDTO dto
    ) {
        return addressService.createAddress(principal.getUserId(), dto);
    }

    @GetMapping("/addresses")
    public List<AddressResponseDTO> getAll(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return addressService.getAll(principal.getUserId());
    }

    @PutMapping("/addresses/{addressId}")
    public AddressResponseDTO updateInfo(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long addressId,
            @Valid @RequestBody AddressUpdateDTO dto
    ) {
        return addressService.updateInfo(
                principal.getUserId(), addressId, dto
        );
    }

    @PatchMapping("/addresses/{addressId}/default")
    public void setDefault(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long addressId
    ) {
        addressService.setDefault(principal.getUserId(), addressId);
    }

    @DeleteMapping("/addresses/{addressId}")
    public void delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long addressId
    ) {
        addressService.delete(principal.getUserId(), addressId);
    }

    @GetMapping("/profile")
    public CustomerAccountResponseDTO getProfile(@AuthenticationPrincipal UserPrincipal principal) {
        return customerService.getProfile(principal.getUserId());
    }

    @PutMapping("/profile")
    public CustomerAccountResponseDTO updateProfile(@AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody CustomerAccountUpdateDTO dto){
        return customerService.updateProfile(principal.getUserId(), dto);
    }

    @PutMapping("/profile/change-password")
    public void changePassword(@AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody ChangePasswordDTO dto){
        customerService.changePassword(principal.getUserId(), dto);
    }

    @DeleteMapping("/profile")
    public void disableAccount(@AuthenticationPrincipal UserPrincipal principal){
        customerService.disableAccount(principal.getUserId());
    }

    @PutMapping("/orders/{orderId}/cancel")
    public ResponseEntity<String> cancelOrder(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long orderId){
        customerService.cancelOrderByCustomer(principal.getUserId(), orderId);
        return ResponseEntity.ok("Order Cancelled");
    }
}