package com.groom.order.infrastructure.kafka;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.groom.common.event.Type.EventType;
import com.groom.common.event.envelope.EventEnvelope;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderOutboxService {

	private final OrderOutboxRepository outboxRepository;
	private final ObjectMapper objectMapper;

	@Value("${spring.application.name}")
	private String producer;

	@Value("${event.envelope.version:1.0}")
	private String version;

	@Transactional
	public void save(EventType eventType, String aggregateType, UUID aggregateId, String traceId, Object payload) {
		String payloadJson = toJson(payload);
		EventEnvelope envelope = EventEnvelope.builder()
			.eventId(UUID.randomUUID().toString())
			.eventType(eventType)
			.aggregateType(aggregateType)
			.aggregateId(aggregateId.toString())
			.occurredAt(java.time.Instant.now())
			.producer(producer)
			.traceId(traceId)
			.version(version)
			.payload(payloadJson)
			.build();
		outboxRepository.save(OrderOutbox.of(envelope));
	}

	private String toJson(Object payload) {
		try {
			return objectMapper.writeValueAsString(payload);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Failed to serialize event payload", e);
		}
	}
}
