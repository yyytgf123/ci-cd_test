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
public class RefundFailedPayload {
	private UUID orderId;
	private String paymentKey;
	private long cancelAmount;
	private String failCode;
	private String failMessage;
}
