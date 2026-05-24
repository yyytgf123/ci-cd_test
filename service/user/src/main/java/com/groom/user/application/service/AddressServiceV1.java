package com.groom.user.application.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groom.common.presentation.advice.CustomException;
import com.groom.common.presentation.advice.ErrorCode;
import com.groom.user.domain.entity.address.AddressEntity;
import com.groom.user.domain.entity.user.UserEntity;
import com.groom.user.domain.repository.AddressRepository;
import com.groom.user.domain.repository.UserRepository;
import com.groom.user.presentation.dto.request.address.ReqAddressDtoV1;
import com.groom.user.presentation.dto.response.address.ResAddressDtoV1;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AddressServiceV1 {

	private final AddressRepository addressRepository;
	private final UserRepository userRepository;
	private final UserServiceV1 userService;

	public List<ResAddressDtoV1> getAddressesByCognitoSub(String cognitoSub) {
		UUID userId = userRepository.findByCognitoSub(cognitoSub)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND))
			.getUserId();
		return getAddresses(userId);
	}

	@Transactional
	public void createAddressByCognitoSub(String cognitoSub, ReqAddressDtoV1 request) {
		UUID userId = userRepository.findByCognitoSub(cognitoSub)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND))
			.getUserId();
		createAddress(userId, request);
	}

	@Transactional
	public void updateAddressByCognitoSub(String cognitoSub, UUID addressId, ReqAddressDtoV1 request) {
		UUID userId = userRepository.findByCognitoSub(cognitoSub)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND))
			.getUserId();
		updateAddress(userId, addressId, request);
	}

	@Transactional
	public void deleteAddressByCognitoSub(String cognitoSub, UUID addressId) {
		UUID userId = userRepository.findByCognitoSub(cognitoSub)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND))
			.getUserId();
		deleteAddress(userId, addressId);
	}

	@Transactional
	public void setDefaultAddressByCognitoSub(String cognitoSub, UUID addressId) {
		UUID userId = userRepository.findByCognitoSub(cognitoSub)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND))
			.getUserId();
		setDefaultAddress(userId, addressId);
	}

	public List<ResAddressDtoV1> getAddresses(UUID userId) {
		return addressRepository.findByUserUserId(userId).stream()
			.map(ResAddressDtoV1::from)
			.toList();
	}

	@Transactional
	public void createAddress(UUID userId, ReqAddressDtoV1 request) {
		UserEntity user = userService.findUserById(userId);

		if (Boolean.TRUE.equals(request.getIsDefault())) {
			addressRepository.clearDefaultAddress(userId);
		}

		AddressEntity address = AddressEntity.builder()
			.user(user)
			.recipient(request.getRecipient())
			.recipientPhone(request.getRecipientPhone())
			.zipCode(request.getZipCode())
			.address(request.getAddress())
			.detailAddress(request.getDetailAddress())
			.isDefault(Boolean.TRUE.equals(request.getIsDefault()))
			.build();

		addressRepository.save(address);
		log.info("Address created for user: {}", userId);
	}

	@Transactional
	public void updateAddress(UUID userId, UUID addressId, ReqAddressDtoV1 request) {
		AddressEntity address = findAddressByIdAndUserId(addressId, userId);

		if (Boolean.TRUE.equals(request.getIsDefault())) {
			addressRepository.clearDefaultAddress(userId);
		}

		address.update(
			request.getZipCode(),
			request.getAddress(),
			request.getDetailAddress(),
			request.getRecipient(),
			request.getRecipientPhone(),
			request.getIsDefault()
		);

		log.info("Address updated: {}", addressId);
	}

	@Transactional
	public void deleteAddress(UUID userId, UUID addressId) {
		AddressEntity address = findAddressByIdAndUserId(addressId, userId);
		addressRepository.delete(address);
		log.info("Address deleted: {}", addressId);
	}

	@Transactional
	public void setDefaultAddress(UUID userId, UUID addressId) {
		AddressEntity address = findAddressByIdAndUserId(addressId, userId);

		if (Boolean.TRUE.equals(address.getIsDefault())) {
			throw new CustomException(ErrorCode.ALREADY_DEFAULT_ADDRESS);
		}

		addressRepository.clearDefaultAddress(userId);
		address.setDefault(true);

		log.info("Default address set: {}", addressId);
	}

	public ResAddressDtoV1 getAddress(UUID addressId, UUID userId) {
		AddressEntity address = findAddressByIdAndUserId(addressId, userId);
		return ResAddressDtoV1.from(address);
	}

	private AddressEntity findAddressByIdAndUserId(UUID addressId, UUID userId) {
		return addressRepository.findByAddressIdAndUserUserId(addressId, userId)
			.orElseThrow(() -> new CustomException(ErrorCode.ADDRESS_NOT_FOUND));
	}
}
