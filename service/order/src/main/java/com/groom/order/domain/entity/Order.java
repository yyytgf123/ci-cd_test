package com.groom.order.domain.entity;

import com.groom.common.domain.entity.BaseEntity;
import com.groom.order.domain.status.OrderStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "p_order")
public class Order extends BaseEntity {

	/* ================= 식별자 ================= */

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "order_id", nullable = false, unique = true)
	private UUID orderId;

	@Column(name = "order_number", nullable = false, unique = true, length = 20)
	private String orderNumber;

	@Column(name = "buyer_id", nullable = false)
	private UUID buyerId;

	/* ================= 금액 / 상태 ================= */

	@Column(name = "total_payment_amt", nullable = false)
	private Long totalPaymentAmount;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private OrderStatus status;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	private final List<OrderItem> items = new ArrayList<>();

	/* ================= 배송지 스냅샷 ================= */

	@Column(name = "recipient_name", nullable = false, length = 50)
	private String recipientName;

	@Column(name = "recipient_phone", nullable = false, length = 20)
	private String recipientPhone;

	@Column(name = "zip_code", nullable = false, length = 10)
	private String zipCode;

	@Column(name = "shipping_address", nullable = false, length = 300)
	private String shippingAddress;

	@Column(name = "shipping_memo", length = 200)
	private String shippingMemo;

	/* ================= 생성 ================= */

	@Builder
	public Order(
			UUID buyerId,
			String orderNumber,
			Long totalPaymentAmount,
			String recipientName,
			String recipientPhone,
			String zipCode,
			String shippingAddress,
			String shippingMemo) {
		this.orderId = UUID.randomUUID();
		this.buyerId = buyerId;
		this.orderNumber = orderNumber;
		this.totalPaymentAmount = totalPaymentAmount;
		this.recipientName = recipientName;
		this.recipientPhone = recipientPhone;
		this.zipCode = zipCode;
		this.shippingAddress = shippingAddress;
		this.shippingMemo = shippingMemo;
		this.status = OrderStatus.PENDING;
	}

	/**
	 * 1. 결제 성공 (PENDING -> PAID)
	 */
	public void confirmPayment() {
		if (this.status != OrderStatus.PENDING) {
			throw new IllegalStateException("결제 확인은 PENDING 상태에서만 가능합니다. 현재: " + this.status);
		}
		this.status = OrderStatus.PAID;
	}

	/**
	 * 3. 최종 확정 (PAID -> CONFIRMED)
	 */
	public void complete() {
		if (this.status != OrderStatus.PAID) {
			throw new IllegalStateException("주문 확정은 결제 완료(PAID) 이후에만 가능합니다. 현재: " + this.status);
		}
		this.status = OrderStatus.CONFIRMED;
	}

	/**
	 * 4. 실패 처리 (결제 실패, 재고 실패 등)
	 */
	public void fail() {
		if (this.status == OrderStatus.CONFIRMED || this.status == OrderStatus.CANCELLED) {
			throw new IllegalStateException("이미 완료되거나 취소된 주문은 실패 처리할 수 없습니다.");
		}
		this.status = OrderStatus.FAILED;
	}

	/**
	 * 5. 취소 처리 (환불 성공 시)
	 * - PENDING, PAID, CONFIRMED 상태에서 취소 가능
	 * - FAILED, CANCELLED, MANUAL_CHECK 상태에서는 불가
	 */
	public void cancel() {
		if (this.status == OrderStatus.CANCELLED) {
			// 이미 취소된 경우 - idempotency
			return;
		}
		if (this.status == OrderStatus.FAILED || this.status == OrderStatus.MANUAL_CHECK) {
			throw new IllegalStateException("실패 또는 수동 확인 상태인 주문은 취소할 수 없습니다. 현재: " + this.status);
		}
		this.status = OrderStatus.CANCELLED;
	}

	public void addItem(OrderItem item) {
		this.items.add(item);
		// item.setOrder(this); // If OrderItem has setOrder, but it seems it's set in
		// constructor or builder
	}

	/**
	 * 6. 수동 확인 필요 (환불 실패 등)
	 */
	public void requireManualCheck() {
		if (this.status == OrderStatus.CONFIRMED || this.status == OrderStatus.CANCELLED) {
			throw new IllegalStateException("이미 완료되거나 취소된 주문은 수동 확인 상태로 변경할 수 없습니다.");
		}
		this.status = OrderStatus.MANUAL_CHECK;
	}
}
