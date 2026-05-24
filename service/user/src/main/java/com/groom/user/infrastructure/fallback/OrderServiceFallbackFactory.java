package com.groom.user.infrastructure.fallback;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import com.groom.user.infrastructure.client.OrderServiceClient;
import com.groom.user.infrastructure.client.SalesDataResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class OrderServiceFallbackFactory implements FallbackFactory<OrderServiceClient> {

	@Override
	public OrderServiceClient create(Throwable cause) {
		log.error("Order Service 호출 실패, Fallback 실행 - 원인: {}", cause.getMessage());

		return new OrderServiceClient() {

			@Override
			public List<SalesDataResponse> getOwnerSales(UUID ownerId, String periodType, LocalDate date) {
				// 매출 조회는 빈 리스트 반환 (조회 실패 안내)
				log.warn("Fallback: getOwnerSales - ownerId: {}, 빈 리스트 반환", ownerId);
				return Collections.emptyList();
			}
		};
	}
}
