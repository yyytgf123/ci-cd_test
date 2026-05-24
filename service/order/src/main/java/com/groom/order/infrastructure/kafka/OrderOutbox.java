package com.groom.order.infrastructure.kafka;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.groom.common.event.envelope.EventEnvelope;
import com.groom.common.outbox.OutboxStatus;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "order_outbox")
public class OrderOutbox {

    @Id
    @GeneratedValue
    private UUID id;

    private UUID eventId;
    private String eventType;
    private String aggregateType;
    private UUID aggregateId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String payload;

    private String traceId;
    private String producer;
    private String version;

    @Enumerated(EnumType.STRING)
    private OutboxStatus status;

    private Instant createdAt;

    public static OrderOutbox of(EventEnvelope envelope) {
        OrderOutbox o = new OrderOutbox();
        o.eventId = UUID.fromString(envelope.getEventId());
        o.eventType = envelope.getEventType().name();
        o.aggregateType = envelope.getAggregateType();
        o.aggregateId = UUID.fromString(envelope.getAggregateId());
        o.payload = envelope.getPayload(); // JSON String
        o.traceId = envelope.getTraceId();
        o.producer = envelope.getProducer();
        o.version = envelope.getVersion();
        o.status = OutboxStatus.INIT;
        o.createdAt = envelope.getOccurredAt() != null ? envelope.getOccurredAt() : Instant.now();
        return o;
    }

    public void markPublished() {
        this.status = OutboxStatus.PUBLISHED;
    }

    public void markFailed() {
        this.status = OutboxStatus.FAILED;
    }
}
