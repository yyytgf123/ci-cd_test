package com.groom.user.infrastructure.client;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SalesDataResponse {

	private LocalDate date;
	private Long totalAmount;
	private Long orderCount;
}
