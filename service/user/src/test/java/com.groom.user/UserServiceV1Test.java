package com.groom.user;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.groom.common.enums.UserRole;
import com.groom.common.presentation.advice.CustomException;
import com.groom.common.presentation.advice.ErrorCode;
import com.groom.common.util.SecurityUtil;
import com.groom.user.application.service.UserServiceV1;
import com.groom.user.domain.entity.address.AddressEntity;
import com.groom.user.domain.entity.owner.OwnerEntity;
import com.groom.user.domain.entity.owner.OwnerStatus;
import com.groom.user.domain.entity.user.PeriodType;
import com.groom.user.domain.entity.user.UserEntity;
import com.groom.user.domain.entity.user.UserStatus;
import com.groom.user.domain.event.UserUpdateEvent;
import com.groom.user.domain.event.UserWithdrawnEvent;
import com.groom.user.domain.repository.AddressRepository;
import com.groom.user.domain.repository.OwnerRepository;
import com.groom.user.domain.repository.UserRepository;
import com.groom.user.presentation.dto.request.user.ReqUpdateUserDtoV1;
import com.groom.user.presentation.dto.response.owner.ResSalesStatDtoV1;
import com.groom.user.presentation.dto.response.user.ResUserDtoV1;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceV1 테스트")
class UserServiceV1Test {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private OwnerRepository ownerRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private UserServiceV1 userService;

    private UUID userId;
    private UserEntity userEntity;
    private AddressEntity addressEntity;
    private OwnerEntity ownerEntity;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        userEntity = UserEntity.builder()
                .userId(userId)
                .email("test@example.com")
                .password("encodedPassword")
                .nickname("testUser")
                .phoneNumber("010-1234-5678")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();

        addressEntity = AddressEntity.builder()
                .addressId(UUID.randomUUID())
                .user(userEntity)
                .zipCode("12345")
                .address("서울시 강남구")
                .detailAddress("101동 202호")
                .recipient("홍길동")
                .recipientPhone("010-9876-5432")
                .isDefault(true)
                .build();

