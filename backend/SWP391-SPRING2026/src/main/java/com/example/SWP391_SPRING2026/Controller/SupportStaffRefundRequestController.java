package com.example.SWP391_SPRING2026.Controller;

import com.example.SWP391_SPRING2026.DTO.Request.RefundActionRequestDTO;
import com.example.SWP391_SPRING2026.DTO.Response.RefundRequestResponseDTO;
import com.example.SWP391_SPRING2026.Entity.UserPrincipal;
import com.example.SWP391_SPRING2026.Service.RefundRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/support_staff/refund-requests")
@RequiredArgsConstructor
public class SupportStaffRefundRequestController {

    private final RefundRequestService refundRequestService;

    @GetMapping("/requested")
    @ResponseStatus(HttpStatus.OK)
    public List<RefundRequestResponseDTO> getRequestedRefundRequests() {
        return refundRequestService.getRequestedForSupport();
    }

    @PostMapping("/{refundRequestId}/done")
    @ResponseStatus(HttpStatus.OK)
    public RefundRequestResponseDTO markRefundDone(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long refundRequestId,
            @RequestBody(required = false) RefundActionRequestDTO body
    ) {
        String note = body == null ? null : body.getNote();
        return refundRequestService.markDoneBySupport(
                principal.getUserId(),
                refundRequestId,
                note
        );
    }

    @PostMapping("/{refundRequestId}/reject")
    @ResponseStatus(HttpStatus.OK)
    public RefundRequestResponseDTO rejectRefund(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long refundRequestId,
            @RequestBody(required = false) RefundActionRequestDTO body
    ) {
        String note = body == null ? null : body.getNote();
        return refundRequestService.rejectBySupport(
                principal.getUserId(),
                refundRequestId,
                note
        );
    }
}