package ru.yandex.practicum.oauth0.rs.controller;

import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PaymentsController {

    @GetMapping("/payments")
    public Map<String, Object> getPayments(@RequestAttribute("tokenData") Map<String, Object> tokenData) {
        return Map.of(
            "payments", List.of(
                Map.of("id", 1, "amount", 100.0, "status", "completed"),
                Map.of("id", 2, "amount", 250.0, "status", "pending")
            ),
            "user", tokenData.get("sub"),
            "scopes", tokenData.get("scopes")
        );
    }

    @PostMapping("/payments")
    public Map<String, Object> createPayment(@RequestAttribute("tokenData") Map<String, Object> tokenData,
                                             @RequestBody Map<String, Object> paymentData) {
        return Map.of(
            "id", 3,
            "amount", paymentData.get("amount"),
            "status", "created",
            "user", tokenData.get("sub")
        );
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}