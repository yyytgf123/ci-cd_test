package com.groom.user.application.port.out;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.groom.user.domain.entity.user.PeriodType;

public interface OrderQueryPort {

	/**
	 * Owner 매출 통계 조회
	 */
	List<SalesData> getOwnerSales(UUID ownerId, PeriodType periodType, LocalDate date);
}
