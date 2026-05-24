package com.groom.order.domain.status;

public enum OrderItemStatus {
	PREPARING,    // 배송 준비 중
	SHIPPING,     // 배송 중
	DELIVERED,    // 배송 완료
	COMPLETED,    // 구매 확정 (리뷰 작성 가능 상태)
	CANCELED      // 아이템 취소
}
// 	// 즉시 취소 가능 여부 (결제 완료 상태까지만 즉시 취소)
// 	public boolean canCancelImmediately() {
// 		return this == PENDING || this == PAID;
// 	}
//
// 	// 취소 자체가 가능한지 여부 (배송 시작 전이면 가능)
// 	// PREPARING 단계는 '취소 요청'은 가능하므로 true로 둠
// 	public boolean isCancelable() {
// 		return this == PENDING || this == PAID || this == PREPARING;
// 	}
// }
