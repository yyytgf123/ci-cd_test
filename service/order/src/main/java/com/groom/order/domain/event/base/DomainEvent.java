package com.groom.order.domain.event.base;

import java.time.Instant;
import java.util.UUID;

// 공통 인터페이스
public interface DomainEvent {
	UUID getEventId();
	Instant getOccurredAt();
	String getAggregateId();
	String getType();
}

// // 9가지 구체적 이벤트
// public record OrderCreatedEvent(String aggregateId) extends BaseDomainEvent(aggregateId) {}
// public record PaymentSuccessEvent(String aggregateId) extends BaseDomainEvent(aggregateId) {}
// public record PaymentFailEvent(String aggregateId) extends BaseDomainEvent(aggregateId) {}
// public record StockDeductedEvent(String aggregateId) extends BaseDomainEvent(aggregateId) {}
// public record StockDeductionFailedEvent(String aggregateId) extends BaseDomainEvent(aggregateId) {}
// public record OrderCancelledEvent(String aggregateId) extends BaseDomainEvent(aggregateId) {}
// public record RefundSucceededEvent(String aggregateId) extends BaseDomainEvent(aggregateId) {}
// public record RefundFailedEvent(String aggregateId) extends BaseDomainEvent(aggregateId) {}
// public record OrderConfirmedEvent(String aggregateId) extends BaseDomainEvent(aggregateId) {}
