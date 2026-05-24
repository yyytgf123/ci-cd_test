package com.groom.order.presentation.dto.internal;

import java.util.UUID;

import com.groom.order.domain.status.OrderStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderValidationResponse {

	private UUID orderId;
	private Long totalPaymentAmount;
	private OrderStatus status;
}
