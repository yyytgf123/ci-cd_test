package com.groom.order.presentation.dto.request;

import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderCreateRequest {

	// 사용자 배송지 ID (주문 시점의 주소 정보를 스냅샷으로 저장)
	private UUID addressId;

	// // 결제 수단 ID
	// private UUID paymentMethodId;

	// 총 결제 금액
	private Long totalAmount;

	// 주문 상품 목록
	private List<OrderCreateItemRequest> items;
}
