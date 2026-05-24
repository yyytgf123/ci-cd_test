package com.groom.user.presentation.dto.response.owner;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResSalesStatDtoV1 {

	private LocalDate date;
	private Long totalAmount;

	public static ResSalesStatDtoV1 of(LocalDate date, Long totalAmount) {
		return ResSalesStatDtoV1.builder()
			.date(date)
			.totalAmount(totalAmount)
			.build();
	}
}
