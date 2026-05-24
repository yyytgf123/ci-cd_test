package com.groom.order.infrastructure.kafka;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.groom.common.outbox.OutboxStatus;

public interface OrderOutboxRepository extends JpaRepository<OrderOutbox, UUID> {
	List<OrderOutbox> findTop100ByStatusOrderByCreatedAt(OutboxStatus status);
}
