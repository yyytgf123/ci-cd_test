package com.groom.order.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.groom.order.infrastructure.client.dto.StockReserveRequest;

@FeignClient(name = "product-service", url = "${external.product-service.url}", path = "/api/v1/internal/products")
public interface ProductClient {

	/**
	 * 재고 가점유 (Redis + Lua Script)
	 * 실패 시 예외 발생
	 */
	@PostMapping("/stock/reserve")
	void reserveStock(@RequestBody StockReserveRequest request);
}
