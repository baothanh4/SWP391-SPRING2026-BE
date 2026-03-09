package com.example.SWP391_SPRING2026.Controller;

import com.example.SWP391_SPRING2026.DTO.Request.SubmitReturnRequestDTO;
import com.example.SWP391_SPRING2026.DTO.Response.ReturnRequestResponseDTO;
import com.example.SWP391_SPRING2026.Entity.UserPrincipal;
import com.example.SWP391_SPRING2026.Service.ReturnRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer/return-requests")
@RequiredArgsConstructor
public class CustomerReturnRequestController {
    private final ReturnRequestService returnRequestService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReturnRequestResponseDTO submit(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody SubmitReturnRequestDTO dto) {
        return returnRequestService.submit(principal.getUserId(), dto);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<ReturnRequestResponseDTO> myRequests(
            @AuthenticationPrincipal UserPrincipal principal) {
        return returnRequestService.getMyRequests(principal.getUserId());
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ReturnRequestResponseDTO detail(
            @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        return returnRequestService.getMyRequestDetail(principal.getUserId(), id);
    }
}