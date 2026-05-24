package com.groom.order.domain.entity;

import java.util.UUID;

import com.groom.common.domain.entity.BaseEntity;
import com.groom.order.domain.status.OrderStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "p_order_item")
public class OrderItem extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "order_item_id")
	private UUID orderItemId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "order_id", nullable = false)
	private Order order;

	@Column(name = "product_id", nullable = false)
	private UUID productId;

	@Column(name = "variant_id")
	private UUID variantId;

	@Column(name = "owner_id", nullable = false)
	private UUID ownerId;

	// --- 스냅샷 ---
	@Column(name = "product_title", nullable = false, length = 200)
	private String productTitle;

	@Column(name = "product_thumbnail", length = 500)
	private String productThumbnail;

	@Column(name = "option_name", length = 200)
	private String optionName;

	@Column(name = "unit_price", nullable = false)
	private Long unitPrice;

	@Column(name = "quantity", nullable = false)
	private Integer quantity;

	@Column(name = "subtotal", nullable = false)
	private Long subtotal;

	@Enumerated(EnumType.STRING)
	@Column(name = "item_status", nullable = false, length = 20)
	private OrderStatus itemStatus;

	@Builder
	public OrderItem(Order order, UUID productId, UUID variantId, UUID ownerId,
			String productTitle, String productThumbnail, String optionName,
			Long unitPrice, Integer quantity) {
		this.order = order;
		this.productId = productId;
		this.variantId = variantId;
		this.ownerId = ownerId;
		this.productTitle = productTitle;
		this.productThumbnail = productThumbnail;
		this.optionName = optionName;
		this.unitPrice = unitPrice;
		this.quantity = quantity;
		this.subtotal = unitPrice * quantity;
		this.itemStatus = OrderStatus.PENDING;
	}

	public void cancel() {
		this.itemStatus = OrderStatus.CANCELLED;
	}
}
