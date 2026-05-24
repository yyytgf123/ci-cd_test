package com.groom.user.presentation.dto.response.owner;

import java.time.LocalDateTime;
import java.util.UUID;

import com.groom.user.domain.entity.owner.OwnerEntity;
import com.groom.user.domain.entity.owner.OwnerStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResOwnerApprovalDtoV1 {

	private UUID ownerId;
	private UUID userId;
	private String email;
	private String nickname;
	private String storeName;
	private String businessNo;
	private String approvalRequest;
	private OwnerStatus ownerStatus;
	private String rejectedReason;
	private LocalDateTime createdAt;
	private LocalDateTime approvedAt;
	private LocalDateTime rejectedAt;

	// 가게 주소 정보
	private String zipCode;
	private String address;
	private String detailAddress;

	public static ResOwnerApprovalDtoV1 from(OwnerEntity owner) {
		return ResOwnerApprovalDtoV1.builder()
			.ownerId(owner.getOwnerId())
			.userId(owner.getUser().getUserId())
			.email(owner.getUser().getEmail())
			.nickname(owner.getUser().getNickname())
			.storeName(owner.getStoreName())
			.businessNo(owner.getBusinessNo())
			.approvalRequest(owner.getApprovalRequest())
			.ownerStatus(owner.getOwnerStatus())
			.rejectedReason(owner.getRejectedReason())
			.createdAt(owner.getCreatedAt())
			.approvedAt(owner.getApprovedAt())
			.rejectedAt(owner.getRejectedAt())
			.zipCode(owner.getZipCode())
			.address(owner.getAddress())
			.detailAddress(owner.getDetailAddress())
			.build();
	}
}
