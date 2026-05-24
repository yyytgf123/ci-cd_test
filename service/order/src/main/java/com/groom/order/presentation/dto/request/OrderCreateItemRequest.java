package com.groom.order.presentation.dto.request;

import java.util.UUID;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OrderCreateItemRequest {

	private UUID productId;
	private UUID variantId;
	private Integer quantity;

	// Snapshot fields from frontend
	private String productTitle;
	private String productThumbnail;
	private String optionName;
	private Long unitPrice;
}
