package com.example.SWP391_SPRING2026.Service;

import com.example.SWP391_SPRING2026.DTO.Response.RefundRequestResponseDTO;
import com.example.SWP391_SPRING2026.Entity.Order;
import com.example.SWP391_SPRING2026.Entity.RefundRequest;
import com.example.SWP391_SPRING2026.Enum.RefundRequestStatus;
import com.example.SWP391_SPRING2026.Exception.BadRequestException;
import com.example.SWP391_SPRING2026.Exception.ResourceNotFoundException;
import com.example.SWP391_SPRING2026.Repository.OrderRepository;
import com.example.SWP391_SPRING2026.Repository.RefundRequestRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RefundRequestService {

    private final RefundRequestRepository refundRequestRepository;
    private final OrderRepository orderRepository;

    public List<RefundRequestResponseDTO> getRequestedForSupport() {
        return refundRequestRepository.findByStatusOrderByIdDesc(RefundRequestStatus.REQUESTED)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<RefundRequestResponseDTO> getByOrderForCustomer(Long customerId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getUser() == null || !order.getUser().getId().equals(customerId)) {
            throw new BadRequestException("You are not allowed to view refund request of this order");
        }

        return refundRequestRepository.findByOrderIdOrderByIdDesc(orderId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public RefundRequestResponseDTO markDoneBySupport(Long supportUserId, Long refundRequestId, String note) {
        RefundRequest rr = refundRequestRepository.findById(refundRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund request not found"));

        if (rr.getStatus() != RefundRequestStatus.REQUESTED) {
            throw new BadRequestException("Only REQUESTED refund can be marked DONE");
        }

        rr.setStatus(RefundRequestStatus.DONE);
        rr.setUpdatedAt(LocalDateTime.now());

        String finalNote = (note == null || note.isBlank())
                ? "[SUPPORT_DONE] Refund completed manually"
                : "[SUPPORT_DONE] " + note;

        rr.setNote(appendNote(rr.getNote(), finalNote));
        return toResponse(rr);
    }

    public RefundRequestResponseDTO rejectBySupport(Long supportUserId, Long refundRequestId, String note) {
        RefundRequest rr = refundRequestRepository.findById(refundRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund request not found"));

        if (rr.getStatus() != RefundRequestStatus.REQUESTED) {
            throw new BadRequestException("Only REQUESTED refund can be rejected");
        }

        rr.setStatus(RefundRequestStatus.REJECTED);
        rr.setUpdatedAt(LocalDateTime.now());

        String finalNote = (note == null || note.isBlank())
                ? "[SUPPORT_REJECTED] Refund request rejected"
                : "[SUPPORT_REJECTED] " + note;

        rr.setNote(appendNote(rr.getNote(), finalNote));
        return toResponse(rr);
    }

    private String appendNote(String oldNote, String newNote) {
        if (oldNote == null || oldNote.isBlank()) return newNote;
        return oldNote + "\n" + newNote;
    }

    private RefundRequestResponseDTO toResponse(RefundRequest rr) {
        RefundRequestResponseDTO dto = new RefundRequestResponseDTO();
        dto.setId(rr.getId());
        dto.setOrderId(rr.getOrder() == null ? null : rr.getOrder().getId());
        dto.setReason(rr.getReason());
        dto.setPolicy(rr.getPolicy());
        dto.setStatus(rr.getStatus());
        dto.setRefundAmount(rr.getRefundAmount());
        dto.setNote(rr.getNote());
        dto.setCreatedByUserId(rr.getCreatedByUserId());
        dto.setCreatedByRole(rr.getCreatedByRole());
        dto.setCreatedAt(rr.getCreatedAt());
        dto.setUpdatedAt(rr.getUpdatedAt());
        return dto;
    }
}