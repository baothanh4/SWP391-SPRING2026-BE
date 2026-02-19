package com.example.SWP391_SPRING2026.Controller;

import com.example.SWP391_SPRING2026.Service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/vnpay-return")
    public String vnpayReturn(HttpServletRequest request) {

        System.out.println("VNPAY RETURN CALLED");

        return paymentService.handleVnpayReturn(request);
    }
}
