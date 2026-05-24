package com.groom.order.application.event.payload;

import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter @Builder @NoArgsConstructor @AllArgsConstructor
public class OrderCancelledPayload {
    private String version;
    private UUID orderId;
    private UUID userId;
    private String cancelReason;
    private Instant cancelledAt;
}