        ownerEntity = OwnerEntity.builder()
                .ownerId(UUID.randomUUID())
                .user(userEntity)
                .storeName("테스트 스토어")
                .ownerStatus(OwnerStatus.APPROVED)
                .build();
    }

    @Nested
    @DisplayName("getMe() 테스트")
    class GetMeTest {

        @Test
        @DisplayName("일반 사용자 정보 조회 성공")
        void getMe_Success_NormalUser() {
            try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
                securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
                given(userRepository.findByUserIdAndDeletedAtIsNull(userId))
                        .willReturn(Optional.of(userEntity));
                given(addressRepository.findByUserUserIdAndIsDefaultTrue(userId))
                        .willReturn(Optional.of(addressEntity));

                ResUserDtoV1 result = userService.getMe();

                assertThat(result).isNotNull();
                assertThat(result.getId()).isEqualTo(userId);
                assertThat(result.getEmail()).isEqualTo("test@example.com");
                assertThat(result.getNickname()).isEqualTo("testUser");
                assertThat(result.getDefaultAddress()).isNotNull();
                assertThat(result.getOwnerInfo()).isNull();
            }
        }

        @Test
        @DisplayName("OWNER 사용자 정보 조회 성공 - Owner 정보 포함")
        void getMe_Success_OwnerUser() {
            UserEntity ownerUser = UserEntity.builder()
                    .userId(userId)
                    .email("owner@example.com")
                    .password("encodedPassword")
                    .nickname("ownerUser")
                    .phoneNumber("010-1111-2222")
                    .role(UserRole.OWNER)
                    .status(UserStatus.ACTIVE)
                    .build();

            try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
                securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
                given(userRepository.findByUserIdAndDeletedAtIsNull(userId))
                        .willReturn(Optional.of(ownerUser));
                given(addressRepository.findByUserUserIdAndIsDefaultTrue(userId))
                        .willReturn(Optional.empty());
                given(ownerRepository.findByUserUserIdAndDeletedAtIsNull(userId))
                        .willReturn(Optional.of(ownerEntity));

                ResUserDtoV1 result = userService.getMe();

                assertThat(result).isNotNull();
                assertThat(result.getRole()).isEqualTo(UserRole.OWNER);
                assertThat(result.getOwnerInfo()).isNotNull();
            }
        }

        @Test
        @DisplayName("사용자를 찾을 수 없는 경우 예외 발생")
        void getMe_UserNotFound_ThrowsException() {
            try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
                securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
                given(userRepository.findByUserIdAndDeletedAtIsNull(userId))
                        .willReturn(Optional.empty());

                assertThatThrownBy(() -> userService.getMe())
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
            }
        }

        @Test
        @DisplayName("기본 배송지가 없는 경우에도 정상 조회")
        void getMe_NoDefaultAddress_Success() {
            try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
                securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
                given(userRepository.findByUserIdAndDeletedAtIsNull(userId))
                        .willReturn(Optional.of(userEntity));
                given(addressRepository.findByUserUserIdAndIsDefaultTrue(userId))
                        .willReturn(Optional.empty());

                ResUserDtoV1 result = userService.getMe();

                assertThat(result).isNotNull();
                assertThat(result.getDefaultAddress()).isNull();
            }
        }
    }

    @Nested
    @DisplayName("updateMe() 테스트")
    class UpdateMeTest {

        @Test
        @DisplayName("닉네임만 수정 성공")
        void updateMe_NicknameOnly_Success() {
            ReqUpdateUserDtoV1 request = new ReqUpdateUserDtoV1();
            request.setNickname("newNickname");

            try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
                securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
                given(userRepository.findByUserIdAndDeletedAtIsNull(userId))
                        .willReturn(Optional.of(userEntity));
                given(userRepository.findByNickname("newNickname"))
                        .willReturn(Optional.empty());

                userService.updateMe(request);

                assertThat(userEntity.getNickname()).isEqualTo("newNickname");
                verify(eventPublisher).publishEvent(any(UserUpdateEvent.class));
            }
        }

        @Test
        @DisplayName("전화번호만 수정 성공")
        void updateMe_PhoneNumberOnly_Success() {
            ReqUpdateUserDtoV1 request = new ReqUpdateUserDtoV1();
            request.setPhoneNumber("010-5555-6666");

            try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
                securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
                given(userRepository.findByUserIdAndDeletedAtIsNull(userId))
                        .willReturn(Optional.of(userEntity));

                userService.updateMe(request);

                assertThat(userEntity.getPhoneNumber()).isEqualTo("010-5555-6666");
                verify(eventPublisher).publishEvent(any(UserUpdateEvent.class));
            }
        }

        @Test
        @DisplayName("비밀번호만 수정 성공")
        void updateMe_PasswordOnly_Success() {
            ReqUpdateUserDtoV1 request = new ReqUpdateUserDtoV1();
            request.setPassword("newPassword123");

            try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
                securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
                given(userRepository.findByUserIdAndDeletedAtIsNull(userId))
                        .willReturn(Optional.of(userEntity));
                given(passwordEncoder.encode("newPassword123"))
                        .willReturn("encodedNewPassword");

                userService.updateMe(request);

                assertThat(userEntity.getPassword()).isEqualTo("encodedNewPassword");
                
                ArgumentCaptor<UserUpdateEvent> eventCaptor = ArgumentCaptor.forClass(UserUpdateEvent.class);
                verify(eventPublisher).publishEvent(eventCaptor.capture());
                assertThat(eventCaptor.getValue().isPassword()).isTrue();
            }
        }

        @Test
        @DisplayName("모든 필드 수정 성공")
        void updateMe_AllFields_Success() {
            ReqUpdateUserDtoV1 request = new ReqUpdateUserDtoV1();
            request.setNickname("newNickname");
            request.setPhoneNumber("010-7777-8888");
            request.setPassword("newPassword456");

            try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
                securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
                given(userRepository.findByUserIdAndDeletedAtIsNull(userId))
                        .willReturn(Optional.of(userEntity));
                given(userRepository.findByNickname("newNickname"))
                        .willReturn(Optional.empty());
                given(passwordEncoder.encode("newPassword456"))
                        .willReturn("encodedPassword456");

                userService.updateMe(request);

                assertThat(userEntity.getNickname()).isEqualTo("newNickname");
                assertThat(userEntity.getPhoneNumber()).isEqualTo("010-7777-8888");
                assertThat(userEntity.getPassword()).isEqualTo("encodedPassword456");
                
                ArgumentCaptor<UserUpdateEvent> eventCaptor = ArgumentCaptor.forClass(UserUpdateEvent.class);
                verify(eventPublisher).publishEvent(eventCaptor.capture());
                UserUpdateEvent event = eventCaptor.getValue();
                assertThat(event.getNickname()).isEqualTo("newNickname");
                assertThat(event.getPhoneNumber()).isEqualTo("010-7777-8888");
                assertThat(event.isPassword()).isTrue();
            }
        }

        @Test
        @DisplayName("중복된 닉네임으로 수정 시 예외 발생")
        void updateMe_DuplicateNickname_ThrowsException() {
            ReqUpdateUserDtoV1 request = new ReqUpdateUserDtoV1();
            request.setNickname("existingNickname");

            UserEntity anotherUser = UserEntity.builder()
                    .userId(UUID.randomUUID())
                    .nickname("existingNickname")
                    .build();

            try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
                securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
                given(userRepository.findByUserIdAndDeletedAtIsNull(userId))
                        .willReturn(Optional.of(userEntity));
                given(userRepository.findByNickname("existingNickname"))
                        .willReturn(Optional.of(anotherUser));

                assertThatThrownBy(() -> userService.updateMe(request))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NICKNAME_DUPLICATED);
            }
        }

        @Test
        @DisplayName("자신의 현재 닉네임과 같은 경우 수정 가능")
        void updateMe_SameNickname_Success() {
            ReqUpdateUserDtoV1 request = new ReqUpdateUserDtoV1();
            request.setNickname("testUser");

            try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
                securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
                given(userRepository.findByUserIdAndDeletedAtIsNull(userId))
                        .willReturn(Optional.of(userEntity));
                given(userRepository.findByNickname("testUser"))
                        .willReturn(Optional.of(userEntity));

                userService.updateMe(request);

                assertThat(userEntity.getNickname()).isEqualTo("testUser");
            }
        }

        @Test
        @DisplayName("빈 요청인 경우 이벤트 발행 안함")
        void updateMe_EmptyRequest_NoEvent() {
            ReqUpdateUserDtoV1 request = new ReqUpdateUserDtoV1();

            try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
                securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
                given(userRepository.findByUserIdAndDeletedAtIsNull(userId))
                        .willReturn(Optional.of(userEntity));

                userService.updateMe(request);

                verify(eventPublisher, never()).publishEvent(any(UserUpdateEvent.class));
            }
        }
    }

    @Nested
    @DisplayName("deleteMe() 테스트")
    class DeleteMeTest {

        @Test
        @DisplayName("회원 탈퇴 성공")
        void deleteMe_Success() {
            try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
                securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
                given(userRepository.findByUserIdAndDeletedAtIsNull(userId))
                        .willReturn(Optional.of(userEntity));

                userService.deleteMe();

                assertThat(userEntity.isWithdrawn()).isTrue();
                
                ArgumentCaptor<UserWithdrawnEvent> eventCaptor = ArgumentCaptor.forClass(UserWithdrawnEvent.class);
                verify(eventPublisher).publishEvent(eventCaptor.capture());
                assertThat(eventCaptor.getValue().getUserId()).isEqualTo(userId);
            }
        }

        @Test
        @DisplayName("이미 탈퇴한 사용자가 탈퇴 시도 시 예외 발생")
        void deleteMe_AlreadyWithdrawn_ThrowsException() {
            userEntity.withdraw();

            try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
                securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
                given(userRepository.findByUserIdAndDeletedAtIsNull(userId))
                        .willReturn(Optional.of(userEntity));

                assertThatThrownBy(() -> userService.deleteMe())
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_WITHDRAWN);
            }
        }
    }

    @Nested
    @DisplayName("getSalesStats() 테스트")
    class GetSalesStatsTest {

        @Test
        @DisplayName("OWNER 사용자 매출 통계 조회 성공")
        void getSalesStats_Owner_Success() {
            UserEntity ownerUser = UserEntity.builder()
                    .userId(userId)
                    .email("owner@example.com")
                    .password("encodedPassword")
                    .nickname("ownerUser")
                    .phoneNumber("010-1111-2222")
                    .role(UserRole.OWNER)
                    .status(UserStatus.ACTIVE)
                    .build();

            try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
                securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
                given(userRepository.findByUserIdAndDeletedAtIsNull(userId))
                        .willReturn(Optional.of(ownerUser));

                LocalDate date = LocalDate.of(2025, 1, 15);

                List<ResSalesStatDtoV1> result = userService.getSalesStats(PeriodType.DAILY, date);

                assertThat(result).isNotEmpty();
                assertThat(result.get(0).getDate()).isEqualTo(date);
            }
        }

        @Test
        @DisplayName("일반 USER가 매출 통계 조회 시 예외 발생")
        void getSalesStats_User_ThrowsForbidden() {
            try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
                securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
                given(userRepository.findByUserIdAndDeletedAtIsNull(userId))
                        .willReturn(Optional.of(userEntity));

                assertThatThrownBy(() -> userService.getSalesStats(PeriodType.DAILY, LocalDate.now()))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
            }
        }

        @Test
        @DisplayName("날짜가 null인 경우 현재 날짜 사용")
        void getSalesStats_NullDate_UsesToday() {
            UserEntity ownerUser = UserEntity.builder()
                    .userId(userId)
                    .role(UserRole.OWNER)
                    .status(UserStatus.ACTIVE)
                    .build();

            try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
                securityUtil.when(SecurityUtil::getCurrentUserId).thenReturn(userId);
                given(userRepository.findByUserIdAndDeletedAtIsNull(userId))
                        .willReturn(Optional.of(ownerUser));

                List<ResSalesStatDtoV1> result = userService.getSalesStats(PeriodType.MONTHLY, null);

                assertThat(result).isNotEmpty();
                assertThat(result.get(0).getDate()).isEqualTo(LocalDate.now());
            }
        }
    }

    @Nested
    @DisplayName("findUserById() 테스트")
    class FindUserByIdTest {

        @Test
        @DisplayName("사용자 조회 성공")
        void findUserById_Success() {
            given(userRepository.findByUserIdAndDeletedAtIsNull(userId))
                    .willReturn(Optional.of(userEntity));

            UserEntity result = userService.findUserById(userId);

            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("존재하지 않는 사용자 조회 시 예외 발생")
        void findUserById_NotFound_ThrowsException() {
            given(userRepository.findByUserIdAndDeletedAtIsNull(userId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.findUserById(userId))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
        }
    }
}
