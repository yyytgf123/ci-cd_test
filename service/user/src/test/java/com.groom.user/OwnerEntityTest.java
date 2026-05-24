package com.groom.user;

import static org.assertj.core.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.groom.common.enums.UserRole;
import com.groom.user.domain.entity.owner.OwnerEntity;
import com.groom.user.domain.entity.owner.OwnerStatus;
import com.groom.user.domain.entity.user.UserEntity;
import com.groom.user.domain.entity.user.UserStatus;

@DisplayName("OwnerEntity 테스트")
class OwnerEntityTest {

    private OwnerEntity ownerEntity;
    private UserEntity userEntity;
    private UUID ownerId;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        
        userEntity = UserEntity.builder()
                .userId(UUID.randomUUID())
                .email("owner@example.com")
                .password("encodedPassword")
                .nickname("ownerUser")
                .phoneNumber("010-1234-5678")
                .role(UserRole.OWNER)
                .status(UserStatus.ACTIVE)
                .build();

        ownerEntity = OwnerEntity.builder()
                .ownerId(ownerId)
                .user(userEntity)
                .storeName("테스트 스토어")
                .businessNo("123-45-67890")
                .approvalRequest("승인 요청합니다")
                .zipCode("12345")
                .address("서울시 강남구")
                .detailAddress("101동 1층")
                .bank("신한은행")
                .account("110-123-456789")
                .ownerStatus(OwnerStatus.PENDING)
                .build();
    }

    @Nested
    @DisplayName("updateInfo() 테스트")
    class UpdateInfoTest {

        @Test
        @DisplayName("모든 필드 업데이트 성공")
        void updateInfo_AllFields_Success() {
            ownerEntity.updateInfo(
                    "새로운 스토어",
                    "999-88-77777",
                    "54321",
                    "부산시 해운대구",
                    "201동 2층",
                    "국민은행",
                    "999-888-777666"
            );

            assertThat(ownerEntity.getStoreName()).isEqualTo("새로운 스토어");
            assertThat(ownerEntity.getBusinessNo()).isEqualTo("999-88-77777");
            assertThat(ownerEntity.getZipCode()).isEqualTo("54321");
            assertThat(ownerEntity.getAddress()).isEqualTo("부산시 해운대구");
            assertThat(ownerEntity.getDetailAddress()).isEqualTo("201동 2층");
            assertThat(ownerEntity.getBank()).isEqualTo("국민은행");
            assertThat(ownerEntity.getAccount()).isEqualTo("999-888-777666");
        }

        @Test
        @DisplayName("일부 필드만 업데이트 - null은 무시")
        void updateInfo_PartialUpdate_Success() {
            String originalBusinessNo = ownerEntity.getBusinessNo();
            String originalAddress = ownerEntity.getAddress();

            ownerEntity.updateInfo(
                    "수정된 스토어",
                    null,  // businessNo는 변경하지 않음
                    "11111",
                    null,  // address는 변경하지 않음
                    "301동 3층",
                    null,  // bank는 변경하지 않음
                    "000-111-222333"
            );

            assertThat(ownerEntity.getStoreName()).isEqualTo("수정된 스토어");
            assertThat(ownerEntity.getBusinessNo()).isEqualTo(originalBusinessNo);
            assertThat(ownerEntity.getZipCode()).isEqualTo("11111");
            assertThat(ownerEntity.getAddress()).isEqualTo(originalAddress);
            assertThat(ownerEntity.getDetailAddress()).isEqualTo("301동 3층");
            assertThat(ownerEntity.getAccount()).isEqualTo("000-111-222333");
        }

        @Test
        @DisplayName("모든 필드가 null인 경우 기존 값 유지")
        void updateInfo_AllNull_KeepOriginal() {
            String originalStoreName = ownerEntity.getStoreName();
            String originalBusinessNo = ownerEntity.getBusinessNo();
            String originalZipCode = ownerEntity.getZipCode();

            ownerEntity.updateInfo(null, null, null, null, null, null, null);

            assertThat(ownerEntity.getStoreName()).isEqualTo(originalStoreName);
            assertThat(ownerEntity.getBusinessNo()).isEqualTo(originalBusinessNo);
            assertThat(ownerEntity.getZipCode()).isEqualTo(originalZipCode);
        }
    }

    @Nested
    @DisplayName("approve() 테스트")
    class ApproveTest {

        @Test
        @DisplayName("승인 성공")
        void approve_Success() {
            ownerEntity.approve();

            assertThat(ownerEntity.getOwnerStatus()).isEqualTo(OwnerStatus.APPROVED);
            assertThat(ownerEntity.getApprovedAt()).isNotNull();
            assertThat(ownerEntity.getRejectedReason()).isNull();
            assertThat(ownerEntity.getRejectedAt()).isNull();
        }

        @Test
        @DisplayName("거절 후 승인 시 거절 정보 초기화")
        void approve_AfterReject_ClearRejectInfo() {
            ownerEntity.reject("서류 미비");
            ownerEntity.approve();

            assertThat(ownerEntity.getOwnerStatus()).isEqualTo(OwnerStatus.APPROVED);
            assertThat(ownerEntity.getApprovedAt()).isNotNull();
            assertThat(ownerEntity.getRejectedReason()).isNull();
            assertThat(ownerEntity.getRejectedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("reject() 테스트")
    class RejectTest {

        @Test
        @DisplayName("거절 성공")
        void reject_Success() {
            String rejectedReason = "사업자등록증 불일치";

            ownerEntity.reject(rejectedReason);

            assertThat(ownerEntity.getOwnerStatus()).isEqualTo(OwnerStatus.REJECTED);
            assertThat(ownerEntity.getRejectedReason()).isEqualTo(rejectedReason);
            assertThat(ownerEntity.getRejectedAt()).isNotNull();
            assertThat(ownerEntity.getApprovedAt()).isNull();
        }

        @Test
        @DisplayName("승인 후 거절 시 승인 정보 초기화")
        void reject_AfterApprove_ClearApproveInfo() {
            ownerEntity.approve();
            ownerEntity.reject("정책 위반");

            assertThat(ownerEntity.getOwnerStatus()).isEqualTo(OwnerStatus.REJECTED);
            assertThat(ownerEntity.getRejectedReason()).isEqualTo("정책 위반");
            assertThat(ownerEntity.getRejectedAt()).isNotNull();
            assertThat(ownerEntity.getApprovedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("상태 확인 메서드 테스트")
    class StatusCheckTest {

        @Test
        @DisplayName("isPending() - PENDING 상태")
        void isPending_Pending() {
            assertThat(ownerEntity.isPending()).isTrue();
        }

        @Test
        @DisplayName("isPending() - APPROVED 상태")
        void isPending_Approved() {
            ownerEntity.approve();

            assertThat(ownerEntity.isPending()).isFalse();
        }

        @Test
        @DisplayName("isPending() - REJECTED 상태")
        void isPending_Rejected() {
            ownerEntity.reject("거절 사유");

            assertThat(ownerEntity.isPending()).isFalse();
        }

        @Test
        @DisplayName("isApproved() - PENDING 상태")
        void isApproved_Pending() {
            assertThat(ownerEntity.isApproved()).isFalse();
        }

        @Test
        @DisplayName("isApproved() - APPROVED 상태")
        void isApproved_Approved() {
            ownerEntity.approve();

            assertThat(ownerEntity.isApproved()).isTrue();
        }

        @Test
        @DisplayName("isApproved() - REJECTED 상태")
        void isApproved_Rejected() {
            ownerEntity.reject("거절 사유");

            assertThat(ownerEntity.isApproved()).isFalse();
        }

        @Test
        @DisplayName("isRejected() - PENDING 상태")
        void isRejected_Pending() {
            assertThat(ownerEntity.isRejected()).isFalse();
        }

        @Test
        @DisplayName("isRejected() - APPROVED 상태")
        void isRejected_Approved() {
            ownerEntity.approve();

            assertThat(ownerEntity.isRejected()).isFalse();
        }

        @Test
        @DisplayName("isRejected() - REJECTED 상태")
        void isRejected_Rejected() {
            ownerEntity.reject("거절 사유");

            assertThat(ownerEntity.isRejected()).isTrue();
        }
    }

    @Nested
    @DisplayName("Builder 테스트")
    class BuilderTest {

        @Test
        @DisplayName("필수 필드만으로 빌드 성공")
        void builder_MinimalFields() {
            OwnerEntity owner = OwnerEntity.builder()
                    .user(userEntity)
                    .storeName("최소 스토어")
                    .ownerStatus(OwnerStatus.PENDING)
                    .build();

            assertThat(owner.getStoreName()).isEqualTo("최소 스토어");
            assertThat(owner.getOwnerStatus()).isEqualTo(OwnerStatus.PENDING);
            assertThat(owner.getBusinessNo()).isNull();
            assertThat(owner.getBank()).isNull();
        }

        @Test
        @DisplayName("모든 필드로 빌드 성공")
        void builder_AllFields() {
            OwnerEntity owner = OwnerEntity.builder()
                    .ownerId(UUID.randomUUID())
                    .user(userEntity)
                    .storeName("전체 필드 스토어")
                    .businessNo("111-22-33333")
                    .approvalRequest("전체 필드 승인 요청")
                    .zipCode("99999")
                    .address("대전시 서구")
                    .detailAddress("401동 4층")
                    .bank("우리은행")
                    .account("444-555-666777")
                    .ownerStatus(OwnerStatus.PENDING)
                    .build();

            assertThat(owner.getStoreName()).isEqualTo("전체 필드 스토어");
            assertThat(owner.getBusinessNo()).isEqualTo("111-22-33333");
            assertThat(owner.getApprovalRequest()).isEqualTo("전체 필드 승인 요청");
            assertThat(owner.getZipCode()).isEqualTo("99999");
            assertThat(owner.getBank()).isEqualTo("우리은행");
        }
    }
}
