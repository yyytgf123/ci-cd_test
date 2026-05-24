package com.groom.user.infrastructure.adapter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.groom.user.application.port.out.OrderQueryPort;
import com.groom.user.application.port.out.SalesData;
import com.groom.user.domain.entity.user.PeriodType;
import com.groom.user.infrastructure.client.OrderServiceClient;
import com.groom.user.infrastructure.client.SalesDataResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * MSA 환경용 Order 조회 Adapter
 * - OpenFeign을 통해 Order Service로 HTTP 호출
 * - Profile "msa" 활성화 시 사용
 */
@Slf4j
@Component
@Profile("msa")
@RequiredArgsConstructor
public class FeignOrderAdapter implements OrderQueryPort {

	private final OrderServiceClient orderServiceClient;

	@Override
	public List<SalesData> getOwnerSales(UUID ownerId, PeriodType periodType, LocalDate date) {
		log.info("[Feign] Order Service 호출 - getOwnerSales, ownerId: {}, periodType: {}, date: {}",
			ownerId, periodType, date);

		List<SalesDataResponse> responses = orderServiceClient.getOwnerSales(
			ownerId,
			periodType.name(),
			date
		);

		return responses.stream()
			.map(r -> SalesData.of(r.getDate(), r.getTotalAmount(), r.getOrderCount()))
			.toList();
	}
}
