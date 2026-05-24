package com.groom.user.domain.entity.user;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.groom.common.domain.entity.BaseEntity;
import com.groom.common.enums.UserRole;
import com.groom.user.domain.entity.address.AddressEntity;

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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "p_user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SuperBuilder
public class UserEntity extends BaseEntity {

	// =========================
	// PK
	// =========================
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "user_id", columnDefinition = "uuid")
	private UUID userId;

	// =========================
	// Basic Info
	// =========================
	@Column(name = "email", length = 100, nullable = false, unique = true)
	private String email;

	@Column(name = "password", length = 255, nullable = false)
	private String password;

	@Column(name = "nickname", length = 200, nullable = false, unique = true)
	private String nickname;

	@Column(name = "phone_number", length = 200, nullable = false)
	private String phoneNumber;

	// =========================
	// Status / Role
	// =========================
	@Enumerated(EnumType.STRING)
	@Column(name = "role", length = 20, nullable = false)
	private UserRole role;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", length = 20, nullable = false)
	private UserStatus status;

	// =========================
	// Relation
	// =========================
	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private List<AddressEntity> addresses = new ArrayList<>();

	// =========================
	// Update Methods
	// =========================
	public void updateNickname(String nickname) {
		this.nickname = requireText(nickname, "nickname");
	}

	public void updatePhoneNumber(String phoneNumber) {
		this.phoneNumber = requireText(phoneNumber, "phoneNumber");
	}

	public void updatePassword(String encodedPassword) {
		this.password = requireText(encodedPassword, "password");
	}

	// =========================
	// Status Change
	// =========================
	public void withdraw(String deletedBy) {
		if (this.status == UserStatus.WITHDRAWN) {
			return; // idempotent
		}
		this.status = UserStatus.WITHDRAWN;
		// BaseEntity의 soft delete 사용
		super.softDelete(deletedBy);
	}

	@Column(name = "cognito_sub")
	private String cognitoSub;

	// 기존 시그니처 유지용
	public void withdraw() {
		withdraw(null);
	}

	public void ban() {
		this.status = UserStatus.BANNED;
	}

	public void activate() {
		if (this.status == UserStatus.WITHDRAWN) {
			throw new IllegalStateException("withdrawn user must use reactivate()");
		}
		this.status = UserStatus.ACTIVE;
	}

	// =========================
	// Status Check
	// =========================
	public boolean isWithdrawn() {
		return this.status == UserStatus.WITHDRAWN;
	}

	public boolean isBanned() {
		return this.status == UserStatus.BANNED;
	}

	// =========================
	// Reactivate
	// =========================
	public void reactivate(String encodedPassword, String nickname, String phoneNumber) {
		if (this.status != UserStatus.WITHDRAWN) {
			throw new IllegalStateException("only withdrawn user can be reactivated");
		}

		this.password = requireText(encodedPassword, "password");
		this.nickname = requireText(nickname, "nickname");
		this.phoneNumber = requireText(phoneNumber, "phoneNumber");
		this.status = UserStatus.ACTIVE;

		// deleted_at/deleted_by 복구가 필요하면 BaseEntity에 restore() 추가 후 여기서 호출하는 방식 추천
	}

	private static String requireText(String value, String field) {
		if (value == null || value.trim().isEmpty()) {
			throw new IllegalArgumentException(field + " must not be blank");
		}
		return value.trim();
	}

	/**
	 * 이메일 인증 완료 시 PENDING → ACTIVE
	 */
	public void confirmEmail() {
		if (this.status == UserStatus.PENDING) {
			this.status = UserStatus.ACTIVE;
		}
	}

	// 기존 메서드들에 추가
	public void updateCognitoSub(String cognitoSub) {
		this.cognitoSub = cognitoSub;
	}
}
