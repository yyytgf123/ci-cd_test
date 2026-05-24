package com.groom.order.presentation.dto.request;


import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;

public record OrderCancelRequest(

	@NotEmpty(message = "취소할 상품을 최소 1개 이상 선택해야 합니다.")
	List<UUID> orderItemIds// 여러 개를 한 번에 받을 수 있게 리스트로
) {
}
