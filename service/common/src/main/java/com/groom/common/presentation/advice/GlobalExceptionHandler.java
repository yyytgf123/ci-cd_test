package com.groom.common.presentation.advice;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(CustomException.class)
	public ResponseEntity<ErrorResponse> handleCustomException(CustomException exception) {
		log.error("CustomException: {}", exception.getMessage());
		ErrorCode errorCode = exception.getErrorCode();

		ErrorResponse response = ErrorResponse.builder()
			.code(errorCode.getCode())
			.message(exception.getMessage())
			.build();

		return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException exception) {
		List<ErrorResponse.ErrorDetail> details = exception.getBindingResult().getFieldErrors().stream()
			.map(error -> ErrorResponse.ErrorDetail.builder()
				.field(error.getField())
				.reason(error.getDefaultMessage())
				.build())
			.toList();

		return ResponseEntity.badRequest().body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR, details));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleException(Exception exception) {
		log.error("Exception: ", exception);
		return ResponseEntity.internalServerError().body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR));
	}
}
