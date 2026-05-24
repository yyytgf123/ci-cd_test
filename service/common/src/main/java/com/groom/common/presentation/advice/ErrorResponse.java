package com.groom.common.presentation.advice;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

	private String code;
	private String message;
	private List<ErrorDetail> details;

	public static ErrorResponse of(ErrorCode errorCode) {
		return ErrorResponse.builder()
			.code(errorCode.getCode())
			.message(errorCode.getMessage())
			.build();
	}

	public static ErrorResponse of(ErrorCode errorCode, List<ErrorDetail> details) {
		return ErrorResponse.builder()
			.code(errorCode.getCode())
			.message(errorCode.getMessage())
			.details(details)
			.build();
	}

	@Getter
	@Builder
	public static class ErrorDetail {
		private String field;
		private String reason;
	}
}
