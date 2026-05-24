package com.groom.order.infrastructure.client.dto;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StockReserveRequest {
    private UUID orderId;
    private List<StockReserveItem> items;
}