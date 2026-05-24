package com.groom.common.infrastructure.feign.config;

import com.groom.common.presentation.advice.CustomException;
import com.groom.common.presentation.advice.ErrorCode;

import feign.Response;
import feign.codec.ErrorDecoder;

public class FeignErrorDecoder implements ErrorDecoder {

	private final ErrorDecoder defaultDecoder = new Default();

	@Override
	public Exception decode(String methodKey, Response response) {
		String mk = methodKey == null ? "" : methodKey;

		return switch (response.status()) {
			case 400 -> new CustomException(ErrorCode.INVALID_REQUEST);
			case 403 -> new CustomException(ErrorCode.FORBIDDEN);
			case 404 -> map404ByClient(mk);
			case 500, 502, 503 -> map5xxByClient(mk);
			default -> defaultDecoder.decode(methodKey, response);
		};
	}

	private Exception map404ByClient(String methodKey) {
		if (methodKey.contains("UserClient")) {
			return new CustomException(ErrorCode.USER_NOT_FOUND);
		}
		if (methodKey.contains("ProductClient")) {
			return new CustomException(ErrorCode.PRODUCT_NOT_FOUND);
		}
		if (methodKey.contains("OrderClient")) {
			return new CustomException(ErrorCode.ORDER_NOT_FOUND);
		}
		return new CustomException(ErrorCode.NOT_FOUND);
	}

	private Exception map5xxByClient(String methodKey) {
		if (methodKey.contains("OrderClient")) {
			return new CustomException(ErrorCode.ORDER_SERVICE_ERROR);
		}
		if (methodKey.contains("ProductClient")) {
			return new CustomException(ErrorCode.PRODUCT_SERVICE_ERROR);
		}
		return new CustomException(ErrorCode.SERVICE_UNAVAILABLE);
	}
}
