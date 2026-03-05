package com.example.SWP391_SPRING2026.Service;

import com.example.SWP391_SPRING2026.DTO.Request.ReceiveReturnRequestDTO;
import com.example.SWP391_SPRING2026.DTO.Request.SubmitReturnRequestDTO;
import com.example.SWP391_SPRING2026.DTO.Response.ReturnRequestResponseDTO;
import com.example.SWP391_SPRING2026.Entity.*;
import com.example.SWP391_SPRING2026.Enum.*;
import com.example.SWP391_SPRING2026.Exception.BadRequestException;
import com.example.SWP391_SPRING2026.Repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReturnRequestService {

    private final ReturnRequestRepository returnRequestRepository;
    private final ReturnRecordRepository returnRecordRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRequestRepository refundRequestRepository;

    // 1) Customer submit complaint
    public ReturnRequestResponseDTO submit(Long userId, SubmitReturnRequestDTO dto) {

        if (dto.getOrderItemId() == null) throw new BadRequestException("orderItemId is required");
        if (dto.getQuantity() == null || dto.getQuantity() <= 0) throw new BadRequestException("quantity must be > 0");
        if (dto.getReason() == null) throw new BadRequestException("reason is required");

        OrderItems item = orderItemRepository.lockOwnedItem(dto.getOrderItemId(), userId)
                .orElseThrow(() -> new BadRequestException("Order item not found"));

        Order order = item.getOrder();

        // bạn đã chốt: chỉ cho khiếu nại khi COMPLETED
        if (order.getOrderStatus() != OrderStatus.COMPLETED) {
            throw new BadRequestException("Only COMPLETED orders can be complained");
        }

        if (dto.getQuantity() > item.getQuantity()) {
            throw new BadRequestException("Requested quantity exceeds purchased quantity");
        }

        // chặn 1 request active / item
        EnumSet<ReturnRequestStatus> active = EnumSet.of(
                ReturnRequestStatus.SUBMITTED,
                ReturnRequestStatus.WAITING_RETURN,
                ReturnRequestStatus.RECEIVED,
                ReturnRequestStatus.REFUND_REQUESTED
        );

        if (returnRequestRepository.existsByOrderItem_IdAndStatusIn(item.getId(), active)) {
            throw new BadRequestException("This item already has an active return request");
        }

        ReturnRequest rr = new ReturnRequest();
        rr.setOrderItem(item);
        rr.setRequestedQuantity(dto.getQuantity());
        rr.setReason(dto.getReason());
        rr.setStatus(ReturnRequestStatus.SUBMITTED);
        rr.setNote(dto.getNote());
        rr.setEvidenceUrls(dto.getEvidenceUrls());
        rr.setCreatedAt(LocalDateTime.now());
        rr.setUpdatedAt(LocalDateTime.now());

        returnRequestRepository.save(rr);

        return toResponse(rr);
    }

    // 2) Support approve: SUBMITTED -> WAITING_RETURN
    public void approve(Long supportUserId, Long returnRequestId) {
        ReturnRequest rr = returnRequestRepository.findById(returnRequestId)
                .orElseThrow(() -> new BadRequestException("Return request not found"));

        if (rr.getStatus() != ReturnRequestStatus.SUBMITTED) {
            throw new BadRequestException("Only SUBMITTED request can be approved");
        }

        rr.setStatus(ReturnRequestStatus.WAITING_RETURN);
        rr.setApprovedByUserId(supportUserId);
        rr.setApprovedAt(LocalDateTime.now());
        rr.setUpdatedAt(LocalDateTime.now());
    }

    // 3) Support reject
    public void reject(Long supportUserId, Long returnRequestId, String note) {
        ReturnRequest rr = returnRequestRepository.findById(returnRequestId)
                .orElseThrow(() -> new BadRequestException("Return request not found"));

        if (rr.getStatus() != ReturnRequestStatus.SUBMITTED) {
            throw new BadRequestException("Only SUBMITTED request can be rejected");
        }

        rr.setStatus(ReturnRequestStatus.REJECTED);
        rr.setRejectedByUserId(supportUserId);
        rr.setRejectedNote(note);
        rr.setRejectedAt(LocalDateTime.now());
        rr.setUpdatedAt(LocalDateTime.now());
    }

    // 4) Operation receive: WAITING_RETURN -> RECEIVED -> create ReturnRecord + restock + create RefundRequest
    public void receive(Long operationUserId, Long returnRequestId, ReceiveReturnRequestDTO dto) {

        if (dto.getAcceptedQuantity() == null || dto.getAcceptedQuantity() <= 0) {
            throw new BadRequestException("acceptedQuantity must be > 0");
        }

        ReturnRequest rr = returnRequestRepository.findById(returnRequestId)
                .orElseThrow(() -> new BadRequestException("Return request not found"));

        if (rr.getStatus() != ReturnRequestStatus.WAITING_RETURN) {
            throw new BadRequestException("Only WAITING_RETURN request can be received");
        }

        if (dto.getAcceptedQuantity() > rr.getRequestedQuantity()) {
            throw new BadRequestException("acceptedQuantity cannot exceed requestedQuantity");
        }

        OrderItems item = orderItemRepository.lockById(rr.getOrderItem().getId())
                .orElseThrow(() -> new BadRequestException("Order item not found"));

        Order order = item.getOrder();

        // mark received
        rr.setStatus(ReturnRequestStatus.RECEIVED);
        rr.setReceivedByUserId(operationUserId);
        rr.setReceivedAt(LocalDateTime.now());
        rr.setUpdatedAt(LocalDateTime.now());

        // create return record
        ReturnRecord record = new ReturnRecord();
        record.setReturnRequest(rr);
        record.setAcceptedQuantity(dto.getAcceptedQuantity());
        record.setConditionNote(dto.getConditionNote());
        record.setRestocked(false);
        record.setCreatedByUserId(operationUserId);
        record.setCreatedAt(LocalDateTime.now());
        returnRecordRepository.save(record);

        // restock
        restock(item, dto.getAcceptedQuantity());
        record.setRestocked(true);

        // trigger refund request
        long itemRefund = item.getPrice() * dto.getAcceptedQuantity();
        long amountPaid = calcAmountPaid(order.getId());
        long refundAmount = Math.min(itemRefund, amountPaid);

        RefundRequest refund = new RefundRequest();
        refund.setOrder(order);
        refund.setReason(RefundReason.RETURN_REQUEST);
        refund.setPolicy(RefundPolicy.FULL_REFUND);
        refund.setStatus(RefundRequestStatus.REQUESTED);
        refund.setRefundAmount(refundAmount);
        refund.setNote("Refund for return request #" + rr.getId());
        refund.setCreatedByUserId(operationUserId);
        refund.setCreatedByRole("OPERATION_STAFF");
        refund.setCreatedAt(LocalDateTime.now());
        refund.setUpdatedAt(LocalDateTime.now());


        refund.setReturnRequest(rr);

        refundRequestRepository.save(refund);

        rr.setStatus(ReturnRequestStatus.REFUND_REQUESTED);
        rr.setUpdatedAt(LocalDateTime.now());
    }

    private void restock(OrderItems item, int acceptedQty) {
        // combo
        if (Boolean.TRUE.equals(item.getIsCombo()) && item.getProductCombo() != null) {
            for (ComboItem ci : item.getProductCombo().getItems()) {
                ProductVariant pv = productVariantRepository.lockById(ci.getProductVariant().getId())
                        .orElseThrow(() -> new BadRequestException("Variant not found"));

                int add = ci.getQuantity() * acceptedQty;
                pv.setStockQuantity(pv.getStockQuantity() + add);
            }
            return;
        }

        // normal variant
        if (item.getProductVariant() == null) {
            throw new BadRequestException("Order item has no variant to restock");
        }

        ProductVariant pv = productVariantRepository.lockById(item.getProductVariant().getId())
                .orElseThrow(() -> new BadRequestException("Variant not found"));

        pv.setStockQuantity(pv.getStockQuantity() + acceptedQty);
    }

    private long calcAmountPaid(Long orderId) {
        List<Payment> payments = paymentRepository.findByOrder_Id(orderId);
        long sum = 0;
        for (Payment p : payments) {
            if (p.getStatus() == PaymentStatus.SUCCESS || p.getStatus() == PaymentStatus.PAID) {
                sum += p.getAmount();
            }
        }
        return sum;
    }

    public List<ReturnRequestResponseDTO> getMyRequests(Long userId) {
        return returnRequestRepository.findByOrderItem_Order_Address_User_IdOrderByIdDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ReturnRequestResponseDTO getMyRequestDetail(Long userId, Long id) {
        ReturnRequest rr = returnRequestRepository.findByIdAndOrderItem_Order_Address_User_Id(id, userId)
                .orElseThrow(() -> new BadRequestException("Return request not found"));
        return toResponse(rr);
    }

    private ReturnRequestResponseDTO toResponse(ReturnRequest rr) {
        Integer accepted = (rr.getReturnRecord() == null) ? null : rr.getReturnRecord().getAcceptedQuantity();
        return new ReturnRequestResponseDTO(
                rr.getId(),
                rr.getOrderItem().getOrder().getId(),
                rr.getOrderItem().getId(),
                rr.getRequestedQuantity(),
                accepted,
                rr.getReason(),
                rr.getStatus(),
                rr.getNote(),
                rr.getEvidenceUrls(),
                rr.getCreatedAt()
        );
    }
}