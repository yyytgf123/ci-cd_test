package com.groom.common.event.envelope;

import com.groom.common.event.Type.EventType;
import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@lombok.ToString
public class EventEnvelope {

    private String eventId; // uuid
    private EventType eventType; // ORDER_CREATED
    private String aggregateType; // ORDER
    private String aggregateId; // orderId
    private Instant occurredAt;

    private String producer; // order-service
    private String traceId; // saga trace
    private String version; // envelope schema version

    private String payload; // JSON STRING (중요)
}
