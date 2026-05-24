package com.groom.order.integration.helper;

import java.time.Instant;
import java.util.UUID;

import org.springframework.kafka.core.KafkaTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groom.common.event.Type.EventType;
import com.groom.common.event.envelope.EventEnvelope;

/**
 * Kafka 테스트 헬퍼 클래스
 */
public class KafkaTestHelper {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public KafkaTestHelper(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper, String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    /**
     * EventEnvelope을 생성하여 Kafka에 발행
     */
    public void publishEvent(EventType eventType, String aggregateType, UUID aggregateId, Object payload,
            String producer) {
        publishEvent(eventType, aggregateType, aggregateId, payload, producer, this.topic);
    }

    public void publishEvent(EventType eventType, String aggregateType, UUID aggregateId, Object payload,
            String producer, String targetTopic) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);

            EventEnvelope envelope = EventEnvelope.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType(eventType)
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId.toString())
                    .occurredAt(Instant.now())
                    .producer(producer)
                    .traceId(aggregateId.toString())
                    .version("1.0")
                    .payload(payloadJson)
                    .build();

            String envelopeJson = objectMapper.writeValueAsString(envelope);
            kafkaTemplate.send(targetTopic, aggregateId.toString(), envelopeJson);
            kafkaTemplate.flush();
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish event", e);
        }
    }

    /**
     * EventEnvelope JSON을 파싱
     */
    public EventEnvelope parseEnvelope(String json) {
        try {
            return objectMapper.readValue(json, EventEnvelope.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse EventEnvelope", e);
        }
    }

    /**
     * Payload JSON을 파싱
     */
    public <T> T parsePayload(String payloadJson, Class<T> clazz) {
        try {
            return objectMapper.readValue(payloadJson, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse payload", e);
        }
    }
}
