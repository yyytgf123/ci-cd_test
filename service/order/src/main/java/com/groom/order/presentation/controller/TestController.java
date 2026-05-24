package com.groom.order.presentation.controller;

import java.util.UUID;

import com.groom.common.event.Type.EventType;
import com.groom.common.event.payload.OrderCreatedPayload;
import com.groom.order.infrastructure.kafka.OrderOutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TestController {

    private final OrderOutboxService outboxService;

    @GetMapping("/test/order/{orderId}")
    public String testSend(@PathVariable UUID orderId) {
        try {
            outboxService.save(
                    EventType.ORDER_CREATED,
                    "ORDER",
                    orderId,
                    orderId.toString(),
                    OrderCreatedPayload.builder()
                            .orderId(orderId)
                            .userId(UUID.randomUUID())
                            .totalAmount(50000L)
                            .build());
            return "Outbox 저장 완료: " + orderId;
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage() + " | " + e.getClass().getName();
        }
    }
}
