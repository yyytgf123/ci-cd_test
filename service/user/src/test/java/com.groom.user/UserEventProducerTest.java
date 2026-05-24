package com.groom.user;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.groom.common.event.envelope.EventEnvelope;
import com.groom.user.event.producer.UserEventProducer;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserEventProducer 테스트")
class UserEventProducerTest {

    @Mock
    private KafkaTemplate<String, EventEnvelope> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private UserEventProducer userEventProducer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userEventProducer, "topic", "user-lifecycle");
    }

    @Nested
    @DisplayName("publishUserWithdrawn() 테스트")
    class PublishUserWithdrawnTest {

        @Test
        @DisplayName("트랜잭션 없이 즉시 발행")
        void publishUserWithdrawn_NoTransaction_ImmediatePublish() throws JsonProcessingException {
            Long userId = 1L;

            try (MockedStatic<TransactionSynchronizationManager> tsm = mockStatic(TransactionSynchronizationManager.class)) {
                tsm.when(TransactionSynchronizationManager::isActualTransactionActive).thenReturn(false);
                given(objectMapper.writeValueAsString(any())).willReturn("{\"userId\":\"test\"}");

                userEventProducer.publishUserWithdrawn(userId);

                verify(kafkaTemplate).send(eq("user-lifecycle"), eq("1"), any(EventEnvelope.class));
            }
        }

        @Test
        @DisplayName("트랜잭션 활성 상태에서 afterCommit 발행 예약")
        void publishUserWithdrawn_WithTransaction_AfterCommit() throws JsonProcessingException {
            Long userId = 2L;

            try (MockedStatic<TransactionSynchronizationManager> tsm = mockStatic(TransactionSynchronizationManager.class)) {
                tsm.when(TransactionSynchronizationManager::isActualTransactionActive).thenReturn(true);
                given(objectMapper.writeValueAsString(any())).willReturn("{\"userId\":\"test\"}");

                userEventProducer.publishUserWithdrawn(userId);

                tsm.verify(() -> TransactionSynchronizationManager.registerSynchronization(any(TransactionSynchronization.class)));
                verify(kafkaTemplate, never()).send(anyString(), anyString(), any(EventEnvelope.class));
            }
        }

        @Test
        @DisplayName("직렬화 실패 시 에러 로그만 남기고 계속 진행")
        void publishUserWithdrawn_SerializationError() throws JsonProcessingException {
            Long userId = 3L;

            try (MockedStatic<TransactionSynchronizationManager> tsm = mockStatic(TransactionSynchronizationManager.class)) {
                tsm.when(TransactionSynchronizationManager::isActualTransactionActive).thenReturn(false);
                given(objectMapper.writeValueAsString(any())).willThrow(new JsonProcessingException("Serialization error") {});

                assertThatCode(() -> userEventProducer.publishUserWithdrawn(userId))
                        .doesNotThrowAnyException();

                verify(kafkaTemplate, never()).send(anyString(), anyString(), any(EventEnvelope.class));
            }
        }
    }

    @Nested
    @DisplayName("publishUserUpdated() 테스트")
    class PublishUserUpdatedTest {

        @Test
        @DisplayName("트랜잭션 없이 즉시 발행")
        void publishUserUpdated_NoTransaction_ImmediatePublish() throws JsonProcessingException {
            Long userId = 10L;

            try (MockedStatic<TransactionSynchronizationManager> tsm = mockStatic(TransactionSynchronizationManager.class)) {
                tsm.when(TransactionSynchronizationManager::isActualTransactionActive).thenReturn(false);
                given(objectMapper.writeValueAsString(any())).willReturn("{\"userId\":\"test\"}");

                userEventProducer.publishUserUpdated(userId);

                verify(kafkaTemplate).send(eq("user-lifecycle"), eq("10"), any(EventEnvelope.class));
            }
        }

        @Test
        @DisplayName("트랜잭션 활성 상태에서 afterCommit 발행 예약")
        void publishUserUpdated_WithTransaction_AfterCommit() throws JsonProcessingException {
            Long userId = 20L;

            try (MockedStatic<TransactionSynchronizationManager> tsm = mockStatic(TransactionSynchronizationManager.class)) {
                tsm.when(TransactionSynchronizationManager::isActualTransactionActive).thenReturn(true);
                given(objectMapper.writeValueAsString(any())).willReturn("{\"userId\":\"test\"}");

                userEventProducer.publishUserUpdated(userId);

                tsm.verify(() -> TransactionSynchronizationManager.registerSynchronization(any(TransactionSynchronization.class)));
                verify(kafkaTemplate, never()).send(anyString(), anyString(), any(EventEnvelope.class));
            }
        }

        @Test
        @DisplayName("직렬화 실패 시 에러 로그만 남기고 계속 진행")
        void publishUserUpdated_SerializationError() throws JsonProcessingException {
            Long userId = 30L;

            try (MockedStatic<TransactionSynchronizationManager> tsm = mockStatic(TransactionSynchronizationManager.class)) {
                tsm.when(TransactionSynchronizationManager::isActualTransactionActive).thenReturn(false);
                given(objectMapper.writeValueAsString(any())).willThrow(new JsonProcessingException("Serialization error") {});

                assertThatCode(() -> userEventProducer.publishUserUpdated(userId))
                        .doesNotThrowAnyException();

                verify(kafkaTemplate, never()).send(anyString(), anyString(), any(EventEnvelope.class));
            }
        }
    }

    @Nested
    @DisplayName("afterCommit 콜백 테스트")
    class AfterCommitCallbackTest {

        @Test
        @DisplayName("afterCommit 콜백 실행 시 Kafka 발행")
        void afterCommit_SendsToKafka() throws JsonProcessingException {
            Long userId = 100L;

            try (MockedStatic<TransactionSynchronizationManager> tsm = mockStatic(TransactionSynchronizationManager.class)) {
                tsm.when(TransactionSynchronizationManager::isActualTransactionActive).thenReturn(true);
                given(objectMapper.writeValueAsString(any())).willReturn("{\"userId\":\"test\"}");

                ArgumentCaptor<TransactionSynchronization> syncCaptor = ArgumentCaptor.forClass(TransactionSynchronization.class);

                userEventProducer.publishUserWithdrawn(userId);

                tsm.verify(() -> TransactionSynchronizationManager.registerSynchronization(syncCaptor.capture()));

                // afterCommit 콜백 수동 실행
                TransactionSynchronization sync = syncCaptor.getValue();
                sync.afterCommit();

                verify(kafkaTemplate).send(eq("user-lifecycle"), eq("100"), any(EventEnvelope.class));
            }
        }
    }
}
