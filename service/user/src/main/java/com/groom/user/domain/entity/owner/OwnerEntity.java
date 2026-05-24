package com.groom.user.domain.entity.owner;

import java.time.LocalDateTime;
import java.util.UUID;

import com.groom.common.domain.entity.BaseEntity;
import com.groom.user.domain.entity.user.UserEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "p_owner")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SuperBuilder
public class OwnerEntity extends BaseEntity {

	// =========================
	// PK
	// =========================
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "owner_id", columnDefinition = "uuid")
	private UUID ownerId;

	// =========================
	// Relation
	// =========================
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private UserEntity user;

	// =========================
	// Store / Business Info
	// =========================
	@Column(name = "store_name", length = 200, nullable = false)
	private String storeName;

	@Column(name = "business_no", length = 50)
	private String businessNo;

	@Column(name = "approval_request", length = 500)
	private String approvalRequest;

	// =========================
	// Store Address
	// =========================
	@Column(name = "zip_code", length = 20)
	private String zipCode;

	@Column(name = "address", length = 200)
	private String address;

	@Column(name = "detail_address", length = 200)
	private String detailAddress;

	// =========================
	// Settlement Info
	// =========================
	@Column(name = "bank", length = 50)
	private String bank;

	@Column(name = "account", length = 100)
	private String account;

	// =========================
	// Status
	// =========================
	@Enumerated(EnumType.STRING)
	@Column(name = "owner_status", length = 20, nullable = false)
	private OwnerStatus ownerStatus;

	@Column(name = "approved_at")
	private LocalDateTime approvedAt;

	@Column(name = "rejected_reason", length = 200)
	private String rejectedReason;

	@Column(name = "rejected_at")
	private LocalDateTime rejectedAt;

	// =========================
	// Business Methods
	// =========================
	public void updateInfo(String storeName, String businessNo, String zipCode,
		String address, String detailAddress, String bank, String account) {

		if (storeName != null) {
			this.storeName = storeName;
		}
		if (businessNo != null) {
			this.businessNo = businessNo;
		}
		if (zipCode != null) {
			this.zipCode = zipCode;
		}
		if (address != null) {
			this.address = address;
		}
		if (detailAddress != null) {
			this.detailAddress = detailAddress;
		}
		if (bank != null) {
			this.bank = bank;
		}
		if (account != null) {
			this.account = account;
		}
	}

	// =========================
	// Approval Methods
	// =========================
	public void approve() {
		this.ownerStatus = OwnerStatus.APPROVED;
		this.approvedAt = LocalDateTime.now();
		this.rejectedReason = null;
		this.rejectedAt = null;
	}

	public void reject(String rejectedReason) {
		this.ownerStatus = OwnerStatus.REJECTED;
		this.rejectedReason = rejectedReason;
		this.rejectedAt = LocalDateTime.now();
		this.approvedAt = null;
	}

	public boolean isPending() {
		return this.ownerStatus == OwnerStatus.PENDING;
	}

	public boolean isApproved() {
		return this.ownerStatus == OwnerStatus.APPROVED;
	}

	public boolean isRejected() {
		return this.ownerStatus == OwnerStatus.REJECTED;
	}
}
