package com.groom.common.event.payload;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedPayload {
	private UUID orderId;
	private UUID userId;
	private long totalAmount;
}
