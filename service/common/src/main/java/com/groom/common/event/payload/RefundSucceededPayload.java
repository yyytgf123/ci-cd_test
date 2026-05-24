package com.groom.common.event.payload;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundSucceededPayload {
	private UUID orderId;
	private String paymentKey;
	private long cancelAmount;
	private Instant refundedAt;
}
