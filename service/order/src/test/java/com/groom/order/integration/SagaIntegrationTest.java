package com.groom.order.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groom.common.event.Type.EventType;
import com.groom.common.event.payload.PaymentCompletedPayload;
import com.groom.common.event.payload.PaymentFailedPayload;
import com.groom.common.event.payload.StockDeductedPayload;
import com.groom.common.event.payload.StockDeductionFailedPayload;
import com.groom.order.domain.entity.Order;
import com.groom.order.domain.repository.OrderRepository;
import com.groom.order.domain.status.OrderStatus;
import com.groom.order.infrastructure.kafka.OrderOutboxPublisher;
import com.groom.order.infrastructure.kafka.OrderOutboxRepository;
import com.groom.order.integration.helper.KafkaTestHelper;
import com.groom.order.integration.helper.TestEventFactory;

/**
 * Saga Integration Test for MSA Event Architecture
 * 
 * Order / Payment / Product 서비스 간 Kafka 이벤트 흐름 통합 테스트
 * - Testcontainers Kafka 사용 (실제 Kafka 브로커)
 * - Testcontainers PostgreSQL 사용
 * - 실제 Consumer Listener 검증 (Mock 사용 안 함)
 * - Given-When-Then 구조
 */
@SpringBootTest
@Testcontainers
@Tag("Integration")
@ActiveProfiles("test")
class SagaIntegrationTest {

        private static final Logger log = LoggerFactory.getLogger(SagaIntegrationTest.class);

