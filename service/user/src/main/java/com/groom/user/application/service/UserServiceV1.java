package com.groom.user.application.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.groom.common.presentation.advice.CustomException;
import com.groom.common.presentation.advice.ErrorCode;
import com.groom.common.util.SecurityUtil;
import com.groom.user.domain.entity.address.AddressEntity;
import com.groom.user.domain.entity.owner.OwnerEntity;
import com.groom.user.domain.entity.user.PeriodType;
import com.groom.user.domain.entity.user.UserEntity;
import com.groom.common.enums.UserRole;
import com.groom.user.domain.event.UserUpdateEvent;
import com.groom.user.domain.event.UserWithdrawnEvent;
import com.groom.user.domain.repository.AddressRepository;
import com.groom.user.domain.repository.OwnerRepository;
import com.groom.user.domain.repository.UserRepository;
import com.groom.user.presentation.dto.request.user.ReqUpdateUserDtoV1;
import com.groom.user.presentation.dto.response.owner.ResSalesStatDtoV1;
import com.groom.user.presentation.dto.response.user.ResUserDtoV1;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceV1 {

	private final UserRepository userRepository;
	private final AddressRepository addressRepository;
	private final PasswordEncoder passwordEncoder;
	private final OwnerRepository ownerRepository;
	private final ApplicationEventPublisher eventPublisher;

	public ResUserDtoV1 getMe() {
		UUID userId = SecurityUtil.getCurrentUserId();

		// -- 기본 배송지 조회 --
		UserEntity user = findUserById(userId);

		AddressEntity defaultAddress = addressRepository
			.findByUserUserIdAndIsDefaultTrue(userId)
			.orElse(null);

		OwnerEntity owner = null;
		if (user.getRole() == UserRole.OWNER) {
			owner = ownerRepository.findByUserUserIdAndDeletedAtIsNull(userId)
				.orElse(null);

		}

		return ResUserDtoV1.from(user, defaultAddress, owner);
	}

	@Transactional
	public void updateMe(ReqUpdateUserDtoV1 request) {
		UUID userId = SecurityUtil.getCurrentUserId();
		UserEntity user = findUserById(userId);

		String newNickname = null;
		String newPhoneNumber = null;
		boolean password = false;

		if (StringUtils.hasText(request.getNickname())) {
			validateNicknameNotTaken(request.getNickname(), userId);
			user.updateNickname(request.getNickname());
			newNickname = request.getNickname();
		}

		if (StringUtils.hasText(request.getPhoneNumber())) {
			user.updatePhoneNumber(request.getPhoneNumber());
			newPhoneNumber = request.getPhoneNumber();
		}

		if (StringUtils.hasText(request.getPassword())) {
			user.updatePassword(passwordEncoder.encode(request.getPassword()));
			password = true;
		}

		if (newNickname != null || newPhoneNumber != null || password) {
			eventPublisher.publishEvent(UserUpdateEvent.builder()
				.userId(userId)
				.nickname(newNickname)
				.phoneNumber(newPhoneNumber)
				.password(password)
				.occurredAt(LocalDateTime.now())
				.build());
		}

		log.info("User updated: {}", userId);
	}

	@Transactional
	public void deleteMe() {
		UUID userId = SecurityUtil.getCurrentUserId();
		UserEntity user = findUserById(userId);

		if (user.isWithdrawn()) {
			throw new CustomException(ErrorCode.ALREADY_WITHDRAWN);
		}

		user.withdraw();

		eventPublisher.publishEvent(new UserWithdrawnEvent(userId));

		log.info("User withdrew: {}", userId);
	}

	public List<ResSalesStatDtoV1> getSalesStats(PeriodType periodType, LocalDate date) {
		UUID userId = SecurityUtil.getCurrentUserId();
		UserEntity user = findUserById(userId);

		if (user.getRole() != UserRole.OWNER) {
			throw new CustomException(ErrorCode.FORBIDDEN);
		}

		log.info("Sales stats requested: userId={}, periodType={}, date={}", userId, periodType, date);

		LocalDate targetDate = date != null ? date : LocalDate.now();
		return List.of(ResSalesStatDtoV1.of(targetDate, 0L));
	}

	public UserEntity findUserById(UUID userId) {
		return userRepository.findByUserIdAndDeletedAtIsNull(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
	}

	private void validateNicknameNotTaken(String nickname, UUID currentUserId) {
		userRepository.findByNickname(nickname)
			.filter(u -> !u.getUserId().equals(currentUserId))
			.ifPresent(u -> {
				throw new CustomException(ErrorCode.NICKNAME_DUPLICATED);
			});
	}
}
