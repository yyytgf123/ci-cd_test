package com.groom.order.presentation.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.groom.order.domain.entity.Order;
import com.groom.order.domain.status.OrderStatus;

public record OrderResponse(
		UUID orderId,
		String orderNo,
		OrderStatus status,
		Long totalAmount,
		LocalDateTime orderedAt,
		List<OrderItemResponse> items // 핵심: 리스트 포함
) {
	public static OrderResponse from(Order order) {
		List<OrderItemResponse> itemResponses = order.getItems().stream()
				.map(OrderItemResponse::from)
				.toList();

		return new OrderResponse(
				order.getOrderId(),
				order.getOrderNumber(),
				order.getStatus(),
				order.getTotalPaymentAmount(),
				order.getCreatedAt(),
				itemResponses);
	}
}
