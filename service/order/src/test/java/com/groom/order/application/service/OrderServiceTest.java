package com.groom.order.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.groom.common.event.Type.EventType;
import com.groom.common.event.payload.OrderCancelledPayload;
import com.groom.common.event.payload.OrderCreatedPayload;
import com.groom.order.domain.entity.Order;
import com.groom.order.domain.repository.OrderRepository;
import com.groom.order.domain.status.OrderStatus;
import com.groom.order.infrastructure.client.ProductClient;
import com.groom.order.infrastructure.client.UserClient;
import com.groom.order.infrastructure.client.dto.StockReserveRequest;
import com.groom.order.infrastructure.client.dto.UserAddressResponse;
import com.groom.order.infrastructure.client.dto.UserIdResponse;
import com.groom.order.infrastructure.kafka.OrderOutboxService;
import com.groom.order.presentation.dto.internal.OrderValidationResponse;
import com.groom.order.presentation.dto.request.OrderCreateItemRequest;
import com.groom.order.presentation.dto.request.OrderCreateRequest;
import com.groom.order.presentation.dto.response.OrderResponse;

/**
 * OrderService 단위 테스트
 * 
 * Mock을 사용하여 외부 의존성을 격리하고 서비스 로직만 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService 테스트")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserClient userClient;

    @Mock
    private ProductClient productClient;

    @Mock
    private OrderOutboxService outboxService;

    @InjectMocks
    private OrderService orderService;

    private UUID userId;
    private String cognitoSub;
    private UUID productId;
    private UUID variantId;
    private Long totalAmount;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        cognitoSub = "test-cognito-sub";
        productId = UUID.randomUUID();
        variantId = UUID.randomUUID();
        totalAmount = 50000L;
        lenient().when(userClient.getUserByCognitoSub(cognitoSub)).thenReturn(new UserIdResponse(userId));
    }

    @Nested
    @DisplayName("주문 생성 테스트 (createOrder)")
    class CreateOrderTest {

        @Test
        @DisplayName("정상적인 주문 생성 시 orderId를 반환해야 한다")
        void createOrder_Success_ShouldReturn_OrderId() {
            // given
            OrderCreateRequest request = createOrderRequest();
            UserAddressResponse address = createUserAddress();

            given(userClient.getUserAddress(userId, userId)).willReturn(address);

            // when
            UUID orderId = orderService.createOrder(cognitoSub, request);

            // then
            assertThat(orderId).isNotNull();

            // verify interactions
            then(userClient).should(times(1)).getUserByCognitoSub(cognitoSub);
            then(userClient).should(times(1)).getUserAddress(userId, userId);
            then(productClient).should(times(1)).reserveStock(any(StockReserveRequest.class));
            then(orderRepository).should(times(1)).save(any(Order.class));
            then(outboxService).should(times(1)).save(
                    eq(EventType.ORDER_CREATED),
                    eq("ORDER"),
                    any(UUID.class),
                    any(String.class),
                    any(OrderCreatedPayload.class));
        }

        @Test
        @DisplayName("주문 생성 시 cognitoSub으로 사용자를 조회해야 한다")
        void createOrder_ShouldValidate_User() {
            // given
            OrderCreateRequest request = createOrderRequest();
            UserAddressResponse address = createUserAddress();

            given(userClient.getUserAddress(userId, userId)).willReturn(address);

            // when
            orderService.createOrder(cognitoSub, request);

            // then
            then(userClient).should(times(1)).getUserByCognitoSub(cognitoSub);
        }

        @Test
        @DisplayName("주문 생성 시 사용자 주소를 조회해야 한다")
        void createOrder_ShouldFetch_UserAddress() {
            // given
            OrderCreateRequest request = createOrderRequest();
            UserAddressResponse address = createUserAddress();

            given(userClient.getUserAddress(userId, userId)).willReturn(address);

            // when
            orderService.createOrder(cognitoSub, request);

            // then
            then(userClient).should(times(1)).getUserAddress(userId, userId);
        }

        @Test
        @DisplayName("주문 생성 시 재고 가점유를 요청해야 한다")
        void createOrder_ShouldReserve_Stock() {
            // given
            OrderCreateRequest request = createOrderRequest();
            UserAddressResponse address = createUserAddress();

            given(userClient.getUserAddress(userId, userId)).willReturn(address);

            // when
            orderService.createOrder(cognitoSub, request);

            // then
            then(productClient).should(times(1)).reserveStock(
                    argThat(stockRequest -> {
                        assertThat(stockRequest.getItems()).hasSize(1);
                        assertThat(stockRequest.getItems().get(0).getProductId()).isEqualTo(productId);
                        assertThat(stockRequest.getItems().get(0).getVariantId()).isEqualTo(variantId);
                        assertThat(stockRequest.getItems().get(0).getQuantity()).isEqualTo(2);
                        return true;
                    }));
        }

        @Test
        @DisplayName("주문 생성 시 Order 엔티티를 저장해야 한다")
        void createOrder_ShouldSave_OrderEntity() {
            // given
            OrderCreateRequest request = createOrderRequest();
            UserAddressResponse address = createUserAddress();

            given(userClient.getUserAddress(userId, userId)).willReturn(address);

            // when
            orderService.createOrder(cognitoSub, request);

            // then
            then(orderRepository).should(times(1)).save(
                    argThat(order -> {
                        assertThat(order.getBuyerId()).isEqualTo(userId);
                        assertThat(order.getTotalPaymentAmount()).isEqualTo(totalAmount);
                        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
                        assertThat(order.getRecipientName()).isEqualTo("홍길동");
                        assertThat(order.getRecipientPhone()).isEqualTo("010-1234-5678");
                        assertThat(order.getItems()).hasSize(1);
                        return true;
                    }));
        }

        @Test
        @DisplayName("주문 생성 시 ORDER_CREATED 이벤트를 Outbox에 저장해야 한다")
        void createOrder_ShouldSave_OrderCreatedEvent_ToOutbox() {
            // given
            OrderCreateRequest request = createOrderRequest();
            UserAddressResponse address = createUserAddress();

            given(userClient.getUserAddress(userId, userId)).willReturn(address);

            // when
            orderService.createOrder(cognitoSub, request);

            // then
            then(outboxService).should(times(1)).save(
                    eq(EventType.ORDER_CREATED),
                    eq("ORDER"),
                    any(UUID.class),
                    any(String.class),
                    argThat((OrderCreatedPayload payload) -> {
                        assertThat(payload.getOrderId()).isNotNull();
                        assertThat(payload.getUserId()).isEqualTo(userId);
                        assertThat(payload.getTotalAmount()).isEqualTo(totalAmount);
                        return true;
                    }));
        }

        @Test
        @DisplayName("주문번호는 yyyyMMdd-XXXXXX 형식이어야 한다")
        void createOrder_OrderNumber_ShouldMatch_Format() {
            // given
            OrderCreateRequest request = createOrderRequest();
            UserAddressResponse address = createUserAddress();

            given(userClient.getUserAddress(userId, userId)).willReturn(address);

            // when
            orderService.createOrder(cognitoSub, request);

            // then
            then(orderRepository).should(times(1)).save(
                    argThat(order -> {
                        String orderNumber = order.getOrderNumber();
                        assertThat(orderNumber).matches("\\d{8}-\\d{6}");
                        return true;
                    }));
        }

        @Test
        @DisplayName("사용자 검증 실패 시 예외가 발생해야 한다")
        void createOrder_WhenUserValidationFails_ShouldThrowException() {
            // given
            OrderCreateRequest request = createOrderRequest();

            doThrow(new IllegalArgumentException("유효하지 않은 사용자입니다"))
                    .when(userClient).getUserByCognitoSub(cognitoSub);

            // when & then
            assertThatThrownBy(() -> orderService.createOrder(cognitoSub, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("유효하지 않은 사용자입니다");

            // verify no further interactions
            then(productClient).should(never()).reserveStock(any());
            then(orderRepository).should(never()).save(any());
            then(outboxService).should(never()).save(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("재고 가점유 실패 시 예외가 발생해야 한다")
        void createOrder_WhenStockReserveFails_ShouldThrowException() {
            // given
            OrderCreateRequest request = createOrderRequest();
            UserAddressResponse address = createUserAddress();

            given(userClient.getUserAddress(userId, userId)).willReturn(address);
            doThrow(new IllegalStateException("재고가 부족합니다"))
                    .when(productClient).reserveStock(any(StockReserveRequest.class));

            // when & then
            assertThatThrownBy(() -> orderService.createOrder(cognitoSub, request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("재고가 부족합니다");

            // verify no order saved
            then(orderRepository).should(never()).save(any());
            then(outboxService).should(never()).save(any(), any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("내 주문 목록 조회 테스트 (getMyOrders)")
    class GetMyOrdersTest {

        @Test
        @DisplayName("buyerId로 주문 목록을 조회할 수 있어야 한다")
        void getMyOrders_Success_ShouldReturn_PagedOrders() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            Order order1 = createOrder();
            Order order2 = createOrder();
            Page<Order> orderPage = new PageImpl<>(List.of(order1, order2), pageable, 2);

            given(orderRepository.findAllByBuyerId(userId, pageable)).willReturn(orderPage);

            // when
            Page<OrderResponse> result = orderService.getMyOrders(cognitoSub, pageable);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);

            then(orderRepository).should(times(1)).findAllByBuyerId(userId, pageable);
        }

        @Test
        @DisplayName("주문이 없을 경우 빈 페이지를 반환해야 한다")
        void getMyOrders_WhenNoOrders_ShouldReturn_EmptyPage() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Order> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            given(orderRepository.findAllByBuyerId(userId, pageable)).willReturn(emptyPage);

            // when
            Page<OrderResponse> result = orderService.getMyOrders(cognitoSub, pageable);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("주문 상세 조회 테스트 (getOrder)")
    class GetOrderTest {

        @Test
        @DisplayName("orderId로 주문을 조회할 수 있어야 한다")
        void getOrder_Success_ShouldReturn_OrderResponse() {
            // given
            UUID orderId = UUID.randomUUID();
            Order order = createOrder();

            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            // when
            OrderResponse result = orderService.getOrder(orderId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.orderId()).isEqualTo(order.getOrderId());

            then(orderRepository).should(times(1)).findById(orderId);
        }

        @Test
        @DisplayName("존재하지 않는 주문 조회 시 예외가 발생해야 한다")
        void getOrder_WhenOrderNotFound_ShouldThrowException() {
            // given
            UUID orderId = UUID.randomUUID();

            given(orderRepository.findById(orderId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> orderService.getOrder(orderId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("주문을 찾을 수 없습니다");

            then(orderRepository).should(times(1)).findById(orderId);
        }
    }

    @Nested
    @DisplayName("상품별 주문 목록 조회 테스트 (getOrdersByProduct)")
    class GetOrdersByProductTest {

        @Test
        @DisplayName("productId로 주문 목록을 조회할 수 있어야 한다")
        void getOrdersByProduct_Success_ShouldReturn_OrderList() {
            // given
            Order order1 = createOrder();
            Order order2 = createOrder();

            given(orderRepository.findAllByProductId(productId)).willReturn(List.of(order1, order2));

            // when
            List<OrderResponse> result = orderService.getOrdersByProduct(productId);

            // then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(2);

            then(orderRepository).should(times(1)).findAllByProductId(productId);
        }

        @Test
        @DisplayName("해당 상품이 포함된 주문이 없을 경우 빈 리스트를 반환해야 한다")
        void getOrdersByProduct_WhenNoOrders_ShouldReturn_EmptyList() {
            // given
            given(orderRepository.findAllByProductId(productId)).willReturn(List.of());

            // when
            List<OrderResponse> result = orderService.getOrdersByProduct(productId);

            // then
            assertThat(result).isNotNull();
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("주문 취소 테스트 (cancelOrder)")
    class CancelOrderTest {

        @Test
        @DisplayName("주문 취소 시 Order 상태가 CANCELLED로 변경되어야 한다")
        void cancelOrder_Success_ShouldChange_StatusTo_CANCELLED() {
            // given
            UUID orderId = UUID.randomUUID();
            Order order = createOrder();

            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            // when
            orderService.cancelOrder(orderId);

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);

            then(orderRepository).should(times(1)).findById(orderId);
            then(orderRepository).should(times(1)).save(order);
        }

        @Test
        @DisplayName("주문 취소 시 ORDER_CANCELLED 이벤트를 Outbox에 저장해야 한다")
        void cancelOrder_ShouldSave_OrderCancelledEvent_ToOutbox() {
            // given
            UUID orderId = UUID.randomUUID();
            Order order = createOrder();

            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            // when
            orderService.cancelOrder(orderId);

            // then
            then(outboxService).should(times(1)).save(
                    eq(EventType.ORDER_CANCELLED),
                    eq("ORDER"),
                    eq(orderId),
                    eq(orderId.toString()),
                    argThat((OrderCancelledPayload payload) -> {
                        assertThat(payload.getOrderId()).isEqualTo(orderId);
                        assertThat(payload.getReason()).isEqualTo("사용자 요청");
                        assertThat(payload.getCancelledAt()).isNotNull();
                        return true;
                    }));
        }

        @Test
        @DisplayName("존재하지 않는 주문 취소 시 예외가 발생해야 한다")
        void cancelOrder_WhenOrderNotFound_ShouldThrowException() {
            // given
            UUID orderId = UUID.randomUUID();

            given(orderRepository.findById(orderId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> orderService.cancelOrder(orderId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("주문을 찾을 수 없습니다");

            then(orderRepository).should(times(1)).findById(orderId);
            then(orderRepository).should(never()).save(any());
            then(outboxService).should(never()).save(any(), any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("결제용 주문 조회 테스트 (getOrderForPayment)")
    class GetOrderForPaymentTest {

        @Test
        @DisplayName("결제용 주문 조회 시 OrderValidationResponse를 반환해야 한다")
        void getOrderForPayment_Success_ShouldReturn_OrderValidationResponse() {
            // given
            UUID orderId = UUID.randomUUID();
            Order order = createOrder();

            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            // when
            OrderValidationResponse result = orderService.getOrderForPayment(orderId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getOrderId()).isEqualTo(order.getOrderId());
            assertThat(result.getTotalPaymentAmount()).isEqualTo(order.getTotalPaymentAmount());
            assertThat(result.getStatus()).isEqualTo(order.getStatus());

            then(orderRepository).should(times(1)).findById(orderId);
        }

        @Test
        @DisplayName("존재하지 않는 주문 조회 시 예외가 발생해야 한다")
        void getOrderForPayment_WhenOrderNotFound_ShouldThrowException() {
            // given
            UUID orderId = UUID.randomUUID();

            given(orderRepository.findById(orderId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> orderService.getOrderForPayment(orderId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("주문을 찾을 수 없습니다");
        }
    }

    // ========== Helper Methods ==========

    private OrderCreateRequest createOrderRequest() {
        OrderCreateRequest request = new OrderCreateRequest();
        request.setTotalAmount(totalAmount);

        OrderCreateItemRequest item = new OrderCreateItemRequest();
        item.setProductId(productId);
        item.setVariantId(variantId);
        item.setQuantity(2);
        item.setProductTitle("테스트 상품");
        item.setProductThumbnail("https://example.com/thumbnail.jpg");
        item.setOptionName("기본옵션");
        item.setUnitPrice(25000L);

        request.setItems(List.of(item));
        return request;
    }

    private UserAddressResponse createUserAddress() {
        return UserAddressResponse.builder()
                .recipientName("홍길동")
                .recipientPhone("010-1234-5678")
                .zipCode("12345")
                .address("서울시 강남구 테헤란로 123")
                .detailAddress("4층 401호")
                .build();
    }

    private Order createOrder() {
        return Order.builder()
                .buyerId(userId)
                .orderNumber("20260204-123456")
                .totalPaymentAmount(totalAmount)
                .recipientName("홍길동")
                .recipientPhone("010-1234-5678")
                .zipCode("12345")
                .shippingAddress("서울시 강남구 테헤란로 123 4층 401호")
                .shippingMemo("부재 시 문 앞에 놓아주세요")
                .build();
    }
}
