package com.groom.common.infrastructure.kafka.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;
import com.groom.common.event.envelope.EventEnvelope;

@Configuration
public class KafkaConfig {

        @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
        private String bootstrapServers;

        @Value("${spring.kafka.consumer.group-id:default-group}")
        private String groupId;

        // --- Producer Configuration ---
        @Bean
        public ProducerFactory<String, EventEnvelope> producerFactory() {
                Map<String, Object> config = new HashMap<>();
                config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
                config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
                config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

                // Idempotence & Reliability
                config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true); // 멱등성 보장
                config.put(ProducerConfig.ACKS_CONFIG, "all"); // 모든 리플리카 승인 대기
                config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5); // 순서 보장 한도 내 최대값

                // Batch & Throughput
                config.put(ProducerConfig.LINGER_MS_CONFIG, 5); // 5ms 대기
                config.put(ProducerConfig.BATCH_SIZE_CONFIG, 32768); // 32KB 배치 크기

                return new DefaultKafkaProducerFactory<>(config);
        }

        @Bean
        public KafkaTemplate<String, EventEnvelope> kafkaTemplate() {
                return new KafkaTemplate<>(producerFactory());
        }

        // --- Consumer Configuration ---
        @Bean
        public ConsumerFactory<String, EventEnvelope> consumerFactory() {
                Map<String, Object> config = new HashMap<>();
                config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
                config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
                config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                                org.springframework.kafka.support.serializer.ErrorHandlingDeserializer.class);
                config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                                org.springframework.kafka.support.serializer.ErrorHandlingDeserializer.class);

                // Configure ErrorHandlingDeserializer delegates
                config.put(org.springframework.kafka.support.serializer.ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS,
                                StringDeserializer.class);
                config.put(org.springframework.kafka.support.serializer.ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS,
                                JsonDeserializer.class);

                // Offset & Commit
                config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // 수동 커밋
                config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"); // 처음부터 읽기

                // Tuning
                config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 50); // 배치 처리 갯수 제한 (타임아웃 방지)

                // Rebalance Strategy (CooperativeStickyAssignor: 점진적 재균형으로 가용성 향상)
                config.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG,
                                "org.apache.kafka.clients.consumer.CooperativeStickyAssignor");

                // Trusted Packages & Type Mapping
                config.put(JsonDeserializer.TRUSTED_PACKAGES, "*"); // 모든 패키지 허용
                config.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false); // 헤더 정보 무시
                config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, EventEnvelope.class); // 기본 타입 설정

                return new DefaultKafkaConsumerFactory<>(config);
        }

        @Bean
        public ConcurrentKafkaListenerContainerFactory<String, EventEnvelope> kafkaListenerContainerFactory(
                        KafkaTemplate<String, EventEnvelope> kafkaTemplate) {

                ConcurrentKafkaListenerContainerFactory<String, EventEnvelope> factory = new ConcurrentKafkaListenerContainerFactory<>();
                factory.setConsumerFactory(consumerFactory());

                // Manual Immediate Ack Mode (데이터 정합성 최우선)
                factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

                // Error Handling & Recovery (Retry + DLT)
                // 1초 간격, 최대 3회 재시도 (FixedBackOff)
                CommonErrorHandler errorHandler = new DefaultErrorHandler(
                                new DeadLetterPublishingRecoverer(kafkaTemplate), // 최종 실패 시 DLT로 발행
                                new FixedBackOff(1000L, 3) // 1초 간격, 3회 시도
                );
                factory.setCommonErrorHandler(errorHandler);

                return factory;
        }
}
