package com.groom.user.application.service;

import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groom.common.presentation.advice.CustomException;
import com.groom.common.presentation.advice.ErrorCode;
import com.groom.user.domain.entity.owner.OwnerEntity;
import com.groom.user.domain.entity.owner.OwnerStatus;
import com.groom.user.domain.entity.user.UserEntity;
import com.groom.common.enums.UserRole;
import com.groom.user.domain.entity.user.UserStatus;
import com.groom.user.domain.repository.OwnerRepository;
import com.groom.user.domain.repository.UserRepository;
import com.groom.user.presentation.dto.request.admin.ReqCreateManagerDtoV1;
import com.groom.user.presentation.dto.response.admin.ResOwnerApprovalListDtoV1;
import com.groom.user.presentation.dto.response.owner.ResOwnerApprovalDtoV1;
import com.groom.user.presentation.dto.response.user.ResUserDtoV1;
import com.groom.user.presentation.dto.response.user.ResUserListDtoV1;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminServiceV1 {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final OwnerRepository ownerRepository;
	private final ApplicationEventPublisher eventPublisher;

	// ==================== Manager 기능 ====================

	/**
	 * 회원 목록 조회 (Manager)
	 */
	public ResUserListDtoV1 getUserList(Pageable pageable) {
		Page<ResUserDtoV1> users = userRepository.findByDeletedAtIsNull(pageable)
			.map(ResUserDtoV1::from);
		return ResUserListDtoV1.from(users);
	}

	/**
	 * 회원 제재 (Manager) - 정책 위반 계정 밴
	 */
	@Transactional
	public void banUser(UUID userId) {
		UserEntity user = findUserById(userId);

		if (user.getRole() == UserRole.MANAGER || user.getRole() == UserRole.MASTER) {
			throw new CustomException(ErrorCode.FORBIDDEN, "관리자 계정은 제재할 수 없습니다.");
		}

		if (user.isBanned()) {
			throw new CustomException(ErrorCode.VALIDATION_ERROR, "이미 제재된 사용자입니다.");
		}

		user.ban();
		log.info("User banned: {}", userId);
	}

	/**
	 * 회원 제재 해제 (Manager)
	 */
	@Transactional
	public void unbanUser(UUID userId) {
		UserEntity user = findUserById(userId);

		if (!user.isBanned()) {
			throw new CustomException(ErrorCode.VALIDATION_ERROR, "제재된 사용자가 아닙니다.");
		}

		user.activate();
		log.info("User unbanned: {}", userId);
	}

	// ==================== Master 기능 ====================

	/**
	 * Manager 계정 생성 (Master only)
	 */
	@Transactional
	public ResUserDtoV1 createManager(ReqCreateManagerDtoV1 request) {
		if (userRepository.existsByEmailAndDeletedAtIsNull(request.getEmail())) {
			throw new CustomException(ErrorCode.EMAIL_DUPLICATED);
		}

		if (userRepository.existsByNicknameAndDeletedAtIsNull(request.getNickname())) {
			throw new CustomException(ErrorCode.NICKNAME_DUPLICATED);
		}

		UserEntity manager = UserEntity.builder()
			.email(request.getEmail())
			.password(passwordEncoder.encode(request.getPassword()))
			.nickname(request.getNickname())
			.phoneNumber(request.getPhoneNumber())
			.role(UserRole.MANAGER)
			.status(UserStatus.ACTIVE)
			.build();

		userRepository.save(manager);
		log.info("Manager created: {}", request.getEmail());

		return ResUserDtoV1.from(manager);
	}

	/**
	 * Manager 계정 삭제 (Master only)
	 */
	@Transactional
	public void deleteManager(UUID managerId) {
		UserEntity manager = findUserById(managerId);

		if (manager.getRole() != UserRole.MANAGER) {
			throw new CustomException(ErrorCode.VALIDATION_ERROR, "Manager 계정만 삭제할 수 있습니다.");
		}

		manager.withdraw();
		log.info("Manager deleted: {}", managerId);
	}

	/**
	 * Manager 목록 조회 (Master only)
	 */
	public ResUserListDtoV1 getManagerList(Pageable pageable) {
		Page<ResUserDtoV1> managers = userRepository.findByRoleAndDeletedAtIsNull(UserRole.MANAGER, pageable)
			.map(ResUserDtoV1::from);
		return ResUserListDtoV1.from(managers);
	}

	private UserEntity findUserById(UUID userId) {
		return userRepository.findByUserIdAndDeletedAtIsNull(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
	}

	/**
	 * 승인 대기 중인 owner 목록 조회 (Manager)
	 */
	public ResOwnerApprovalListDtoV1 getPendingOwnerList(Pageable pageable) {
		Page<ResOwnerApprovalDtoV1> pendingOwners = ownerRepository
			.findByOwnerStatusAndDeletedAtIsNull(OwnerStatus.PENDING, pageable)
			.map(ResOwnerApprovalDtoV1::from);
		return ResOwnerApprovalListDtoV1.from(pendingOwners);
	}

	/**
	 * 특정 상태의 owner 목록 조회 (Manager)
	 */
	public ResOwnerApprovalListDtoV1 getOwnerListByStatus(OwnerStatus status, Pageable pageable) {
		Page<ResOwnerApprovalDtoV1> owners = ownerRepository
			.findByOwnerStatusAndDeletedAtIsNull(status, pageable)
			.map(ResOwnerApprovalDtoV1::from);
		return ResOwnerApprovalListDtoV1.from(owners);
	}

	/**
	 * owner 승인 요청 상세 조회 (Manager)
	 */
	public ResOwnerApprovalDtoV1 getOwnerApprovalDetail(UUID ownerId) {
		OwnerEntity owner = findOwnerById(ownerId);
		return ResOwnerApprovalDtoV1.from(owner);
	}

	/**
	 * owner 승인 (Manager)
	 */
	@Transactional
	public ResOwnerApprovalDtoV1 approveOwner(UUID ownerId) {
		OwnerEntity owner = findOwnerById(ownerId);

		if (!owner.isPending()) {
			throw new CustomException(ErrorCode.VALIDATION_ERROR,
				"승인 대기 상태인 요청만 승인할 수 있습니다. 현재 상태: " + owner.getOwnerStatus());
		}

		owner.approve();
		log.info("Owner approved: OwnerId={}, storeName={}", ownerId, owner.getStoreName());

		return ResOwnerApprovalDtoV1.from(owner);
	}

	/**
	 * Owner 승인 거절 (Manager)
	 */
	@Transactional
	public ResOwnerApprovalDtoV1 rejectOwner(UUID ownerId, String rejectedReason) {
		OwnerEntity owner = findOwnerById(ownerId);

		if (!owner.isPending()) {
			throw new CustomException(ErrorCode.VALIDATION_ERROR,
				"승인 대기 상태인 요청만 거절할 수 있습니다. 현재 상태: " + owner.getOwnerStatus());
		}

		owner.reject(rejectedReason);
		log.info("owner rejected: ownerId={}, storeName={}, reason={}",
			ownerId, owner.getStoreName(), rejectedReason);

		return ResOwnerApprovalDtoV1.from(owner);
	}

	private OwnerEntity findOwnerById(UUID ownerId) {
		return ownerRepository.findByOwnerIdAndDeletedAtIsNull(ownerId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "해당 판매자를 찾을 수 없습니다."));
	}
}
