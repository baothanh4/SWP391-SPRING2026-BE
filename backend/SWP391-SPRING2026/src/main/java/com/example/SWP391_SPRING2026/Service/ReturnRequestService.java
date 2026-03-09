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

    public ReturnRequestResponseDTO submit(Long customerUserId, SubmitReturnRequestDTO dto) {
        if (dto.getOrderItemId() == null) {
            throw new BadRequestException("orderItemId is required");
        }

        if (dto.getQuantity() == null || dto.getQuantity() <= 0) {
            throw new BadRequestException("quantity must be > 0");
        }

        if (dto.getReason() == null) {
            throw new BadRequestException("reason is required");
        }

        OrderItems item = orderItemRepository.lockOwnedItem(dto.getOrderItemId(), customerUserId)
                .orElseThrow(() -> new BadRequestException("Order item not found"));

        Order order = item.getOrder();

        if (order == null || order.getAddress() == null || order.getAddress().getUser() == null) {
            throw new BadRequestException("Invalid order ownership");
        }

        if (!order.getAddress().getUser().getId().equals(customerUserId)) {
            throw new BadRequestException("You are not allowed to create return request for this order item");
        }

        if (order.getOrderStatus() != OrderStatus.COMPLETED) {
            throw new BadRequestException("Return request is only allowed when order is COMPLETED");
        }

        int purchasedQty = item.getQuantity() == null ? 0 : item.getQuantity();
        if (dto.getQuantity() > purchasedQty) {
            throw new BadRequestException("quantity cannot exceed purchased quantity");
        }

        boolean existsActiveRequest = returnRequestRepository.existsByOrderItem_IdAndStatusIn(
                item.getId(),
                EnumSet.of(
                        ReturnRequestStatus.SUBMITTED,
                        ReturnRequestStatus.WAITING_RETURN,
                        ReturnRequestStatus.RECEIVED,
                        ReturnRequestStatus.REFUND_REQUESTED
                )
        );

        if (existsActiveRequest) {
            throw new BadRequestException("This order item already has an active return request");
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

    public void approve(Long supportUserId, Long returnRequestId) {
        ReturnRequest rr = returnRequestRepository.findById(returnRequestId)
                .orElseThrow(() -> new BadRequestException("Return request not found"));

        if (rr.getStatus() != ReturnRequestStatus.SUBMITTED) {
            throw new BadRequestException("Only SUBMITTED return request can be approved");
        }

        rr.setStatus(ReturnRequestStatus.WAITING_RETURN);
        rr.setApprovedByUserId(supportUserId);
        rr.setApprovedAt(LocalDateTime.now());
        rr.setUpdatedAt(LocalDateTime.now());
    }

    public void reject(Long supportUserId, Long returnRequestId, String note) {
        ReturnRequest rr = returnRequestRepository.findById(returnRequestId)
                .orElseThrow(() -> new BadRequestException("Return request not found"));

        if (rr.getStatus() != ReturnRequestStatus.SUBMITTED) {
            throw new BadRequestException("Only SUBMITTED return request can be rejected");
        }

        rr.setStatus(ReturnRequestStatus.REJECTED);
        rr.setRejectedByUserId(supportUserId);
        rr.setRejectedNote(note);
        rr.setRejectedAt(LocalDateTime.now());
        rr.setUpdatedAt(LocalDateTime.now());
    }

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

        rr.setStatus(ReturnRequestStatus.RECEIVED);
        rr.setReceivedByUserId(operationUserId);
        rr.setReceivedAt(LocalDateTime.now());
        rr.setUpdatedAt(LocalDateTime.now());

        ReturnRecord record = new ReturnRecord();
        record.setReturnRequest(rr);
        record.setAcceptedQuantity(dto.getAcceptedQuantity());
        record.setConditionNote(dto.getConditionNote());
        record.setRestocked(false);
        record.setCreatedByUserId(operationUserId);
        record.setCreatedAt(LocalDateTime.now());
        returnRecordRepository.save(record);

        restock(item, dto.getAcceptedQuantity());
        record.setRestocked(true);

        long itemRefund = item.getPrice() * dto.getAcceptedQuantity();
        long amountPaid = calcAmountPaid(order.getId());
        long alreadyReservedRefund = calcAlreadyReservedRefund(order.getId());
        long refundableBalance = Math.max(amountPaid - alreadyReservedRefund, 0L);
        long refundAmount = Math.min(itemRefund, refundableBalance);

        if (refundAmount <= 0) {
            throw new BadRequestException("No refundable balance left for this order");
        }

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

    private long calcAlreadyReservedRefund(Long orderId) {
        Long sum = refundRequestRepository.sumRefundAmountByOrderAndStatuses(
                orderId,
                EnumSet.of(
                        RefundRequestStatus.REQUESTED,
                        RefundRequestStatus.APPROVED,
                        RefundRequestStatus.DONE
                )
        );
        return sum == null ? 0L : sum;
    }

    private void restock(OrderItems item, int acceptedQty) {
        if (Boolean.TRUE.equals(item.getIsCombo()) && item.getProductCombo() != null) {
            for (ComboItem ci : item.getProductCombo().getItems()) {
                ProductVariant pv = productVariantRepository.lockById(ci.getProductVariant().getId())
                        .orElseThrow(() -> new BadRequestException("Variant not found"));

                int add = ci.getQuantity() * acceptedQty;
                pv.setStockQuantity(pv.getStockQuantity() + add);
            }
            return;
        }

        if (item.getProductVariant() == null) {
            throw new BadRequestException("Order item has no variant to restock");
        }

        ProductVariant pv = productVariantRepository.lockById(item.getProductVariant().getId())
                .orElseThrow(() -> new BadRequestException("Variant not found"));

        pv.setStockQuantity(pv.getStockQuantity() + acceptedQty);
    }

    private long calcAmountPaid(Long orderId) {
        List<Payment> payments = paymentRepository.findByOrder_Id(orderId);
        long sum = 0L;

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
        Integer accepted = (rr.getReturnRecord() == null)
                ? null
                : rr.getReturnRecord().getAcceptedQuantity();

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