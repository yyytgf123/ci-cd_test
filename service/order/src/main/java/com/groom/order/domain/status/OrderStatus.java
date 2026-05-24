package com.groom.order.domain.status;

public enum OrderStatus {
	PENDING, // 결제 대기
	PAID, // 결제 완료 (재고 차감 전)
	CONFIRMED, // 최종 주문 확정 (모든 프로세스 완료)

	// --- 실패/취소 관련 ---
	FAILED, // 결제 실패 등 일반 실패
	CANCELLED, // 사용자 취소
	MANUAL_CHECK // 환불 실패 등 수동 확인 필요
}
//
// public boolean canShip() {
// return this == PAID;
// }

// public boolean canDeliver() {
// return this == SHIPPING;
// }

// public boolean canConfirm() {
// return this == DELIVERED;
// }
