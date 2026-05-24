package com.groom.order.integration.helper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.groom.common.event.payload.OrderCreatedPayload;
import com.groom.common.event.payload.PaymentCompletedPayload;
import com.groom.common.event.payload.PaymentFailedPayload;
import com.groom.common.event.payload.StockDeductedPayload;
import com.groom.common.event.payload.StockDeductionFailedPayload;

/**
 * 테스트용 이벤트 페이로드 생성 팩토리
 */
public class TestEventFactory {

    public static OrderCreatedPayload createOrderCreatedPayload(UUID orderId, UUID userId, long totalAmount) {
        return OrderCreatedPayload.builder()
                .orderId(orderId)
                .userId(userId)
                .totalAmount(totalAmount)
                .build();
    }

    public static PaymentCompletedPayload createPaymentCompletedPayload(UUID orderId, long amount) {
        return PaymentCompletedPayload.builder()
                .orderId(orderId)
                .paymentKey("test-payment-key-" + UUID.randomUUID())
                .amount(amount)
                .paidAt(Instant.now())
                .build();
    }

    public static PaymentFailedPayload createPaymentFailedPayload(UUID orderId, long amount, String failReason) {
        return PaymentFailedPayload.builder()
                .orderId(orderId)
                .paymentKey("test-payment-key-" + UUID.randomUUID())
                .amount(amount)
                .failCode("PAYMENT_FAILED")
                .failMessage(failReason)
                .build();
    }

    public static StockDeductedPayload createStockDeductedPayload(UUID orderId, List<UUID> productIds) {
        List<StockDeductedPayload.DeductedItem> items = productIds.stream()
                .map(productId -> StockDeductedPayload.DeductedItem.builder()
                        .productId(productId)
                        .variantId(UUID.randomUUID())
                        .quantity(1)
                        .remainingStock(99)
                        .build())
                .toList();

        return StockDeductedPayload.builder()
                .orderId(orderId)
                .items(items)
                .build();
    }

    public static StockDeductionFailedPayload createStockDeductionFailedPayload(UUID orderId, List<UUID> productIds,
            String failReason) {
        List<StockDeductionFailedPayload.FailedItem> failedItems = productIds.stream()
                .map(productId -> StockDeductionFailedPayload.FailedItem.builder()
                        .productId(productId)
                        .variantId(UUID.randomUUID())
                        .requestedQuantity(1)
                        .availableStock(0)
                        .reason(failReason)
                        .build())
                .toList();

        return StockDeductionFailedPayload.builder()
                .orderId(orderId)
                .failReason(failReason)
                .failedItems(failedItems)
                .build();
    }
}
