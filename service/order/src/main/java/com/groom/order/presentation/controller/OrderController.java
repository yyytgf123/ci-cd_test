package com.groom.order.presentation.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.groom.order.application.service.OrderService;
import com.groom.order.presentation.dto.request.OrderCreateRequest;
import com.groom.order.presentation.dto.response.OrderCreateResponse;
import com.groom.order.presentation.dto.response.OrderResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Order", description = "주문 생성 및 조회 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/orders")
public class OrderController {

	private final OrderService orderService;

	@Operation(summary = "주문 생성", description = "인증된 사용자의 정보로 주문을 생성합니다.")
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "주문 생성 성공",
			content = @Content(schema = @Schema(implementation = UUID.class, example = "7ba12345-1234-1234-1234-123456789abc"))
		)
	})
	@PostMapping
	public ResponseEntity<OrderCreateResponse> createOrder(
		@RequestBody OrderCreateRequest request,
		@AuthenticationPrincipal Jwt jwt
	) {
		String cognitoSub = jwt.getSubject();

		UUID orderId = orderService.createOrder(cognitoSub, request);
		return ResponseEntity.ok(new OrderCreateResponse(orderId));
	}

	@Operation(summary = "내 주문 목록 조회", description = "로그인한 사용자의 주문 내역을 조회합니다.")
	@GetMapping
	public ResponseEntity<Page<OrderResponse>> getMyOrders(
		@AuthenticationPrincipal Jwt jwt,
		@PageableDefault(size = 10, sort = "createdAt") Pageable pageable
	) {
		String cognitoSub = jwt.getSubject();
		return ResponseEntity.ok(orderService.getMyOrders(cognitoSub, pageable));
	}

	@GetMapping("/{orderId}")
	public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID orderId) {
		OrderResponse response = orderService.getOrder(orderId);
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "상품별 주문 목록 조회", description = "특정 상품(ProductId)이 포함된 모든 주문 내역을 조회합니다.")
	@GetMapping("/product/{productId}")
	public ResponseEntity<List<OrderResponse>> getOrdersByProduct(@PathVariable UUID productId) {
		List<OrderResponse> responses = orderService.getOrdersByProduct(productId);
		return ResponseEntity.ok(responses);
	}

	@PostMapping("/{orderId}/cancel")
	public ResponseEntity<String> cancelOrder(@PathVariable UUID orderId) {
		orderService.cancelOrder(orderId);
		return ResponseEntity.ok("주문이 성공적으로 취소되었습니다.");
	}
}
