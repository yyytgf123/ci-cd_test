package com.groom.common.event.payload;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockDeductionFailedPayload {
	private UUID orderId;
	private String failReason;
	private List<FailedItem> failedItems;

	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class FailedItem {
		private UUID productId;
		private UUID variantId;
		private int requestedQuantity;
		private int availableStock;
		private String reason;
	}
}
