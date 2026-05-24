package com.groom.order.infrastructure.client.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StockReserveItem {
    private UUID productId;
    private UUID variantId;
    private int quantity;
}
