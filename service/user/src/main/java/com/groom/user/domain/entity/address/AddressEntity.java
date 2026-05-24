package com.groom.user.domain.entity.address;

import java.util.UUID;

import com.groom.common.domain.entity.BaseEntity;
import com.groom.user.domain.entity.user.UserEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "p_address")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SuperBuilder
public class AddressEntity extends BaseEntity {

	// =========================
	// PK
	// =========================
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "address_id", columnDefinition = "uuid")
	private UUID addressId;

	// =========================
	// Relation
	// =========================
	// User Table Mapping
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private UserEntity user;

	// =========================
	// Recipient Info
	// =========================
	@Column(name = "recipient", length = 50)
	private String recipient;

	@Column(name = "recipient_phone", length = 20)
	private String recipientPhone;

	// =========================
	// Address Info
	// =========================
	@Column(name = "zip_code", length = 10, nullable = false)
	private String zipCode;

	@Column(name = "address", length = 200, nullable = false)
	private String address;

	@Column(name = "detail_address", length = 200, nullable = false)
	private String detailAddress;

	// =========================
	// Default Flag
	// =========================
	@Column(name = "is_default", nullable = false)
	@Builder.Default
	private Boolean isDefault = false;

	// =========================
	// Business Methods
	// =========================
	public void update(String zipCode, String address, String detailAddress, String recipient, String recipientPhone,
		Boolean isDefault) {

		this.zipCode = zipCode;
		this.address = address;
		this.detailAddress = detailAddress;
		this.recipient = recipient;
		this.recipientPhone = recipientPhone;

		if (isDefault != null) {
			this.isDefault = isDefault;
		}
	}

	public void setDefault(boolean isDefault) {
		this.isDefault = isDefault;
	}
}
