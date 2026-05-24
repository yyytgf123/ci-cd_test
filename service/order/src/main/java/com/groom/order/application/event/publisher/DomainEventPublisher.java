package com.groom.order.application.event.publisher;

import com.groom.order.domain.event.base.DomainEvent;

// 1. Publisher 인터페이스 (Port)
public interface DomainEventPublisher {
	void publish(DomainEvent event);
}
