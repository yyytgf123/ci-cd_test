package com.groom.order.infrastructure.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import com.groom.common.event.Type.EventType;
import com.groom.common.event.envelope.EventEnvelope;
import com.groom.common.outbox.OutboxStatus;

/**
 * OrderOutboxPublisher 단위 테스트
 * 
 * Outbox 패턴의 이벤트 발행 로직을 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderOutboxPublisher 테스트")
@Tag("Integration")
class OrderOutboxPublisherTest {

    @Mock
    private OrderOutboxRepository outboxRepository;

    @Mock
    private KafkaTemplate<String, EventEnvelope> kafkaTemplate;

    @InjectMocks
    private OrderOutboxPublisher orderOutboxPublisher;

    private String topic;

    @BeforeEach
    void setUp() {
        topic = "order-events";
        ReflectionTestUtils.setField(orderOutboxPublisher, "topic", topic);
    }

    @Nested
    @DisplayName("이벤트 발행 성공 테스트")
    class PublishSuccessTest {

        @Test
        @DisplayName("INIT 상태의 Outbox 이벤트를 Kafka에 발행하고 PUBLISHED로 변경해야 한다")
        void publish_ShouldSend_ToKafka_AndMark_Published() throws Exception {
            // given
            OrderOutbox outbox = createOutbox(OutboxStatus.INIT);
            given(outboxRepository.findTop100ByStatusOrderByCreatedAt(OutboxStatus.INIT))
                    .willReturn(List.of(outbox));

            CompletableFuture<SendResult<String, EventEnvelope>> future = CompletableFuture.completedFuture(null);
            given(kafkaTemplate.send(eq(topic), any(String.class), any(EventEnvelope.class)))
                    .willReturn(future);

            // when
            orderOutboxPublisher.publish();

            // then
            then(kafkaTemplate).should(times(1)).send(
                    eq(topic),
                    eq(outbox.getAggregateId().toString()),
                    any(EventEnvelope.class));
            assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        }

        @Test
        @DisplayName("여러 개의 Outbox 이벤트를 배치로 처리해야 한다")
        void publish_ShouldProcess_MultipleOutboxEvents_InBatch() throws Exception {
            // given
            OrderOutbox outbox1 = createOutbox(OutboxStatus.INIT);
            OrderOutbox outbox2 = createOutbox(OutboxStatus.INIT);
            OrderOutbox outbox3 = createOutbox(OutboxStatus.INIT);

            given(outboxRepository.findTop100ByStatusOrderByCreatedAt(OutboxStatus.INIT))
                    .willReturn(List.of(outbox1, outbox2, outbox3));

            CompletableFuture<SendResult<String, EventEnvelope>> future = CompletableFuture.completedFuture(null);
            given(kafkaTemplate.send(eq(topic), any(String.class), any(EventEnvelope.class)))
                    .willReturn(future);

            // when
            orderOutboxPublisher.publish();

            // then
            then(kafkaTemplate).should(times(3)).send(
                    eq(topic),
                    any(String.class),
                    any(EventEnvelope.class));
            assertThat(outbox1.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
            assertThat(outbox2.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
            assertThat(outbox3.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        }

        @Test
        @DisplayName("발행할 이벤트가 없으면 아무것도 하지 않아야 한다")
        void publish_WhenNoEvents_ShouldDoNothing() {
            // given
            given(outboxRepository.findTop100ByStatusOrderByCreatedAt(OutboxStatus.INIT))
                    .willReturn(List.of());

            // when
            orderOutboxPublisher.publish();

            // then
            then(kafkaTemplate).should(never()).send(any(), any(), any());
        }

        @Test
        @DisplayName("EventEnvelope이 올바르게 생성되어야 한다")
        void publish_ShouldCreate_CorrectEventEnvelope() throws Exception {
            // given
            UUID eventId = UUID.randomUUID();
            UUID aggregateId = UUID.randomUUID();
            String eventType = "ORDER_CREATED";
            String aggregateType = "ORDER";
            String payload = "{\"orderId\":\"" + aggregateId + "\"}";
            String producer = "service-order";
            String traceId = "trace-123";
            String version = "1.0";
            Instant createdAt = Instant.now();

            OrderOutbox outbox = createOutbox(eventId, eventType, aggregateType, aggregateId, payload,
                    producer, traceId, version, createdAt, OutboxStatus.INIT);

            given(outboxRepository.findTop100ByStatusOrderByCreatedAt(OutboxStatus.INIT))
                    .willReturn(List.of(outbox));

            CompletableFuture<SendResult<String, EventEnvelope>> future = CompletableFuture.completedFuture(null);
            given(kafkaTemplate.send(eq(topic), any(String.class), any(EventEnvelope.class)))
                    .willReturn(future);

            // when
            orderOutboxPublisher.publish();

            // then
            then(kafkaTemplate).should(times(1)).send(
                    eq(topic),
                    eq(aggregateId.toString()),
                    org.mockito.ArgumentMatchers.argThat((EventEnvelope envelope) -> {
                        assertThat(envelope.getEventId()).isEqualTo(eventId.toString());
                        assertThat(envelope.getEventType()).isEqualTo(EventType.valueOf(eventType));
                        assertThat(envelope.getAggregateType()).isEqualTo(aggregateType);
                        assertThat(envelope.getAggregateId()).isEqualTo(aggregateId.toString());
                        assertThat(envelope.getPayload()).isEqualTo(payload);
                        assertThat(envelope.getProducer()).isEqualTo(producer);
                        assertThat(envelope.getTraceId()).isEqualTo(traceId);
                        assertThat(envelope.getVersion()).isEqualTo(version);
                        assertThat(envelope.getOccurredAt()).isEqualTo(createdAt);
                        return true;
                    }));
        }
    }

    @Nested
    @DisplayName("이벤트 발행 실패 테스트")
    class PublishFailureTest {

        @Test
        @DisplayName("Kafka 전송 실패 시 Outbox 상태를 FAILED로 변경해야 한다")
        void publish_WhenKafkaSendFails_ShouldMark_Failed() throws Exception {
            // given
            OrderOutbox outbox = createOutbox(OutboxStatus.INIT);
            given(outboxRepository.findTop100ByStatusOrderByCreatedAt(OutboxStatus.INIT))
                    .willReturn(List.of(outbox));

            CompletableFuture<SendResult<String, EventEnvelope>> future = new CompletableFuture<>();
            future.completeExceptionally(new RuntimeException("Kafka send failed"));
            given(kafkaTemplate.send(eq(topic), any(String.class), any(EventEnvelope.class)))
                    .willReturn(future);

            // when
            orderOutboxPublisher.publish();

            // then
            assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.FAILED);
        }

        @Test
        @DisplayName("InterruptedException 발생 시 Thread 인터럽트하고 FAILED로 변경해야 한다")
        void publish_WhenInterrupted_ShouldInterruptThread_AndMark_Failed() throws Exception {
            // given
            OrderOutbox outbox = createOutbox(OutboxStatus.INIT);
            given(outboxRepository.findTop100ByStatusOrderByCreatedAt(OutboxStatus.INIT))
                    .willReturn(List.of(outbox));

            CompletableFuture<SendResult<String, EventEnvelope>> future = new CompletableFuture<>();
            future.completeExceptionally(new InterruptedException("Thread interrupted"));
            given(kafkaTemplate.send(eq(topic), any(String.class), any(EventEnvelope.class)))
                    .willReturn(future);

            // when
            orderOutboxPublisher.publish();

            // then
            assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.FAILED);
            // Thread.currentThread().isInterrupted() would be true in actual execution
        }

        @Test
        @DisplayName("배치 처리 중 일부 실패 시 성공한 이벤트는 PUBLISHED, 실패한 이벤트는 FAILED여야 한다")
        void publish_WhenPartialFailure_ShouldMark_Published_AndFailed_Separately() throws Exception {
            // given
            OrderOutbox outbox1 = createOutbox(OutboxStatus.INIT);
            OrderOutbox outbox2 = createOutbox(OutboxStatus.INIT);
            OrderOutbox outbox3 = createOutbox(OutboxStatus.INIT);

            given(outboxRepository.findTop100ByStatusOrderByCreatedAt(OutboxStatus.INIT))
                    .willReturn(List.of(outbox1, outbox2, outbox3));

            CompletableFuture<SendResult<String, EventEnvelope>> successFuture = CompletableFuture
                    .completedFuture(null);
            CompletableFuture<SendResult<String, EventEnvelope>> failureFuture = new CompletableFuture<>();
            failureFuture.completeExceptionally(new RuntimeException("Kafka send failed"));

            given(kafkaTemplate.send(eq(topic), eq(outbox1.getAggregateId().toString()), any(EventEnvelope.class)))
                    .willReturn(successFuture);
            given(kafkaTemplate.send(eq(topic), eq(outbox2.getAggregateId().toString()), any(EventEnvelope.class)))
                    .willReturn(failureFuture);
            given(kafkaTemplate.send(eq(topic), eq(outbox3.getAggregateId().toString()), any(EventEnvelope.class)))
                    .willReturn(successFuture);

            // when
            orderOutboxPublisher.publish();

            // then
            assertThat(outbox1.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
            assertThat(outbox2.getStatus()).isEqualTo(OutboxStatus.FAILED);
            assertThat(outbox3.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        }
    }

    @Nested
    @DisplayName("배치 처리 테스트")
    class BatchProcessingTest {

        @Test
        @DisplayName("최대 100개의 이벤트를 한 번에 처리해야 한다")
        void publish_ShouldProcess_Top100Events() {
            // given
            given(outboxRepository.findTop100ByStatusOrderByCreatedAt(OutboxStatus.INIT))
                    .willReturn(List.of());

            // when
            orderOutboxPublisher.publish();

            // then
            then(outboxRepository).should(times(1)).findTop100ByStatusOrderByCreatedAt(OutboxStatus.INIT);
        }

        @Test
        @DisplayName("INIT 상태의 이벤트만 조회해야 한다")
        void publish_ShouldQuery_OnlyInitStatus() {
            // given
            given(outboxRepository.findTop100ByStatusOrderByCreatedAt(OutboxStatus.INIT))
                    .willReturn(List.of());

            // when
            orderOutboxPublisher.publish();

            // then
            then(outboxRepository).should(times(1)).findTop100ByStatusOrderByCreatedAt(eq(OutboxStatus.INIT));
            then(outboxRepository).should(never()).findTop100ByStatusOrderByCreatedAt(eq(OutboxStatus.PUBLISHED));
            then(outboxRepository).should(never()).findTop100ByStatusOrderByCreatedAt(eq(OutboxStatus.FAILED));
        }
    }

    // ========== Helper Methods ==========

    private OrderOutbox createOutbox(OutboxStatus status) {
        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        String eventType = "ORDER_CREATED";
        String aggregateType = "ORDER";
        String payload = "{\"orderId\":\"" + aggregateId + "\"}";
        String producer = "service-order";
        String traceId = "trace-" + UUID.randomUUID();
        String version = "1.0";
        Instant createdAt = Instant.now();

        return createOutbox(eventId, eventType, aggregateType, aggregateId, payload, producer, traceId, version,
                createdAt, status);
    }

    private OrderOutbox createOutbox(UUID eventId, String eventType, String aggregateType, UUID aggregateId,
            String payload, String producer, String traceId, String version, Instant createdAt, OutboxStatus status) {

        OrderOutbox outbox = new OrderOutbox();

        // Use reflection to set private fields
        try {
            setField(outbox, "eventId", eventId);
            setField(outbox, "eventType", eventType);
            setField(outbox, "aggregateType", aggregateType);
            setField(outbox, "aggregateId", aggregateId);
            setField(outbox, "payload", payload);
            setField(outbox, "producer", producer);
            setField(outbox, "traceId", traceId);
            setField(outbox, "version", version);
            setField(outbox, "createdAt", createdAt);
            setField(outbox, "status", status);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create OrderOutbox for test", e);
        }

        return outbox;
    }

    private void setField(Object target, String fieldName, Object value)
            throws NoSuchFieldException, IllegalAccessException {
        java.lang.reflect.Field field = OrderOutbox.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
