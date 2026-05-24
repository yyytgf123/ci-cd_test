package com.groom.user.infrastructure.client;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.groom.common.infrastructure.feign.config.FeignConfig;
import com.groom.user.infrastructure.fallback.OrderServiceFallbackFactory;

@FeignClient(
	name = "order-service",
	url = "${feign.client.order-service.url:}",
	configuration = FeignConfig.class,
	fallbackFactory = OrderServiceFallbackFactory.class
)
public interface OrderServiceClient {

	/**
	 * Owner 매출 통계 조회
	 */
	@GetMapping("/api/internal/orders/owners/{ownerId}/sales")
	List<SalesDataResponse> getOwnerSales(
		@PathVariable("ownerId") UUID ownerId,
		@RequestParam("periodType") String periodType,
		@RequestParam("date") LocalDate date
	);
}
