package com.groom.common.event.Type;

public enum EventType {

    // Order
    ORDER_CREATED,
    ORDER_STATUS_CHANGED,
    ORDER_CANCELLED,
    ORDER_CONFIRMED,

    // Payment
    PAYMENT_COMPLETED,
    PAYMENT_FAILED,
    REFUND_SUCCEEDED,
    REFUND_FAILED,

    // Product
    STOCK_DEDUCTED,
    STOCK_DEDUCTION_FAILED,

    // Cart
    CART_CLEARED,

    // User
    USER_WITHDRAWN,
    USER_UPDATED
}
