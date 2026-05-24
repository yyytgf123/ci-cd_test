package com.groom.order.presentation.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.groom.order.application.service.OrderService;
import com.groom.order.presentation.dto.internal.OrderValidationResponse;

import lombok.RequiredArgsConstructor;
//Order Service가 제공하는 API(Payment → Order, 결제 직전 검증)
@RestController
@RequiredArgsConstructor
public class OrderInternalController {

	private final OrderService orderService;

	@GetMapping("/internal/orders/{orderId}")
	public OrderValidationResponse getOrder(@PathVariable UUID orderId) {
		return orderService.getOrderForPayment(orderId);
	}
}


