package com.groom.user.event.producer;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.groom.common.event.Type.EventType;
import com.groom.common.event.envelope.EventEnvelope;
import com.groom.common.event.payload.UserUpdatedPayload;
import com.groom.common.event.payload.UserWithdrawnPayload;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventProducer {

    private final KafkaTemplate<String, EventEnvelope> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${event.kafka.topics.user:user-lifecycle}")
    private String topic;

    public void publishUserWithdrawn(Long userId) {
        log.info("[UserEvent] UserWithdrawnEvent 발행 요청 - userId={}", userId);

        UserWithdrawnPayload payload = UserWithdrawnPayload.builder()
                .userId(UUID.nameUUIDFromBytes(userId.toString().getBytes()))
                .build();

        try {
            EventEnvelope envelope = EventEnvelope.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType(EventType.USER_WITHDRAWN)
                    .aggregateType("USER")
                    .aggregateId(userId.toString())
                    .occurredAt(java.time.Instant.now())
                    .producer("service-user")
                    .payload(objectMapper.writeValueAsString(payload))
                    .build();

            publishAfterCommit(userId.toString(), envelope);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize payload", e);
        }
    }

    public void publishUserUpdated(Long userId) {
        log.info("[UserEvent] UserUpdatedEvent 발행 요청 - userId={}", userId);

        UserUpdatedPayload payload = UserUpdatedPayload.builder()
                .userId(UUID.nameUUIDFromBytes(userId.toString().getBytes()))
                .build();

        try {
            EventEnvelope envelope = EventEnvelope.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType(EventType.USER_UPDATED)
                    .aggregateType("USER")
                    .aggregateId(userId.toString())
                    .occurredAt(java.time.Instant.now())
                    .producer("service-user")
                    .payload(objectMapper.writeValueAsString(payload))
                    .build();

            publishAfterCommit(userId.toString(), envelope);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize payload", e);
        }
    }

    private void publishAfterCommit(String key, EventEnvelope event) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            log.debug("[UserEvent] 트랜잭션 활성 상태 → afterCommit 발행 예약. eventType={}", event.getEventType());
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    log.debug("[UserEvent] afterCommit 발행 실행. eventType={}", event.getEventType());
                    kafkaTemplate.send(topic, key, event);
                }
            });
            return;
        }

        log.debug("[UserEvent] 트랜잭션 없음 → 즉시 발행. eventType={}", event.getEventType());
        kafkaTemplate.send(topic, key, event);
    }
}
