package com.groom.user;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.groom.common.enums.UserRole;
import com.groom.common.presentation.advice.CustomException;
import com.groom.common.presentation.advice.ErrorCode;
import com.groom.user.application.service.AdminServiceV1;
import com.groom.user.domain.entity.owner.OwnerEntity;
import com.groom.user.domain.entity.owner.OwnerStatus;
import com.groom.user.domain.entity.user.UserEntity;
import com.groom.user.domain.entity.user.UserStatus;
import com.groom.user.domain.repository.OwnerRepository;
import com.groom.user.domain.repository.UserRepository;
import com.groom.user.presentation.dto.request.admin.ReqCreateManagerDtoV1;
import com.groom.user.presentation.dto.response.admin.ResOwnerApprovalListDtoV1;
import com.groom.user.presentation.dto.response.owner.ResOwnerApprovalDtoV1;
import com.groom.user.presentation.dto.response.user.ResUserDtoV1;
import com.groom.user.presentation.dto.response.user.ResUserListDtoV1;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminServiceV1 테스트")
class AdminServiceV1Test {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private OwnerRepository ownerRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AdminServiceV1 adminService;

    private UUID userId;
    private UUID ownerId;
    private UserEntity userEntity;
    private UserEntity managerEntity;
    private OwnerEntity ownerEntity;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        pageable = PageRequest.of(0, 20);

        userEntity = UserEntity.builder()
                .userId(userId)
                .email("user@example.com")
                .password("encodedPassword")
                .nickname("testUser")
                .phoneNumber("010-1234-5678")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();

        managerEntity = UserEntity.builder()
                .userId(UUID.randomUUID())
                .email("manager@example.com")
                .password("encodedPassword")
                .nickname("managerUser")
                .phoneNumber("010-1111-2222")
                .role(UserRole.MANAGER)
                .status(UserStatus.ACTIVE)
                .build();

