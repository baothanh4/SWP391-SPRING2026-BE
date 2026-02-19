package com.example.SWP391_SPRING2026.Controller;

import com.example.SWP391_SPRING2026.Service.OrderConfirmService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ghn")
@RequiredArgsConstructor
public class GhnWebhookController {

    private final OrderConfirmService orderConfirmService;

    @Value("${ghn.token}")
    private String ghnToken;

    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(
            @RequestHeader("Token") String token,
            @RequestBody Map<String, Object> body) {

        // 🔥 VERIFY TOKEN
        if (!ghnToken.equals(token)) {
            return ResponseEntity.status(403).body("Invalid token");
        }

        String ghnCode = body.get("OrderCode").toString();
        String status = body.get("Status").toString();

        orderConfirmService.updateFromWebhook(ghnCode, status);

        return ResponseEntity.ok("OK");
    }
}
