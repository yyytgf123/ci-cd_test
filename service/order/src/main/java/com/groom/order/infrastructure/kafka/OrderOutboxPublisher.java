package com.groom.order.infrastructure.kafka;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.groom.common.event.Type.EventType;
import com.groom.common.event.envelope.EventEnvelope;
import com.groom.common.outbox.OutboxStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderOutboxPublisher {

	private final OrderOutboxRepository outboxRepository;
	private final KafkaTemplate<String, EventEnvelope> kafkaTemplate;

	@Value("${event.kafka.topics.order:order-events}")
	private String topic;

	@Scheduled(fixedDelayString = "${outbox.publisher.delay-ms:1000}")
	@Transactional
	public void publish() {
		List<OrderOutbox> batch = outboxRepository.findTop100ByStatusOrderByCreatedAt(OutboxStatus.INIT);
		if (batch.isEmpty()) {
			return;
		}

		for (OrderOutbox outbox : batch) {
			EventEnvelope envelope = EventEnvelope.builder()
					.eventId(outbox.getEventId().toString())
					.eventType(EventType.valueOf(outbox.getEventType()))
					.aggregateType(outbox.getAggregateType())
					.aggregateId(outbox.getAggregateId().toString())
					.occurredAt(outbox.getCreatedAt())
					.producer(outbox.getProducer())
					.traceId(outbox.getTraceId())
					.version(outbox.getVersion())
					.payload(outbox.getPayload())
					.build();

			try {
				kafkaTemplate.send(topic, outbox.getAggregateId().toString(), envelope).get();
				outbox.markPublished();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				outbox.markFailed();
				log.error("Order outbox publish interrupted. eventId={}", outbox.getEventId(), e);
			} catch (ExecutionException e) {
				outbox.markFailed();
				log.error("Order outbox publish failed. eventId={}", outbox.getEventId(), e);
			}
		}
	}
}
