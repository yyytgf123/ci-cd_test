package com.groom.order.domain.event.base;

import java.time.Instant;
import java.util.UUID;

// 공통 추상 클래스
public abstract class BaseDomainEvent implements DomainEvent {
	private final UUID eventId = UUID.randomUUID();
	private final Instant occurredAt = Instant.now();
	private final String aggregateId;

	protected BaseDomainEvent(String aggregateId) {
		this.aggregateId = aggregateId;
	}

	@Override public UUID getEventId() { return eventId; }
	@Override public Instant getOccurredAt() { return occurredAt; }
	@Override public String getAggregateId() { return aggregateId; }
	@Override public String getType() { return this.getClass().getSimpleName(); }
}
