package com.groom.order.application.event.payload;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OrderCreatedPayload {

    private UUID orderId;
    private UUID userId;
    private String status;      // PENDING
    private long totalAmount;
    private String orderNumber;
    private ShippingAddress shippingAddress;
    private OrderSnapshot snapshot;

    @Getter
    @NoArgsConstructor
    public static class OrderSnapshot {
        private List<OrderItemSnapshot> items;
    }

    @Getter
    @NoArgsConstructor
    public static class OrderItemSnapshot {
        private UUID productId;
        private UUID variantId;
        private String productName;
        private long unitPrice;
        private int quantity;
    }
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShippingAddress {
        private String zip;
        private String line1;
        private String line2;
    }
}

