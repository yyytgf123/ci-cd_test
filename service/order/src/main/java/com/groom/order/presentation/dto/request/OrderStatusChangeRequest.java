package com.groom.order.presentation.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;

public record OrderStatusChangeRequest(
	@NotEmpty(message = "상태를 변경할 상품을 최소 1개 이상 선택해야 합니다.")
	List<UUID> orderItemIds
) {
}