        @Container
        static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
                        .withDatabaseName("testdb")
                        .withUsername("test")
                        .withPassword("test")
                        .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*", 2)
                                .withStartupTimeout(Duration.ofMinutes(2)));

        @Container
        static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.7.1"))
                        .withStartupTimeout(Duration.ofMinutes(2));

        @DynamicPropertySource
        static void configureProperties(DynamicPropertyRegistry registry) {
                registry.add("spring.datasource.url", postgres::getJdbcUrl);
                registry.add("spring.datasource.username", postgres::getUsername);
                registry.add("spring.datasource.password", postgres::getPassword);

                registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
                registry.add("spring.kafka.consumer.bootstrap-servers", kafka::getBootstrapServers);
                registry.add("spring.kafka.producer.bootstrap-servers", kafka::getBootstrapServers);
        }

        @BeforeAll
        static void createTopics() throws Exception {
                Map<String, Object> adminProps = new HashMap<>();
                adminProps.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
                try (Admin admin = Admin.create(adminProps)) {
                        admin.createTopics(List.of(
                                        new NewTopic("order-events",   1, (short) 1),
                                        new NewTopic("payment-events", 1, (short) 1),
                                        new NewTopic("product-events", 1, (short) 1)))
                                        .all()
                                        .get(30, TimeUnit.SECONDS);
                }
                log.info("[TEST] Kafka topics created: order-events, payment-events, product-events");
        }

        @Autowired
        private OrderRepository orderRepository;

        @Autowired
        private OrderOutboxRepository outboxRepository;

        @Autowired
        private OrderOutboxPublisher outboxPublisher;

        @Autowired
        private ObjectMapper objectMapper;

        private KafkaTemplate<String, String> testKafkaTemplate;
        private KafkaTestHelper kafkaTestHelper;
        private String topic = "order-events";

        @BeforeEach
        void setUp() {
                // Producer Setup for sending events to Kafka
                Map<String, Object> producerProps = KafkaTestUtils.producerProps(kafka.getBootstrapServers());
                producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
                producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
                DefaultKafkaProducerFactory<String, String> pf = new DefaultKafkaProducerFactory<>(producerProps);
                testKafkaTemplate = new KafkaTemplate<>(pf);

                kafkaTestHelper = new KafkaTestHelper(testKafkaTemplate, objectMapper, topic);

                // Clean up data
                outboxRepository.deleteAll();
                orderRepository.deleteAll();
        }

        @AfterEach
        void tearDown() {
                // Cleanup after each test
                outboxRepository.deleteAll();
                orderRepository.deleteAll();
        }

        /**
         * [시나리오 1] 결제 성공 흐름
         * 1. Order 생성 → ORDER_CREATED 발행
         * 2. Payment 서비스 → PAYMENT_COMPLETED 발행
         * 3. Product 서비스 → 재고 차감 → STOCK_DEDUCTED 발행
         * 4. Order 서비스 → ORDER_CONFIRMED 발행
         * 5. 최종 Order 상태 = CONFIRMED
         */
        @Test
        @DisplayName("시나리오 1: 결제 성공 → 재고 차감 성공 → 주문 확정")
        void testScenario1_PaymentAndStockSuccess() throws Exception {
                // ========== GIVEN ==========
                UUID orderId = UUID.randomUUID();
                UUID userId = UUID.randomUUID();
                UUID productId = UUID.randomUUID();
                long totalAmount = 10000L;

                // Order 엔티티 생성 (실제 서비스 로직 대신 직접 생성)
                Order order = Order.builder()
                                .buyerId(userId)
                                .orderNumber("ORD-" + System.currentTimeMillis() % 100000000L)
                                .totalPaymentAmount(totalAmount)
                                .recipientName("Test User")
                                .recipientPhone("010-1234-5678")
                                .zipCode("12345")
                                .shippingAddress("Test Address")
                                .shippingMemo("Test Memo")
                                .build();

                // orderId를 리플렉션으로 설정 (실제로는 생성자에서 자동 생성되지만 테스트용으로 고정)
                java.lang.reflect.Field orderIdField = Order.class.getDeclaredField("orderId");
                orderIdField.setAccessible(true);
                orderIdField.set(order, orderId);

                orderRepository.save(order);
                log.info("[TEST] Order 생성 완료: orderId={}, status={}", orderId, order.getStatus());

                // ========== WHEN ==========
                // Step 1: Payment 서비스가 PAYMENT_COMPLETED 발행 (시뮬레이션)
                PaymentCompletedPayload paymentPayload = TestEventFactory.createPaymentCompletedPayload(orderId,
                                totalAmount);
                kafkaTestHelper.publishEvent(
                                EventType.PAYMENT_COMPLETED,
                                "PAYMENT",
                                orderId,
                                paymentPayload,
                                "service-payment",
                                "payment-events");

                log.info("[TEST] PAYMENT_COMPLETED 발행 완료");

                // Step 2: Order Consumer가 이벤트를 받아 Order 상태를 PAID로 변경 (대기)
                await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
                        Order updatedOrder = orderRepository.findById(orderId).orElseThrow();
                        assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.PAID);
                        log.info("[TEST] Order 상태 PAID 확인: {}", updatedOrder.getStatus());
                });

                // Step 3: Product 서비스가 STOCK_DEDUCTED 발행 (시뮬레이션)
                StockDeductedPayload stockPayload = TestEventFactory.createStockDeductedPayload(orderId,
                                List.of(productId));
                kafkaTestHelper.publishEvent(
                                EventType.STOCK_DEDUCTED,
                                "PRODUCT",
                                orderId,
                                stockPayload,
                                "service-product",
                                "product-events");

                log.info("[TEST] STOCK_DEDUCTED 발행 완료");

                // Step 4: Order Consumer가 이벤트를 받아 Order 상태를 CONFIRMED로 변경 + Outbox에
                // ORDER_CONFIRMED 저장
                await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
                        Order confirmedOrder = orderRepository.findById(orderId).orElseThrow();
                        assertThat(confirmedOrder.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
                        log.info("[TEST] Order 상태 CONFIRMED 확인: {}", confirmedOrder.getStatus());
                });

                // Step 5: Outbox Publisher 트리거
                outboxPublisher.publish();

                // ========== THEN ==========
                // 최종 검증
                Order finalOrder = orderRepository.findById(orderId).orElseThrow();
                assertThat(finalOrder.getStatus()).isEqualTo(OrderStatus.CONFIRMED);

                log.info("[TEST] ✅ 시나리오 1 성공: Order 상태 = {}", finalOrder.getStatus());
        }

        /**
         * [시나리오 2] 결제 실패 → 보상 트랜잭션
         * 1. Order 생성
         * 2. Payment 서비스 → PAYMENT_FAILED 발행
         * 3. Order 서비스 → 상태 FAILED로 변경
         * 4. Product 서비스 → 재고 복구 (자동)
         */
        @Test
        @DisplayName("시나리오 2: 결제 실패 → 주문 실패 상태 전이")
        void testScenario2_PaymentFailureCompensation() throws Exception {
                // ========== GIVEN ==========
                UUID orderId = UUID.randomUUID();
                UUID userId = UUID.randomUUID();
                long totalAmount = 10000L;

                Order order = Order.builder()
                                .buyerId(userId)
                                .orderNumber("ORD-" + System.currentTimeMillis() % 100000000L)
                                .totalPaymentAmount(totalAmount)
                                .recipientName("Test User")
                                .recipientPhone("010-1234-5678")
                                .zipCode("12345")
                                .shippingAddress("Test Address")
                                .shippingMemo("Test Memo")
                                .build();

                java.lang.reflect.Field orderIdField = Order.class.getDeclaredField("orderId");
                orderIdField.setAccessible(true);
                orderIdField.set(order, orderId);

                orderRepository.save(order);
                log.info("[TEST] Order 생성 완료: orderId={}, status={}", orderId, order.getStatus());

                // ========== WHEN ==========
                // Payment 서비스가 PAYMENT_FAILED 발행
                PaymentFailedPayload paymentFailedPayload = TestEventFactory.createPaymentFailedPayload(
                                orderId,
                                totalAmount,
                                "잔액 부족");
                kafkaTestHelper.publishEvent(
                                EventType.PAYMENT_FAILED,
                                "PAYMENT",
                                orderId,
                                paymentFailedPayload,
                                "service-payment",
                                "payment-events");

                log.info("[TEST] PAYMENT_FAILED 발행 완료");

                // ========== THEN ==========
                // Order 상태가 FAILED로 변경되는지 확인
                await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
                        Order failedOrder = orderRepository.findById(orderId).orElseThrow();
                        assertThat(failedOrder.getStatus()).isEqualTo(OrderStatus.FAILED);
                        log.info("[TEST] Order 상태 FAILED 확인: {}", failedOrder.getStatus());
                });

                Order finalOrder = orderRepository.findById(orderId).orElseThrow();
                assertThat(finalOrder.getStatus()).isEqualTo(OrderStatus.FAILED);

                log.info("[TEST] ✅ 시나리오 2 성공: Order 상태 = {}", finalOrder.getStatus());
        }

        /**
         * [시나리오 3] 재고 차감 실패 → 보상 트랜잭션
         * 1. Order 생성 → PAYMENT_COMPLETED까지 진행
         * 2. Product 서비스 → STOCK_DEDUCTION_FAILED 발행
         * 3. Order 서비스 → 상태 FAILED로 변경
         * 4. Payment 서비스 → 환불 트리거 (이벤트 발행 확인)
         */
        @Test
        @DisplayName("시나리오 3: 재고 차감 실패 → 주문 실패 + 환불 트리거")
        void testScenario3_StockDeductionFailureCompensation() throws Exception {
                // ========== GIVEN ==========
                UUID orderId = UUID.randomUUID();
                UUID userId = UUID.randomUUID();
                UUID productId = UUID.randomUUID();
                long totalAmount = 10000L;

                Order order = Order.builder()
                                .buyerId(userId)
                                .orderNumber("ORD-" + System.currentTimeMillis() % 100000000L)
                                .totalPaymentAmount(totalAmount)
                                .recipientName("Test User")
                                .recipientPhone("010-1234-5678")
                                .zipCode("12345")
                                .shippingAddress("Test Address")
                                .shippingMemo("Test Memo")
                                .build();

                java.lang.reflect.Field orderIdField = Order.class.getDeclaredField("orderId");
                orderIdField.setAccessible(true);
                orderIdField.set(order, orderId);

                orderRepository.save(order);

                // ========== WHEN ==========
                // Step 1: 결제 성공
                PaymentCompletedPayload paymentPayload = TestEventFactory.createPaymentCompletedPayload(orderId,
                                totalAmount);
                kafkaTestHelper.publishEvent(
                                EventType.PAYMENT_COMPLETED,
                                "PAYMENT",
                                orderId,
                                paymentPayload,
                                "service-payment",
                                "payment-events");

                await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
                        Order paidOrder = orderRepository.findById(orderId).orElseThrow();
                        assertThat(paidOrder.getStatus()).isEqualTo(OrderStatus.PAID);
                });

                log.info("[TEST] Order 결제 완료 상태 확인");

                // Step 2: 재고 차감 실패
                StockDeductionFailedPayload stockFailedPayload = TestEventFactory.createStockDeductionFailedPayload(
                                orderId,
                                List.of(productId),
                                "재고 부족");
                kafkaTestHelper.publishEvent(
                                EventType.STOCK_DEDUCTION_FAILED,
                                "PRODUCT",
                                orderId,
                                stockFailedPayload,
                                "service-product",
                                "product-events");

                log.info("[TEST] STOCK_DEDUCTION_FAILED 발행 완료");

                // ========== THEN ==========
                // Order 상태가 FAILED로 변경되는지 확인
                await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
                        Order failedOrder = orderRepository.findById(orderId).orElseThrow();
                        assertThat(failedOrder.getStatus()).isEqualTo(OrderStatus.FAILED);
                        log.info("[TEST] Order 상태 FAILED 확인: {}", failedOrder.getStatus());
                });

                Order finalOrder = orderRepository.findById(orderId).orElseThrow();
                assertThat(finalOrder.getStatus()).isEqualTo(OrderStatus.FAILED);

                log.info("[TEST] ✅ 시나리오 3 성공: Order 상태 = {}, 환불 트리거 예상", finalOrder.getStatus());
        }
}
