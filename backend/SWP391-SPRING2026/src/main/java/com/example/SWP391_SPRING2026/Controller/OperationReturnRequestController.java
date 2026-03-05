package com.example.SWP391_SPRING2026.Controller;

import com.example.SWP391_SPRING2026.DTO.Request.ReceiveReturnRequestDTO;
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
@RequestMapping("/api/operation_staff/return-requests")
@RequiredArgsConstructor
public class OperationReturnRequestController {

    private final ReturnRequestRepository returnRequestRepository;
    private final ReturnRequestService returnRequestService;

    @GetMapping("/waiting-return")
    @ResponseStatus(HttpStatus.OK)
    public List<ReturnRequest> waitingReturn() {
        return returnRequestRepository.findByStatusOrderByIdDesc(ReturnRequestStatus.WAITING_RETURN);
    }

    @PostMapping("/{id}/received")
    @ResponseStatus(HttpStatus.OK)
    public void received(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestBody ReceiveReturnRequestDTO dto
    ) {
        returnRequestService.receive(principal.getUserId(), id, dto);
    }
}