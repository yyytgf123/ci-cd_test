package com.groom.user.application.port.out;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SalesData {

	private LocalDate date;
	private Long totalAmount;
	private Long orderCount;

	public static SalesData of(LocalDate date, Long totalAmount, Long orderCount) {
		return SalesData.builder()
			.date(date)
			.totalAmount(totalAmount)
			.orderCount(orderCount)
			.build();
	}

	public static SalesData empty(LocalDate date) {
		return SalesData.builder()
			.date(date)
			.totalAmount(0L)
			.orderCount(0L)
			.build();
	}
}
