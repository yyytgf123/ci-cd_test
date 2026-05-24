package com.groom.order.application.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groom.common.event.Type.EventType;
import com.groom.common.event.payload.OrderCancelledPayload;
import com.groom.common.event.payload.OrderCreatedPayload;
import com.groom.order.domain.entity.Order;
import com.groom.order.domain.entity.OrderItem;
import com.groom.order.domain.repository.OrderRepository;
import com.groom.order.infrastructure.client.ProductClient;
import com.groom.order.infrastructure.client.UserClient;
import com.groom.order.infrastructure.client.dto.StockReserveItem;
import com.groom.order.infrastructure.client.dto.StockReserveRequest;
import com.groom.order.infrastructure.client.dto.UserAddressResponse;
import com.groom.order.infrastructure.kafka.OrderOutboxService;
import com.groom.order.presentation.dto.request.OrderCreateRequest;
import com.groom.order.presentation.dto.response.OrderResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

	private final OrderRepository orderRepository;
	private final UserClient userClient;
	private final ProductClient productClient;
	private final OrderOutboxService outboxService;

	/**
	 * 주문을 생성하고, OrderCreatedEvent를 Outbox에 저장합니다.
	 */
	@Transactional
	public UUID createOrder(String cognitoSub, OrderCreateRequest request) {
		// 1. cognitoSub으로 사용자 검증 및 실제 userId 조회
		UUID userId = userClient.getUserByCognitoSub(cognitoSub).getUserId();

		// 2. 주소 정보 조회 (Snapshot용)
		UserAddressResponse address = userClient.getUserAddress(userId, userId);

		// 3. Order 엔티티 생성 (DB 저장 전)
		Order order = Order.builder()
				.buyerId(userId)
				.orderNumber(generateOrderNumber())
				.totalPaymentAmount(request.getTotalAmount())
				.recipientName(address.getRecipientName())
				.recipientPhone(address.getRecipientPhone())
				.zipCode(address.getZipCode())
				.shippingAddress(address.getAddress() + " " + address.getDetailAddress())
				.shippingMemo("부재 시 문 앞에 놓아주세요") // TODO: Request에서 받거나 기본값
				.build();
		UUID orderId = order.getOrderId(); // 미리 ID 생성

		// 4. 재고 가점유 요청 (Bulk)
		List<StockReserveItem> stockItems = request.getItems().stream()
				.map(item -> new StockReserveItem(
						item.getProductId(),
						item.getVariantId(),
						item.getQuantity()))
				.toList();
		productClient.reserveStock(new StockReserveRequest(orderId, stockItems));

		// 5. OrderItem 생성 및 추가
		for (var itemRequest : request.getItems()) {
			OrderItem orderItem = OrderItem.builder()
					.order(order)
					.productId(itemRequest.getProductId())
					.variantId(itemRequest.getVariantId())
					.ownerId(UUID.randomUUID()) // TODO: Product Owner ID 필요 (ProductClient에서 받아오거나 Request에 포함)
					.productTitle(itemRequest.getProductTitle())
					.productThumbnail(itemRequest.getProductThumbnail())
					.optionName(itemRequest.getOptionName())
					.unitPrice(itemRequest.getUnitPrice())
					.quantity(itemRequest.getQuantity())
					.build();
			order.addItem(orderItem);
		}

		// 6. DB 저장
		orderRepository.save(order);

		// 7. 결제 요청 이벤트 Outbox 저장
		outboxService.save(
			EventType.ORDER_CREATED,
			"ORDER",
			orderId,
			orderId.toString(),
			OrderCreatedPayload.builder()
				.orderId(orderId)
				.userId(userId)
				.totalAmount(order.getTotalPaymentAmount())
				.build()
		);

		log.info("주문(ID: {})이 생성되었습니다. 결제 프로세스를 시작합니다.", orderId);
		return orderId;
	}

	private String generateOrderNumber() {
		String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
		int randomPart = ThreadLocalRandom.current().nextInt(100000, 999999);
		return datePart + "-" + randomPart;
	}

	public Page<OrderResponse> getMyOrders(String cognitoSub, Pageable pageable) {
		UUID buyerId = userClient.getUserByCognitoSub(cognitoSub).getUserId();
		return orderRepository.findAllByBuyerId(buyerId, pageable)
				.map(OrderResponse::from);
	}

	public OrderResponse getOrder(UUID orderId) {
		Order order = orderRepository.findById(orderId) // Assuming findById works with UUID as per previous
				.orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다. ID: " + orderId));

		return OrderResponse.from(order);
	}

	public List<OrderResponse> getOrdersByProduct(UUID productId) {
		return orderRepository.findAllByProductId(productId).stream()
				.map(OrderResponse::from)
				.toList();
	}

	@Transactional
	public void cancelOrder(UUID orderId) {
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다. ID: " + orderId));
		order.cancel();
		orderRepository.save(order);

		// 재고 복구를 위한 이벤트 Outbox 저장
		outboxService.save(
			EventType.ORDER_CANCELLED,
			"ORDER",
			orderId,
			orderId.toString(),
			OrderCancelledPayload.builder()
				.orderId(orderId)
				.reason("사용자 요청")
				.cancelledAt(Instant.now())
				.build()
		);

		log.info("주문(ID: {})이 취소되었습니다. 재고 복구 프로세스를 시작합니다.", orderId);
	}

	public com.groom.order.presentation.dto.internal.OrderValidationResponse getOrderForPayment(
			UUID orderId) {
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다. ID: " + orderId));
		return new com.groom.order.presentation.dto.internal.OrderValidationResponse(
				order.getOrderId(),
				order.getTotalPaymentAmount(),
				order.getStatus());
	}
}
