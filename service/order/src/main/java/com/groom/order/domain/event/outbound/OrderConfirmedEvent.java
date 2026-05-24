package com.groom.order.domain.event.outbound;

import java.util.UUID;

/**
 * 주문 확정 시 발행되는 이벤트 (결제 및 재고 차감 완료)
 * 이 이벤트는 장바구니 정리 등 후속 처리를 트리거하는데 사용될 수 있습니다.
 */
public record OrderConfirmedEvent(
    UUID userId,
    UUID orderId
) {}