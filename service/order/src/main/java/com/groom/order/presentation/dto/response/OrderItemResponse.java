package com.groom.order.presentation.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import com.groom.order.domain.entity.OrderItem;

public record OrderItemResponse(
	UUID productId,
	String productName,
	BigDecimal unitPrice,
	int quantity,
	BigDecimal subtotal
) {
	// Entity -> DTO 변환 메서드 (Static Factory Method)
	public static OrderItemResponse from(OrderItem item) {
		return new OrderItemResponse(
			item.getProductId(),
			item.getProductTitle(),
			BigDecimal.valueOf(item.getUnitPrice()),
			item.getQuantity(),
			BigDecimal.valueOf(item.getSubtotal())
		);
	}
}
