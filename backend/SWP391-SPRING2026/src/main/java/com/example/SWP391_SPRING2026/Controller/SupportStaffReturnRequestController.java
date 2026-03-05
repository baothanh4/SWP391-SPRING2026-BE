package com.example.SWP391_SPRING2026.Controller;

import com.example.SWP391_SPRING2026.DTO.Request.RejectReturnRequestDTO;
import com.example.SWP391_SPRING2026.Entity.ReturnRequest;
import com.example.SWP391_SPRING2026.Entity.UserPrincipal;
import com.example.SWP391_SPRING2026.Enum.ReturnRequestStatus;
import com.example.SWP391_SPRING2026.Repository.ReturnRequestRepository;
import com.example.SWP391_SPRING2026.Service.ReturnRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/support_staff/return-requests")
@RequiredArgsConstructor
public class SupportStaffReturnRequestController {

    private final ReturnRequestRepository returnRequestRepository;
    private final ReturnRequestService returnRequestService;

    @GetMapping("/submitted")
    @ResponseStatus(HttpStatus.OK)
    public List<ReturnRequest> submitted() {
        return returnRequestRepository.findByStatusOrderByIdDesc(ReturnRequestStatus.SUBMITTED);
    }

    @PostMapping("/{id}/approve")
    @ResponseStatus(HttpStatus.OK)
    public void approve(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        returnRequestService.approve(principal.getUserId(), id);
    }

    @PostMapping("/{id}/reject")
    @ResponseStatus(HttpStatus.OK)
    public void reject(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestBody(required = false) RejectReturnRequestDTO body
    ) {
        String note = (body == null) ? null : body.getNote();
        returnRequestService.reject(principal.getUserId(), id, note);
    }
}