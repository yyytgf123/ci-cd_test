package com.groom.common.presentation.advice;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

	// =====================
	// Common
	// =====================
	VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "요청 값이 올바르지 않습니다."),
	INVALID_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "잘못된 요청입니다."),
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다."),
	ACCESS_DENIED(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "해당 리소스에 대한 접근 권한이 없습니다."),

	// =====================
	// Auth
	// =====================
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증이 필요합니다."),
	FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "접근 권한이 없습니다."),
	INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "유효하지 않은 토큰입니다."),
	EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "EXPIRED_TOKEN", "만료된 토큰입니다."),
	INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "INVALID_PASSWORD", "비밀번호가 일치하지 않습니다."),

	// =====================
	// User
	// =====================
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."),
	EMAIL_DUPLICATED(HttpStatus.CONFLICT, "EMAIL_DUPLICATED", "이미 사용 중인 이메일입니다."),
	NICKNAME_DUPLICATED(HttpStatus.CONFLICT, "NICKNAME_DUPLICATED", "이미 사용 중인 닉네임입니다."),
	ALREADY_WITHDRAWN(HttpStatus.CONFLICT, "ALREADY_WITHDRAWN", "이미 탈퇴한 사용자입니다."),

    BAD_REQUEST(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "잘못된 요청입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "요청한 리소스를 찾을 수 없습니다."),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", "서비스를 일시적으로 사용할 수 없습니다."),

    // =====================
    // Address
    // =====================
    ADDRESS_NOT_FOUND(HttpStatus.NOT_FOUND, "ADDRESS_NOT_FOUND", "배송지를 찾을 수 없습니다."),
    ALREADY_DEFAULT_ADDRESS(HttpStatus.CONFLICT, "ALREADY_DEFAULT_ADDRESS", "이미 기본 배송지로 설정되어 있습니다."),

	// =====================
	// Cart
	// =====================
	CART_NOT_FOUND(HttpStatus.NOT_FOUND, "CART_NOT_FOUND", "장바구니를 찾을 수 없습니다."),
	CART_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "CART_ITEM_NOT_FOUND", "장바구니에서 상품을 찾을 수 없습니다."),
	CART_EMPTY(HttpStatus.BAD_REQUEST, "CART_EMPTY", "장바구니가 비어 있습니다."),

	// =====================
	// Product
	// =====================
	CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND", "카테고리를 찾을 수 없습니다."),
	PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "상품을 찾을 수 없습니다."),
	VARIANT_NOT_FOUND(HttpStatus.NOT_FOUND, "VARIANT_NOT_FOUND", "상품 옵션(SKU)을 찾을 수 없습니다."),
	DUPLICATE_SKU_CODE(HttpStatus.CONFLICT, "DUPLICATE_SKU_CODE", "이미 존재하는 SKU 코드입니다."),
	PRODUCT_NOT_ON_SALE(HttpStatus.BAD_REQUEST, "PRODUCT_NOT_ON_SALE", "판매 중인 상품이 아닙니다."),
	PRODUCT_ALREADY_SUSPENDED(HttpStatus.CONFLICT, "PRODUCT_ALREADY_SUSPENDED", "이미 판매 정지된 상품입니다."),
	PRODUCT_NOT_SUSPENDED(HttpStatus.BAD_REQUEST, "PRODUCT_NOT_SUSPENDED", "판매 정지 상태가 아닙니다."),
	PRODUCT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "PRODUCT_ACCESS_DENIED", "해당 상품에 대한 접근 권한이 없습니다."),
	STOCK_NOT_ENOUGH(HttpStatus.BAD_REQUEST, "STOCK_NOT_ENOUGH", "재고가 부족합니다."),
	PRODUCT_HAS_ORDERS(HttpStatus.CONFLICT, "PRODUCT_HAS_ORDERS", "주문이 존재하는 상품은 삭제할 수 없습니다."),
	CATEGORY_HAS_CHILDREN(HttpStatus.BAD_REQUEST, "CATEGORY_HAS_CHILDREN", "하위 카테고리가 존재하여 삭제할 수 없습니다."),
	CATEGORY_HAS_PRODUCTS(HttpStatus.BAD_REQUEST, "CATEGORY_HAS_PRODUCTS", "등록된 상품이 존재하여 삭제할 수 없습니다."),
	VARIANT_REQUIRED(HttpStatus.BAD_REQUEST, "VARIANT_REQUIRED", "옵션 상품은 variantId가 필요합니다."),
	VARIANT_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "VARIANT_NOT_ALLOWED", "옵션이 없는 상품에는 variantId를 보낼 수 없습니다."),

	// =====================
	// Order / Review
	// =====================
	ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "주문을 찾을 수 없습니다."),
	REVIEW_ALREADY_EXISTS(HttpStatus.CONFLICT, "REVIEW_ALREADY_EXISTS", "이미 리뷰가 존재합니다."),
	REVIEW_NOT_ALLOWED_ORDER_STATUS(
		HttpStatus.CONFLICT,
		"REVIEW_NOT_ALLOWED_ORDER_STATUS",
		"리뷰 가능한 주문 상태가 아닙니다."
	),

	// =====================
	// External / Feign
	// =====================
	ORDER_SERVICE_ERROR(
		HttpStatus.SERVICE_UNAVAILABLE,
		"ORDER_SERVICE_ERROR",
		"주문 서비스 응답 오류"
	),
    PRODUCT_SERVICE_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "PRODUCT_SERVICE_ERROR", "상품 서비스 응답 오류"),

	// =====================
	// Payment - Ready / Checkout
	// =====================
	PAYMENT_NOT_READY(HttpStatus.CONFLICT, "PAYMENT_NOT_READY", "결제창을 열 수 없는 상태입니다."),
	PAYMENT_CONFIG_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "PAYMENT_CONFIG_ERROR", "결제 설정 정보가 올바르지 않습니다."),
	PAYMENT_INVALID_AMOUNT(HttpStatus.BAD_REQUEST, "PAYMENT_INVALID_AMOUNT", "결제 금액이 올바르지 않습니다."),


	// =====================
    // Payment - 기본 상태
    // =====================
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "결제 정보를 찾을 수 없습니다."),
    PAYMENT_ALREADY_CANCELLED(HttpStatus.BAD_REQUEST, "PAYMENT_ALREADY_CANCELLED", "이미 취소된 결제입니다."),
    PAYMENT_ALREADY_FAILED(HttpStatus.BAD_REQUEST, "PAYMENT_ALREADY_FAILED", "이미 실패 처리된 결제입니다."),
    PAYMENT_NOT_CONFIRMABLE(HttpStatus.BAD_REQUEST, "PAYMENT_NOT_CONFIRMABLE", "결제를 승인할 수 없는 상태입니다."),
    PAYMENT_KEY_MISSING(HttpStatus.INTERNAL_SERVER_ERROR, "PAYMENT_KEY_MISSING", "결제 키가 존재하지 않습니다."),

    // =====================
    // Payment - 승인(Confirm)
    // =====================
    PAYMENT_CONFIRM_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "PAYMENT_CONFIRM_ERROR", "결제 승인 처리 중 오류가 발생했습니다."),
    PAYMENT_NOT_DONE(HttpStatus.BAD_REQUEST, "PAYMENT_NOT_DONE", "결제가 정상적으로 승인되지 않았습니다."),
	PAYMENT_GATEWAY_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "PAYMENT_GATEWAY_UNAVAILABLE", "결제 승인 서버(결제대행사)와의 통신이 원활하지 않습니다. 잠시 후 다시 시도해 주세요."),

	// =====================
    // Payment - 취소/환불
    // =====================
    REFUND_PROCESSING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "REFUND_PROCESSING_FAILED", "환불 처리 중 오류가 발생했습니다."),
    REFUND_NOT_CANCELED(HttpStatus.INTERNAL_SERVER_ERROR, "REFUND_NOT_CANCELED", "결제 취소가 정상적으로 처리되지 않았습니다."),

    // =====================
    // Toss (외부 PG 대표 에러)
    // =====================
    TOSS_PROVIDER_ERROR(HttpStatus.BAD_REQUEST, "TOSS_PROVIDER_ERROR", "결제사 처리 중 일시적인 오류가 발생했습니다."),
    TOSS_UNAUTHORIZED_KEY(HttpStatus.UNAUTHORIZED, "TOSS_UNAUTHORIZED_KEY", "결제 인증 정보가 유효하지 않습니다."),
    TOSS_REJECTED(HttpStatus.FORBIDDEN, "TOSS_REJECTED", "결제가 거절되었습니다.");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;
}
