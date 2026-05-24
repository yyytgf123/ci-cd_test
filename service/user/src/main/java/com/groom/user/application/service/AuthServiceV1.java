package com.groom.user.application.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.groom.common.presentation.advice.CustomException;
import com.groom.common.presentation.advice.ErrorCode;
import com.groom.user.domain.entity.owner.OwnerEntity;
import com.groom.user.domain.entity.owner.OwnerStatus;
import com.groom.user.domain.entity.user.UserEntity;
import com.groom.common.enums.UserRole;
import com.groom.user.domain.entity.user.UserStatus;
import com.groom.user.domain.event.OwnerSignedUpEvent;
import com.groom.user.domain.event.UserSignedUpEvent;
import com.groom.user.domain.repository.OwnerRepository;
import com.groom.user.domain.repository.UserRepository;
import com.groom.user.presentation.dto.request.user.ReqConfirmSignupDtoV1;
import com.groom.user.presentation.dto.request.user.ReqLoginDtoV1;
import com.groom.user.presentation.dto.request.user.ReqSignupDtoV1;
import com.groom.user.presentation.dto.response.user.ResTokenDtoV1;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceV1 {

	private final UserRepository userRepository;
	private final OwnerRepository ownerRepository;
	private final PasswordEncoder passwordEncoder;
	private final ApplicationEventPublisher eventPublisher;
	private final CognitoIdentityProviderClient cognitoClient;

	@Value("${aws.cognito.client-id}")
	private String clientId;

	@Value("${aws.cognito.client-secret}")
	private String clientSecret;

	@Value("${aws.cognito.user-pool-id}")
	private String userPoolId;

	// ==================== SECRET_HASH 생성 ====================

	private String calculateSecretHash(String username) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			SecretKeySpec secretKeySpec = new SecretKeySpec(
				clientSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"
			);
			mac.init(secretKeySpec);
			mac.update(username.getBytes(StandardCharsets.UTF_8));
			byte[] rawHmac = mac.doFinal(clientId.getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(rawHmac);
		} catch (Exception e) {
			throw new RuntimeException("SECRET_HASH 생성 실패", e);
		}
	}

	// ==================== 회원가입 ====================

	@Transactional
	public String signup(ReqSignupDtoV1 request) {
		if (request.getRole() != UserRole.USER && request.getRole() != UserRole.OWNER) {
			throw new CustomException(ErrorCode.VALIDATION_ERROR, "USER 또는 OWNER만 회원가입할 수 있습니다.");
		}

		Optional<UserEntity> existingUser = userRepository.findByEmail(request.getEmail());
		if (existingUser.isPresent()) {
			UserEntity user = existingUser.get();
			if (user.isWithdrawn()) {
				reactivateUser(user, request);
				return user.getUserId().toString();
			}
			throw new CustomException(ErrorCode.EMAIL_DUPLICATED);
		}

		if (userRepository.existsByNicknameAndDeletedAtIsNull(request.getNickname())) {
			throw new CustomException(ErrorCode.NICKNAME_DUPLICATED);
		}

		String cognitoUserSub = registerToCognito(request);

		if (request.isOwner()) {
			validateOwnerFields(request);
			UserEntity user = createAndSaveUser(request, cognitoUserSub);
			OwnerEntity owner = createAndSaveOwner(user, request);

			eventPublisher.publishEvent(new OwnerSignedUpEvent(
				user.getUserId(), owner.getOwnerId(), user.getEmail(), owner.getStoreName()
			));
			log.info("OWNER signed up: {}", request.getEmail());
		} else {
			UserEntity user = createAndSaveUser(request, cognitoUserSub);
			eventPublisher.publishEvent(new UserSignedUpEvent(
				user.getUserId(), user.getEmail(), user.getRole()
			));
			log.info("USER signed up: {}", request.getEmail());
		}

		return cognitoUserSub;
	}

	private String registerToCognito(ReqSignupDtoV1 request) {
		try {
			SignUpRequest cognitoRequest = SignUpRequest.builder()
				.clientId(clientId)
				.secretHash(calculateSecretHash(request.getEmail()))
				.username(request.getEmail())
				.password(request.getPassword())
				.userAttributes(
					AttributeType.builder().name("email").value(request.getEmail()).build(),
					AttributeType.builder().name("name").value(request.getNickname()).build()
				)
				.build();

			SignUpResponse response = cognitoClient.signUp(cognitoRequest);
			log.info("Cognito 회원가입 성공: {}, userSub: {}", request.getEmail(), response.userSub());

			return response.userSub();

		} catch (UsernameExistsException e) {
			log.error("Cognito 이미 존재하는 사용자: {}", request.getEmail());
			throw new CustomException(ErrorCode.EMAIL_DUPLICATED);
		} catch (InvalidPasswordException e) {
			log.error("Cognito 비밀번호 정책 불일치: {}", e.getMessage());
			throw new CustomException(ErrorCode.VALIDATION_ERROR,
				"비밀번호는 8자 이상, 대문자/소문자/숫자/특수문자를 포함해야 합니다.");
		} catch (InvalidParameterException e) {
			log.error("Cognito 파라미터 오류: {}", e.getMessage());
			throw new CustomException(ErrorCode.VALIDATION_ERROR, e.getMessage());
		}
	}

	// ==================== 이메일 인증 ====================

	@Transactional
	public void confirmSignup(ReqConfirmSignupDtoV1 request) {
		try {
			ConfirmSignUpRequest confirmRequest = ConfirmSignUpRequest.builder()
				.clientId(clientId)
				.secretHash(calculateSecretHash(request.getEmail()))
				.username(request.getEmail())
				.confirmationCode(request.getCode())
				.build();

			cognitoClient.confirmSignUp(confirmRequest);

			userRepository.findByEmail(request.getEmail())
				.ifPresent(user -> {
					user.confirmEmail();
					log.info("사용자 이메일 인증 완료: {}", request.getEmail());
				});

		} catch (CodeMismatchException e) {
			throw new CustomException(ErrorCode.VALIDATION_ERROR, "인증 코드가 일치하지 않습니다.");
		} catch (ExpiredCodeException e) {
			throw new CustomException(ErrorCode.VALIDATION_ERROR, "인증 코드가 만료되었습니다. 재발송을 요청해주세요.");
		} catch (UserNotFoundException e) {
			throw new CustomException(ErrorCode.USER_NOT_FOUND);
		}
	}

	public void resendConfirmationCode(String email) {
		try {
			ResendConfirmationCodeRequest request = ResendConfirmationCodeRequest.builder()
				.clientId(clientId)
				.secretHash(calculateSecretHash(email))
				.username(email)
				.build();

			cognitoClient.resendConfirmationCode(request);
			log.info("인증 코드 재발송: {}", email);

		} catch (UserNotFoundException e) {
			throw new CustomException(ErrorCode.USER_NOT_FOUND);
		} catch (LimitExceededException e) {
			throw new CustomException(ErrorCode.VALIDATION_ERROR, "요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요.");
		}
	}

	// ==================== 로그인 ====================

	public ResTokenDtoV1 login(ReqLoginDtoV1 request) {
		try {
			InitiateAuthRequest authRequest = InitiateAuthRequest.builder()
				.authFlow(AuthFlowType.USER_PASSWORD_AUTH)
				.clientId(clientId)
				.authParameters(Map.of(
					"USERNAME", request.getEmail(),
					"PASSWORD", request.getPassword(),
					"SECRET_HASH", calculateSecretHash(request.getEmail())
				))
				.build();

			InitiateAuthResponse response = cognitoClient.initiateAuth(authRequest);
			AuthenticationResultType result = response.authenticationResult();

			log.info("로그인 성공: {}", request.getEmail());

			return ResTokenDtoV1.of(
				result.accessToken(),
				result.refreshToken()
			);

		} catch (NotAuthorizedException e) {
			log.warn("로그인 실패 - 인증 오류: {}", request.getEmail());
			throw new CustomException(ErrorCode.INVALID_PASSWORD, "이메일 또는 비밀번호가 올바르지 않습니다.");
		} catch (UserNotConfirmedException e) {
			log.warn("로그인 실패 - 이메일 미인증: {}", request.getEmail());
			throw new CustomException(ErrorCode.VALIDATION_ERROR, "이메일 인증이 완료되지 않았습니다.");
		} catch (UserNotFoundException e) {
			throw new CustomException(ErrorCode.USER_NOT_FOUND);
		} catch (PasswordResetRequiredException e) {
			throw new CustomException(ErrorCode.VALIDATION_ERROR, "비밀번호 재설정이 필요합니다.");
		}
	}

	// ==================== 토큰 갱신 ====================

	public ResTokenDtoV1 refreshToken(String refreshToken, String email) {
		try {
			InitiateAuthRequest authRequest = InitiateAuthRequest.builder()
				.authFlow(AuthFlowType.REFRESH_TOKEN_AUTH)
				.clientId(clientId)
				.authParameters(Map.of(
					"REFRESH_TOKEN", refreshToken,
					"SECRET_HASH", calculateSecretHash(email)
				))
				.build();

			InitiateAuthResponse response = cognitoClient.initiateAuth(authRequest);
			AuthenticationResultType result = response.authenticationResult();

			return ResTokenDtoV1.of(
				result.accessToken(),
				result.idToken()
			);

		} catch (NotAuthorizedException e) {
			throw new CustomException(ErrorCode.INVALID_TOKEN, "유효하지 않은 리프레시 토큰입니다.");
		}
	}

	// ==================== 로그아웃 ====================

	public void logout(String accessToken) {
		try {
			GlobalSignOutRequest signOutRequest = GlobalSignOutRequest.builder()
				.accessToken(accessToken)
				.build();

			cognitoClient.globalSignOut(signOutRequest);
			log.info("로그아웃 완료");

		} catch (NotAuthorizedException e) {
			log.warn("로그아웃 실패 - 이미 만료된 토큰");
		}
	}

	// ==================== 사용자 정보 조회 ====================

	public Map<String, String> getUserInfo(String accessToken) {
		try {
			GetUserRequest request = GetUserRequest.builder()
				.accessToken(accessToken)
				.build();

			GetUserResponse response = cognitoClient.getUser(request);

			return response.userAttributes().stream()
				.collect(java.util.stream.Collectors.toMap(
					AttributeType::name,
					AttributeType::value
				));

		} catch (NotAuthorizedException e) {
			throw new CustomException(ErrorCode.INVALID_TOKEN);
		}
	}

	// ==================== Helper Methods ====================

	private UserEntity createAndSaveUser(ReqSignupDtoV1 request, String cognitoUserSub) {
		UserEntity user = UserEntity.builder()
			.email(request.getEmail())
			.password(passwordEncoder.encode(request.getPassword()))
			.nickname(request.getNickname())
			.phoneNumber(request.getPhoneNumber())
			.role(request.getRole())
			.status(UserStatus.PENDING)
			.cognitoSub(cognitoUserSub)
			.build();
		return userRepository.save(user);
	}

	private OwnerEntity createAndSaveOwner(UserEntity user, ReqSignupDtoV1 request) {
		OwnerEntity owner = OwnerEntity.builder()
			.user(user)
			.storeName(request.getStore())
			.zipCode(request.getZipCode())
			.address(request.getAddress())
			.detailAddress(request.getDetailAddress())
			.bank(request.getBank())
			.account(request.getAccount())
			.approvalRequest(request.getApprovalRequest())
			.ownerStatus(OwnerStatus.PENDING)
			.build();
		return ownerRepository.save(owner);
	}

	private void reactivateUser(UserEntity user, ReqSignupDtoV1 request) {
		String cognitoUserSub = registerToCognito(request);
		user.reactivate(
			passwordEncoder.encode(request.getPassword()),
			request.getNickname(),
			request.getPhoneNumber()
		);
		user.updateCognitoSub(cognitoUserSub);
		log.info("User reactivated: {}", request.getEmail());
	}

	private void validateOwnerFields(ReqSignupDtoV1 request) {
		if (!StringUtils.hasText(request.getStore())) {
			throw new CustomException(ErrorCode.VALIDATION_ERROR, "가게 이름은 필수입니다.");
		}
	}
}
