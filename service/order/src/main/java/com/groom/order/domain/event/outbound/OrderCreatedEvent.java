package com.groom.order.domain.event.outbound;

import java.util.UUID;

/**
 * 주문 생성 시 발행되는 이벤트
 */
public record OrderCreatedEvent(UUID orderId, Long amount) {
    public static OrderCreatedEvent of(UUID orderId, Long amount) {
        return new OrderCreatedEvent(orderId, amount);
    }

    public OrderCreatedEvent {
        if (amount < 0) {
            throw new IllegalArgumentException("금액은 0보다 커야 합니다.");
        }
    }
}