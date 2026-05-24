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
public class StockDeductedPayload {
	private UUID orderId;
	private List<DeductedItem> items;

	@Getter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class DeductedItem {
		private UUID productId;
		private UUID variantId;
		private int quantity;
		private int remainingStock;
	}
}
