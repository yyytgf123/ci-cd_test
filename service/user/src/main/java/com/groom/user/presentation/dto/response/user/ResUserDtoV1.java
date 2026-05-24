package com.groom.user.presentation.dto.response.user;

import java.time.LocalDateTime;
import java.util.UUID;

import com.groom.user.domain.entity.address.AddressEntity;
import com.groom.user.domain.entity.owner.OwnerEntity;
import com.groom.user.domain.entity.user.UserEntity;
import com.groom.common.enums.UserRole;
import com.groom.user.domain.entity.user.UserStatus;
import com.groom.user.presentation.dto.response.address.ResAddressDtoV1;
import com.groom.user.presentation.dto.response.owner.ResOwnerApprovalDtoV1;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResUserDtoV1 {

	private UUID id;
	private String email;
	private String nickname;
	private String phoneNumber;
	private UserRole role;
	private UserStatus status;
	// 기본 배송지 추가
	private ResAddressDtoV1 defaultAddress;
	// --------------
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	// Seller
	private ResOwnerApprovalDtoV1 ownerInfo;
	// ------

	public static ResUserDtoV1 from(UserEntity user) {
		return ResUserDtoV1.builder()
			.id(user.getUserId())
			.email(user.getEmail())
			.nickname(user.getNickname())
			.phoneNumber(user.getPhoneNumber())
			.role(user.getRole())
			.status(user.getStatus())
			.createdAt(user.getCreatedAt())
			.updatedAt(user.getUpdatedAt())
			.build();
	}

	public static ResUserDtoV1 from(UserEntity user, AddressEntity defaultAddress) {
		return ResUserDtoV1.builder()
			.id(user.getUserId())
			.email(user.getEmail())
			.nickname(user.getNickname())
			.phoneNumber(user.getPhoneNumber())
			.role(user.getRole())
			.status(user.getStatus())
			.defaultAddress(defaultAddress != null ? ResAddressDtoV1.from(defaultAddress) : null)
			.createdAt(user.getCreatedAt())
			.updatedAt(user.getUpdatedAt())
			.build();
	}

	public static ResUserDtoV1 from(UserEntity user, AddressEntity defaultAddress, OwnerEntity owner) {
		return ResUserDtoV1.builder()
			.id(user.getUserId())
			.email(user.getEmail())
			.nickname(user.getNickname())
			.phoneNumber(user.getPhoneNumber())
			.role(user.getRole())
			.status(user.getStatus())
			.defaultAddress(defaultAddress != null ? ResAddressDtoV1.from(defaultAddress) : null)
			.ownerInfo(owner != null ? ResOwnerApprovalDtoV1.from(owner) : null)
			.build();
	}
}
