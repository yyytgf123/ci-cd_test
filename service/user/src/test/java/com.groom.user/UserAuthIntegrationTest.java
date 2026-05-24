package com.groom.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.groom.common.enums.UserRole;
import com.groom.common.presentation.advice.CustomException;
import com.groom.common.presentation.advice.ErrorCode;
import com.groom.user.application.service.AuthServiceV1;
import com.groom.user.domain.entity.user.UserEntity;
import com.groom.user.domain.entity.user.UserStatus;
import com.groom.user.domain.repository.UserRepository;
import com.groom.user.presentation.dto.request.user.ReqConfirmSignupDtoV1;
import com.groom.user.presentation.dto.request.user.ReqLoginDtoV1;
import com.groom.user.presentation.dto.request.user.ReqSignupDtoV1;
import com.groom.user.presentation.dto.response.user.ResTokenDtoV1;

import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ConfirmSignUpRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ConfirmSignUpResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpResponse;

/**
 * User 서비스 인증 흐름 Integration Test
 *
 * 검증 범위:
 * - 회원가입 → UserEntity DB 저장 (status: PENDING)
 * - 이메일 인증 → status PENDING → ACTIVE
 * - 로그인 → Cognito accessToken 반환
 * - 중복 이메일 가입 시 예외
 *
 * Cognito는 @MockBean으로 대체, PostgreSQL은 Testcontainers 사용
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@Tag("Integration")
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"user-events", "user-lifecycle"})
class UserAuthIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @MockBean
    CognitoIdentityProviderClient cognitoClient;

    @Autowired
    AuthServiceV1 authService;

    @Autowired
    UserRepository userRepository;

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    // ================================================================
    // 시나리오 1: 회원가입 성공
    // ================================================================

    @Test
    @DisplayName("회원가입 성공 - UserEntity DB 저장, status=PENDING, cognitoSub 저장")
    void signup_Success_SavedWithPendingStatus() {
        // given
        given(cognitoClient.signUp(any(SignUpRequest.class)))
                .willReturn(SignUpResponse.builder()
                        .userSub("fake-cognito-sub-001")
                        .build());

        ReqSignupDtoV1 request = new ReqSignupDtoV1();
        request.setEmail("test@example.com");
        request.setPassword("Password123!");
        request.setNickname("testuser");
        request.setPhoneNumber("010-1234-5678");
        request.setRole(UserRole.USER);

        // when
        authService.signup(request);

        // then
        UserEntity saved = userRepository.findByEmail("test@example.com").orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(UserStatus.PENDING);
        assertThat(saved.getCognitoSub()).isEqualTo("fake-cognito-sub-001");
        assertThat(saved.getNickname()).isEqualTo("testuser");
        assertThat(saved.getRole()).isEqualTo(UserRole.USER);
    }

    // ================================================================
    // 시나리오 2: 이메일 인증 완료 → status ACTIVE
    // ================================================================

    @Test
    @DisplayName("이메일 인증 완료 - status PENDING → ACTIVE")
    void confirmSignup_Success_StatusBecomesActive() {
        // given - 회원가입 먼저
        given(cognitoClient.signUp(any(SignUpRequest.class)))
                .willReturn(SignUpResponse.builder().userSub("fake-sub-002").build());

        ReqSignupDtoV1 signupReq = new ReqSignupDtoV1();
        signupReq.setEmail("confirm@example.com");
        signupReq.setPassword("Password123!");
        signupReq.setNickname("confirmuser");
        signupReq.setPhoneNumber("010-2222-3333");
        signupReq.setRole(UserRole.USER);
        authService.signup(signupReq);

        // 가입 직후 PENDING 확인
        assertThat(userRepository.findByEmail("confirm@example.com").orElseThrow().getStatus())
                .isEqualTo(UserStatus.PENDING);

        given(cognitoClient.confirmSignUp(any(ConfirmSignUpRequest.class)))
                .willReturn(ConfirmSignUpResponse.builder().build());

        // ReqConfirmSignupDtoV1 은 @Setter 없음 → ReflectionTestUtils 사용
        ReqConfirmSignupDtoV1 confirmReq = new ReqConfirmSignupDtoV1();
        ReflectionTestUtils.setField(confirmReq, "email", "confirm@example.com");
        ReflectionTestUtils.setField(confirmReq, "code", "123456");

        // when
        authService.confirmSignup(confirmReq);

        // then
        UserEntity user = userRepository.findByEmail("confirm@example.com").orElseThrow();
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    // ================================================================
    // 시나리오 3: 로그인 성공 → token 반환
    // ================================================================

    @Test
    @DisplayName("로그인 성공 - Cognito accessToken, refreshToken 반환")
    void login_Success_ReturnsToken() {
        // given
        given(cognitoClient.initiateAuth(any(InitiateAuthRequest.class)))
                .willReturn(InitiateAuthResponse.builder()
                        .authenticationResult(AuthenticationResultType.builder()
                                .accessToken("fake-access-token")
                                .refreshToken("fake-refresh-token")
                                .build())
                        .build());

        ReqLoginDtoV1 request = new ReqLoginDtoV1();
        request.setEmail("test@example.com");
        request.setPassword("Password123!");

        // when
        ResTokenDtoV1 result = authService.login(request);

        // then
        assertThat(result.getAccessToken()).isEqualTo("fake-access-token");
        assertThat(result.getRefreshToken()).isEqualTo("fake-refresh-token");
        assertThat(result.getTokenType()).isEqualTo("Bearer");
    }

    // ================================================================
    // 예외: 중복 이메일
    // ================================================================

    @Test
    @DisplayName("중복 이메일 가입 - EMAIL_DUPLICATED 예외")
    void signup_DuplicateEmail_ThrowsException() {
        // given - 첫 번째 가입
        given(cognitoClient.signUp(any(SignUpRequest.class)))
                .willReturn(SignUpResponse.builder().userSub("fake-sub-003").build());

        ReqSignupDtoV1 first = new ReqSignupDtoV1();
        first.setEmail("dup@example.com");
        first.setPassword("Password123!");
        first.setNickname("firstuser");
        first.setPhoneNumber("010-3333-4444");
        first.setRole(UserRole.USER);
        authService.signup(first);

        // when - 동일 이메일로 재가입 시도
        ReqSignupDtoV1 second = new ReqSignupDtoV1();
        second.setEmail("dup@example.com");
        second.setPassword("Password123!");
        second.setNickname("seconduser");
        second.setPhoneNumber("010-5555-6666");
        second.setRole(UserRole.USER);

        // then
        assertThatThrownBy(() -> authService.signup(second))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_DUPLICATED);
    }
}