        ownerEntity = OwnerEntity.builder()
                .ownerId(ownerId)
                .user(userEntity)
                .storeName("테스트 스토어")
                .ownerStatus(OwnerStatus.PENDING)
                .build();
    }

    @Nested
    @DisplayName("getUserList() 테스트")
    class GetUserListTest {

        @Test
        @DisplayName("회원 목록 조회 성공")
        void getUserList_Success() {
            Page<UserEntity> userPage = new PageImpl<>(List.of(userEntity), pageable, 1);
            given(userRepository.findByDeletedAtIsNull(pageable)).willReturn(userPage);

            ResUserListDtoV1 result = adminService.getUserList(pageable);

            assertThat(result).isNotNull();
            assertThat(result.getUsers()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("빈 회원 목록 조회")
        void getUserList_Empty() {
            Page<UserEntity> emptyPage = new PageImpl<>(List.of(), pageable, 0);
            given(userRepository.findByDeletedAtIsNull(pageable)).willReturn(emptyPage);

            ResUserListDtoV1 result = adminService.getUserList(pageable);

            assertThat(result).isNotNull();
            assertThat(result.getUsers()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("banUser() 테스트")
    class BanUserTest {

        @Test
        @DisplayName("일반 사용자 제재 성공")
        void banUser_Success() {
            given(userRepository.findByUserIdAndDeletedAtIsNull(userId))
                    .willReturn(Optional.of(userEntity));

            adminService.banUser(userId);

            assertThat(userEntity.isBanned()).isTrue();
        }

        @Test
        @DisplayName("MANAGER 계정 제재 시도 시 예외 발생")
        void banUser_Manager_ThrowsForbidden() {
            given(userRepository.findByUserIdAndDeletedAtIsNull(any()))
                    .willReturn(Optional.of(managerEntity));

            assertThatThrownBy(() -> adminService.banUser(managerEntity.getUserId()))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
        }

        @Test
        @DisplayName("MASTER 계정 제재 시도 시 예외 발생")
        void banUser_Master_ThrowsForbidden() {
            UserEntity masterEntity = UserEntity.builder()
                    .userId(UUID.randomUUID())
                    .role(UserRole.MASTER)
                    .status(UserStatus.ACTIVE)
                    .build();

            given(userRepository.findByUserIdAndDeletedAtIsNull(any()))
                    .willReturn(Optional.of(masterEntity));

            assertThatThrownBy(() -> adminService.banUser(masterEntity.getUserId()))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
        }

        @Test
        @DisplayName("이미 제재된 사용자 제재 시도 시 예외 발생")
        void banUser_AlreadyBanned_ThrowsException() {
            userEntity.ban();
            given(userRepository.findByUserIdAndDeletedAtIsNull(userId))
                    .willReturn(Optional.of(userEntity));

            assertThatThrownBy(() -> adminService.banUser(userId))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_ERROR);
        }

        @Test
        @DisplayName("존재하지 않는 사용자 제재 시도 시 예외 발생")
        void banUser_UserNotFound_ThrowsException() {
            given(userRepository.findByUserIdAndDeletedAtIsNull(userId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> adminService.banUser(userId))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("unbanUser() 테스트")
    class UnbanUserTest {

        @Test
        @DisplayName("제재 해제 성공")
        void unbanUser_Success() {
            userEntity.ban();
            given(userRepository.findByUserIdAndDeletedAtIsNull(userId))
                    .willReturn(Optional.of(userEntity));

            adminService.unbanUser(userId);

            assertThat(userEntity.isBanned()).isFalse();
            assertThat(userEntity.getStatus()).isEqualTo(UserStatus.ACTIVE);
        }

        @Test
        @DisplayName("제재되지 않은 사용자 제재 해제 시도 시 예외 발생")
        void unbanUser_NotBanned_ThrowsException() {
            given(userRepository.findByUserIdAndDeletedAtIsNull(userId))
                    .willReturn(Optional.of(userEntity));

            assertThatThrownBy(() -> adminService.unbanUser(userId))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_ERROR);
        }
    }

    @Nested
    @DisplayName("createManager() 테스트")
    class CreateManagerTest {

        @Test
        @DisplayName("Manager 계정 생성 성공")
        void createManager_Success() {
            ReqCreateManagerDtoV1 request = new ReqCreateManagerDtoV1();
            request.setEmail("newmanager@example.com");
            request.setPassword("password123");
            request.setNickname("newManager");
            request.setPhoneNumber("010-3333-4444");

            given(userRepository.existsByEmailAndDeletedAtIsNull("newmanager@example.com"))
                    .willReturn(false);
            given(userRepository.existsByNicknameAndDeletedAtIsNull("newManager"))
                    .willReturn(false);
            given(passwordEncoder.encode("password123"))
                    .willReturn("encodedPassword123");
            given(userRepository.save(any(UserEntity.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            ResUserDtoV1 result = adminService.createManager(request);

            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo("newmanager@example.com");
            assertThat(result.getRole()).isEqualTo(UserRole.MANAGER);

            ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getRole()).isEqualTo(UserRole.MANAGER);
        }

        @Test
        @DisplayName("중복 이메일로 Manager 생성 시 예외 발생")
        void createManager_DuplicateEmail_ThrowsException() {
            ReqCreateManagerDtoV1 request = new ReqCreateManagerDtoV1();
            request.setEmail("existing@example.com");
            request.setPassword("password123");
            request.setNickname("newManager");
            request.setPhoneNumber("010-3333-4444");

            given(userRepository.existsByEmailAndDeletedAtIsNull("existing@example.com"))
                    .willReturn(true);

            assertThatThrownBy(() -> adminService.createManager(request))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_DUPLICATED);
        }

        @Test
        @DisplayName("중복 닉네임으로 Manager 생성 시 예외 발생")
        void createManager_DuplicateNickname_ThrowsException() {
            ReqCreateManagerDtoV1 request = new ReqCreateManagerDtoV1();
            request.setEmail("newmanager@example.com");
            request.setPassword("password123");
            request.setNickname("existingNickname");
            request.setPhoneNumber("010-3333-4444");

            given(userRepository.existsByEmailAndDeletedAtIsNull("newmanager@example.com"))
                    .willReturn(false);
            given(userRepository.existsByNicknameAndDeletedAtIsNull("existingNickname"))
                    .willReturn(true);

            assertThatThrownBy(() -> adminService.createManager(request))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NICKNAME_DUPLICATED);
        }
    }

    @Nested
    @DisplayName("deleteManager() 테스트")
    class DeleteManagerTest {

        @Test
        @DisplayName("Manager 계정 삭제 성공")
        void deleteManager_Success() {
            given(userRepository.findByUserIdAndDeletedAtIsNull(any()))
                    .willReturn(Optional.of(managerEntity));

            adminService.deleteManager(managerEntity.getUserId());

            assertThat(managerEntity.isWithdrawn()).isTrue();
        }

        @Test
        @DisplayName("Manager가 아닌 계정 삭제 시도 시 예외 발생")
        void deleteManager_NotManager_ThrowsException() {
            given(userRepository.findByUserIdAndDeletedAtIsNull(userId))
                    .willReturn(Optional.of(userEntity));

            assertThatThrownBy(() -> adminService.deleteManager(userId))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_ERROR);
        }
    }

    @Nested
    @DisplayName("getManagerList() 테스트")
    class GetManagerListTest {

        @Test
        @DisplayName("Manager 목록 조회 성공")
        void getManagerList_Success() {
            Page<UserEntity> managerPage = new PageImpl<>(List.of(managerEntity), pageable, 1);
            given(userRepository.findByRoleAndDeletedAtIsNull(UserRole.MANAGER, pageable))
                    .willReturn(managerPage);

            ResUserListDtoV1 result = adminService.getManagerList(pageable);

            assertThat(result).isNotNull();
            assertThat(result.getUsers()).hasSize(1);
            assertThat(result.getUsers().get(0).getRole()).isEqualTo(UserRole.MANAGER);
        }
    }

    @Nested
    @DisplayName("getPendingOwnerList() 테스트")
    class GetPendingOwnerListTest {

        @Test
        @DisplayName("승인 대기 중인 Owner 목록 조회 성공")
        void getPendingOwnerList_Success() {
            Page<OwnerEntity> ownerPage = new PageImpl<>(List.of(ownerEntity), pageable, 1);
            given(ownerRepository.findByOwnerStatusAndDeletedAtIsNull(OwnerStatus.PENDING, pageable))
                    .willReturn(ownerPage);

            ResOwnerApprovalListDtoV1 result = adminService.getPendingOwnerList(pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("getOwnerListByStatus() 테스트")
    class GetOwnerListByStatusTest {

        @Test
        @DisplayName("상태별 Owner 목록 조회 성공 - APPROVED")
        void getOwnerListByStatus_Approved_Success() {
            OwnerEntity approvedOwner = OwnerEntity.builder()
                    .ownerId(UUID.randomUUID())
                    .user(userEntity)
                    .storeName("승인된 스토어")
                    .ownerStatus(OwnerStatus.APPROVED)
                    .build();

            Page<OwnerEntity> ownerPage = new PageImpl<>(List.of(approvedOwner), pageable, 1);
            given(ownerRepository.findByOwnerStatusAndDeletedAtIsNull(OwnerStatus.APPROVED, pageable))
                    .willReturn(ownerPage);

            ResOwnerApprovalListDtoV1 result = adminService.getOwnerListByStatus(OwnerStatus.APPROVED, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("상태별 Owner 목록 조회 성공 - REJECTED")
        void getOwnerListByStatus_Rejected_Success() {
            Page<OwnerEntity> ownerPage = new PageImpl<>(List.of(), pageable, 0);
            given(ownerRepository.findByOwnerStatusAndDeletedAtIsNull(OwnerStatus.REJECTED, pageable))
                    .willReturn(ownerPage);

            ResOwnerApprovalListDtoV1 result = adminService.getOwnerListByStatus(OwnerStatus.REJECTED, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getOwnerApprovalDetail() 테스트")
    class GetOwnerApprovalDetailTest {

        @Test
        @DisplayName("Owner 승인 요청 상세 조회 성공")
        void getOwnerApprovalDetail_Success() {
            given(ownerRepository.findByOwnerIdAndDeletedAtIsNull(ownerId))
                    .willReturn(Optional.of(ownerEntity));

            ResOwnerApprovalDtoV1 result = adminService.getOwnerApprovalDetail(ownerId);

            assertThat(result).isNotNull();
            assertThat(result.getOwnerId()).isEqualTo(ownerId);
            assertThat(result.getStoreName()).isEqualTo("테스트 스토어");
        }

        @Test
        @DisplayName("존재하지 않는 Owner 상세 조회 시 예외 발생")
        void getOwnerApprovalDetail_NotFound_ThrowsException() {
            given(ownerRepository.findByOwnerIdAndDeletedAtIsNull(ownerId))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> adminService.getOwnerApprovalDetail(ownerId))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("approveOwner() 테스트")
    class ApproveOwnerTest {

        @Test
        @DisplayName("Owner 승인 성공")
        void approveOwner_Success() {
            given(ownerRepository.findByOwnerIdAndDeletedAtIsNull(ownerId))
                    .willReturn(Optional.of(ownerEntity));

            ResOwnerApprovalDtoV1 result = adminService.approveOwner(ownerId);

            assertThat(result).isNotNull();
            assertThat(ownerEntity.getOwnerStatus()).isEqualTo(OwnerStatus.APPROVED);
            assertThat(ownerEntity.getApprovedAt()).isNotNull();
        }

        @Test
        @DisplayName("PENDING 상태가 아닌 Owner 승인 시도 시 예외 발생")
        void approveOwner_NotPending_ThrowsException() {
            ownerEntity.approve();
            given(ownerRepository.findByOwnerIdAndDeletedAtIsNull(ownerId))
                    .willReturn(Optional.of(ownerEntity));

            assertThatThrownBy(() -> adminService.approveOwner(ownerId))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_ERROR);
        }
    }

    @Nested
    @DisplayName("rejectOwner() 테스트")
    class RejectOwnerTest {

        @Test
        @DisplayName("Owner 거절 성공")
        void rejectOwner_Success() {
            String rejectedReason = "서류 미비";
            given(ownerRepository.findByOwnerIdAndDeletedAtIsNull(ownerId))
                    .willReturn(Optional.of(ownerEntity));

            ResOwnerApprovalDtoV1 result = adminService.rejectOwner(ownerId, rejectedReason);

            assertThat(result).isNotNull();
            assertThat(ownerEntity.getOwnerStatus()).isEqualTo(OwnerStatus.REJECTED);
            assertThat(ownerEntity.getRejectedReason()).isEqualTo(rejectedReason);
            assertThat(ownerEntity.getRejectedAt()).isNotNull();
        }

        @Test
        @DisplayName("PENDING 상태가 아닌 Owner 거절 시도 시 예외 발생")
        void rejectOwner_NotPending_ThrowsException() {
            ownerEntity.reject("이전 거절 사유");
            given(ownerRepository.findByOwnerIdAndDeletedAtIsNull(ownerId))
                    .willReturn(Optional.of(ownerEntity));

            assertThatThrownBy(() -> adminService.rejectOwner(ownerId, "새로운 거절 사유"))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_ERROR);
        }
    }
}
