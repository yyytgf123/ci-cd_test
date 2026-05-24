package com.groom.order.infrastructure.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.groom.common.event.Type.EventType;
import com.groom.common.event.payload.OrderCreatedPayload;
import com.groom.common.outbox.OutboxStatus;

/**
 * OrderOut boxServiceTest 단위테스트
 * 
 * Outbox 패턴의 이벤트 저장 로직을 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderOutboxService 테스트")
class OrderOutboxServiceTest {

    @Mock
    private OrderOutboxRepository outboxRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OrderOutboxService outboxService;

    private String producer;
    private String version;

    @BeforeEach
    void setUp() {
        producer = "service-order";
        version = "1.0";
        ReflectionTestUtils.setField(outboxService, "producer", producer);
        ReflectionTestUtils.setField(outboxService, "version", version);
    }

    @Test
    @DisplayName("이벤트를 JSON으로 직렬화하여 Outbox에 저장해야 한다")
    void save_ShouldSerialize_AndSave_ToOutbox() throws JsonProcessingException {
        // given
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        OrderCreatedPayload payload = OrderCreatedPayload.builder()
                .orderId(orderId)
                .userId(userId)
                .totalAmount(50000L)
                .build();

        String payloadJson = "{\"orderId\":\"" + orderId + "\",\"userId\":\"" + userId + "\",\"totalAmount\":50000}";
        given(objectMapper.writeValueAsString(payload))
                .willReturn(payloadJson);

        // when
        outboxService.save(EventType.ORDER_CREATED, "ORDER", orderId, "trace-123", payload);

        // then
        then(objectMapper).should(times(1)).writeValueAsString(payload);
        then(outboxRepository).should(times(1)).save(argThat((OrderOutbox outbox) -> {
            assertThat(outbox.getEventType()).isEqualTo("ORDER_CREATED");
            assertThat(outbox.getAggregateType()).isEqualTo("ORDER");
            assertThat(outbox.getAggregateId()).isEqualTo(orderId);
            assertThat(outbox.getTraceId()).isEqualTo("trace-123");
            assertThat(outbox.getPayload()).isEqualTo(payloadJson);
            assertThat(outbox.getProducer()).isEqualTo(producer);
            assertThat(outbox.getVersion()).isEqualTo(version);
            assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.INIT);
            assertThat(outbox.getEventId()).isNotNull();
            assertThat(outbox.getCreatedAt()).isNotNull();
            return true;
        }));
    }

    @Test
    @DisplayName("JSON 직렬화 실패 시 IllegalStateException을 던져야 한다")
    void save_WhenSerializationFails_ShouldThrow_IllegalStateException() throws JsonProcessingException {
        // given
        UUID orderId = UUID.randomUUID();
        Object invalidPayload = new Object();

        given(objectMapper.writeValueAsString(invalidPayload))
                .willThrow(new JsonProcessingException("Serialization failed") {
                });

        // when & then
        assertThatThrownBy(() -> outboxService.save(EventType.ORDER_CREATED, "ORDER", orderId, "trace-123",
                invalidPayload))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to serialize event payload")
                .hasCauseInstanceOf(JsonProcessingException.class);

        then(outboxRepository).should(times(0)).save(any());
    }

    @Test
    @DisplayName("설정된 producer와 version이 EventEnvelope에 포함되어야 한다")
    void save_ShouldInclude_ProducerAndVersion() throws JsonProcessingException {
        // given
        UUID orderId = UUID.randomUUID();
        String customProducer = "custom-service";
        String customVersion = "2.0";
        ReflectionTestUtils.setField(outboxService, "producer", customProducer);
        ReflectionTestUtils.setField(outboxService, "version", customVersion);

        OrderCreatedPayload payload = OrderCreatedPayload.builder()
                .orderId(orderId)
                .userId(UUID.randomUUID())
                .totalAmount(50000L)
                .build();

        given(objectMapper.writeValueAsString(payload))
                .willReturn("{}");

        // when
        outboxService.save(EventType.ORDER_CREATED, "ORDER", orderId, null, payload);

        // then
        then(outboxRepository).should(times(1)).save(argThat((OrderOutbox outbox) -> {
            assertThat(outbox.getProducer()).isEqualTo(customProducer);
            assertThat(outbox.getVersion()).isEqualTo(customVersion);
            return true;
        }));
    }

    @Test
    @DisplayName("여러 이벤트 타입을 저장할 수 있어야 한다")
    void save_ShouldSave_DifferentEventTypes() throws JsonProcessingException {
        // given
        UUID orderId = UUID.randomUUID();
        OrderCreatedPayload payload = OrderCreatedPayload.builder()
                .orderId(orderId)
                .userId(UUID.randomUUID())
                .totalAmount(50000L)
                .build();

        given(objectMapper.writeValueAsString(any()))
                .willReturn("{}");

        // when
        outboxService.save(EventType.ORDER_CREATED, "ORDER", orderId, null, payload);
        outboxService.save(EventType.ORDER_CANCELLED, "ORDER", orderId, null, payload);
        outboxService.save(EventType.ORDER_CONFIRMED, "ORDER", orderId, null, payload);

        // then
        then(outboxRepository).should(times(3)).save(any(OrderOutbox.class));
    }
}
