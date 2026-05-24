package com.groom.user;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.groom.common.enums.UserRole;
import com.groom.user.application.port.out.SalesData;
import com.groom.user.domain.entity.address.AddressEntity;
import com.groom.user.domain.entity.owner.OwnerEntity;
import com.groom.user.domain.entity.owner.OwnerStatus;
import com.groom.user.domain.entity.user.PeriodType;
import com.groom.user.domain.entity.user.UserEntity;
import com.groom.user.domain.entity.user.UserStatus;
import com.groom.user.presentation.dto.response.address.ResAddressDtoV1;
import com.groom.user.presentation.dto.response.admin.ResOwnerApprovalListDtoV1;
import com.groom.user.presentation.dto.response.owner.ResOwnerApprovalDtoV1;
import com.groom.user.presentation.dto.response.owner.ResSalesStatDtoV1;
import com.groom.user.presentation.dto.response.user.ResTokenDtoV1;
import com.groom.user.presentation.dto.response.user.ResUserDtoV1;
import com.groom.user.presentation.dto.response.user.ResUserListDtoV1;
@Tag("Integration")

@DisplayName("DTO 및 기타 클래스 테스트")
class DtoTest {

    @Nested
    @DisplayName("ResUserDtoV1 테스트")
    class ResUserDtoV1Test {

        @Test
        @DisplayName("UserEntity로부터 DTO 생성")
        void from_UserEntity() {
            UUID userId = UUID.randomUUID();
            UserEntity user = UserEntity.builder()
                    .userId(userId)
                    .email("test@example.com")
                    .nickname("testUser")
                    .phoneNumber("010-1234-5678")
                    .role(UserRole.USER)
                    .status(UserStatus.ACTIVE)
                    .build();

            ResUserDtoV1 dto = ResUserDtoV1.from(user);

            assertThat(dto.getId()).isEqualTo(userId);
            assertThat(dto.getEmail()).isEqualTo("test@example.com");
            assertThat(dto.getNickname()).isEqualTo("testUser");
            assertThat(dto.getRole()).isEqualTo(UserRole.USER);
            assertThat(dto.getStatus()).isEqualTo(UserStatus.ACTIVE);
        }

        @Test
        @DisplayName("UserEntity와 AddressEntity로부터 DTO 생성")
        void from_UserEntity_WithAddress() {
            UUID userId = UUID.randomUUID();
            UserEntity user = UserEntity.builder()
                    .userId(userId)
                    .email("test@example.com")
                    .nickname("testUser")
                    .phoneNumber("010-1234-5678")
                    .role(UserRole.USER)
                    .status(UserStatus.ACTIVE)
                    .build();

            AddressEntity address = AddressEntity.builder()
                    .addressId(UUID.randomUUID())
                    .user(user)
                    .zipCode("12345")
                    .address("서울시 강남구")
                    .detailAddress("101동")
                    .isDefault(true)
                    .build();

            ResUserDtoV1 dto = ResUserDtoV1.from(user, address);

            assertThat(dto.getDefaultAddress()).isNotNull();
            assertThat(dto.getDefaultAddress().getZipCode()).isEqualTo("12345");
        }

        @Test
        @DisplayName("UserEntity, AddressEntity, OwnerEntity로부터 DTO 생성")
        void from_UserEntity_WithOwner() {
            UUID userId = UUID.randomUUID();
            UserEntity user = UserEntity.builder()
                    .userId(userId)
                    .email("owner@example.com")
                    .nickname("ownerUser")
                    .phoneNumber("010-1234-5678")
                    .role(UserRole.OWNER)
                    .status(UserStatus.ACTIVE)
                    .build();

            OwnerEntity owner = OwnerEntity.builder()
                    .ownerId(UUID.randomUUID())
                    .user(user)
                    .storeName("테스트 스토어")
                    .ownerStatus(OwnerStatus.APPROVED)
                    .build();

            ResUserDtoV1 dto = ResUserDtoV1.from(user, null, owner);

            assertThat(dto.getOwnerInfo()).isNotNull();
            assertThat(dto.getOwnerInfo().getStoreName()).isEqualTo("테스트 스토어");
        }
    }

    @Nested
    @DisplayName("ResUserListDtoV1 테스트")
    class ResUserListDtoV1Test {

        @Test
        @DisplayName("Page로부터 DTO 생성")
        void from_Page() {
            ResUserDtoV1 userDto = ResUserDtoV1.builder()
                    .id(UUID.randomUUID())
                    .email("test@example.com")
                    .nickname("testUser")
                    .role(UserRole.USER)
                    .status(UserStatus.ACTIVE)
                    .build();

            Page<ResUserDtoV1> page = new PageImpl<>(
                    List.of(userDto),
                    PageRequest.of(0, 20),
                    1
            );

            ResUserListDtoV1 dto = ResUserListDtoV1.from(page);

            assertThat(dto.getUsers()).hasSize(1);
            assertThat(dto.getPage()).isZero();
            assertThat(dto.getSize()).isEqualTo(20);
            assertThat(dto.getTotalElements()).isEqualTo(1);
            assertThat(dto.getTotalPages()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("ResTokenDtoV1 테스트")
    class ResTokenDtoV1Test {

        @Test
        @DisplayName("토큰 DTO 생성")
        void of() {
            ResTokenDtoV1 dto = ResTokenDtoV1.of("accessToken123", "refreshToken456");

            assertThat(dto.getAccessToken()).isEqualTo("accessToken123");
            assertThat(dto.getRefreshToken()).isEqualTo("refreshToken456");
            assertThat(dto.getTokenType()).isEqualTo("Bearer");
            assertThat(dto.getExpiresIn()).isEqualTo(3600);
        }
    }

    @Nested
    @DisplayName("ResAddressDtoV1 테스트")
    class ResAddressDtoV1Test {

        @Test
        @DisplayName("AddressEntity로부터 DTO 생성")
        void from_AddressEntity() {
            UUID addressId = UUID.randomUUID();
            UserEntity user = UserEntity.builder()
                    .userId(UUID.randomUUID())
                    .email("test@example.com")
                    .nickname("testUser")
                    .phoneNumber("010-1234-5678")
                    .role(UserRole.USER)
                    .status(UserStatus.ACTIVE)
                    .build();

            AddressEntity address = AddressEntity.builder()
                    .addressId(addressId)
                    .user(user)
                    .zipCode("12345")
                    .address("서울시 강남구")
                    .detailAddress("101동 202호")
                    .recipient("홍길동")
                    .recipientPhone("010-9876-5432")
                    .isDefault(true)
                    .build();

            ResAddressDtoV1 dto = ResAddressDtoV1.from(address);

            assertThat(dto.getId()).isEqualTo(addressId);
            assertThat(dto.getZipCode()).isEqualTo("12345");
            assertThat(dto.getAddress()).isEqualTo("서울시 강남구");
            assertThat(dto.getDetailAddress()).isEqualTo("101동 202호");
            assertThat(dto.getRecipient()).isEqualTo("홍길동");
            assertThat(dto.getRecipientPhone()).isEqualTo("010-9876-5432");
            assertThat(dto.getIsDefault()).isTrue();
        }
    }

    @Nested
    @DisplayName("ResOwnerApprovalDtoV1 테스트")
    class ResOwnerApprovalDtoV1Test {

        @Test
        @DisplayName("OwnerEntity로부터 DTO 생성")
        void from_OwnerEntity() {
            UUID userId = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID();
            
            UserEntity user = UserEntity.builder()
                    .userId(userId)
                    .email("owner@example.com")
                    .nickname("ownerUser")
                    .phoneNumber("010-1234-5678")
                    .role(UserRole.OWNER)
                    .status(UserStatus.ACTIVE)
                    .build();

            OwnerEntity owner = OwnerEntity.builder()
                    .ownerId(ownerId)
                    .user(user)
                    .storeName("테스트 스토어")
                    .businessNo("123-45-67890")
                    .approvalRequest("승인 요청합니다")
                    .zipCode("12345")
                    .address("서울시 강남구")
                    .detailAddress("101호")
                    .ownerStatus(OwnerStatus.PENDING)
                    .build();

            ResOwnerApprovalDtoV1 dto = ResOwnerApprovalDtoV1.from(owner);

            assertThat(dto.getOwnerId()).isEqualTo(ownerId);
            assertThat(dto.getUserId()).isEqualTo(userId);
            assertThat(dto.getEmail()).isEqualTo("owner@example.com");
            assertThat(dto.getNickname()).isEqualTo("ownerUser");
            assertThat(dto.getStoreName()).isEqualTo("테스트 스토어");
            assertThat(dto.getBusinessNo()).isEqualTo("123-45-67890");
            assertThat(dto.getOwnerStatus()).isEqualTo(OwnerStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("ResOwnerApprovalListDtoV1 테스트")
    class ResOwnerApprovalListDtoV1Test {

        @Test
        @DisplayName("Page로부터 DTO 생성")
        void from_Page() {
            ResOwnerApprovalDtoV1 ownerDto = ResOwnerApprovalDtoV1.builder()
                    .ownerId(UUID.randomUUID())
                    .storeName("테스트 스토어")
                    .ownerStatus(OwnerStatus.PENDING)
                    .build();

            Page<ResOwnerApprovalDtoV1> page = new PageImpl<>(
                    List.of(ownerDto),
                    PageRequest.of(0, 20),
                    1
            );

            ResOwnerApprovalListDtoV1 dto = ResOwnerApprovalListDtoV1.from(page);

            assertThat(dto.getContent()).hasSize(1);
            assertThat(dto.getPage()).isZero();
            assertThat(dto.getSize()).isEqualTo(20);
            assertThat(dto.getTotalElements()).isEqualTo(1);
            assertThat(dto.isFirst()).isTrue();
            assertThat(dto.isLast()).isTrue();
        }
    }

    @Nested
    @DisplayName("ResSalesStatDtoV1 테스트")
    class ResSalesStatDtoV1Test {

        @Test
        @DisplayName("매출 통계 DTO 생성")
        void of() {
            LocalDate date = LocalDate.of(2025, 1, 15);
            ResSalesStatDtoV1 dto = ResSalesStatDtoV1.of(date, 100000L);

            assertThat(dto.getDate()).isEqualTo(date);
            assertThat(dto.getTotalAmount()).isEqualTo(100000L);
        }

        @Test
        @DisplayName("매출 0인 경우")
        void of_ZeroAmount() {
            LocalDate date = LocalDate.of(2025, 1, 1);
            ResSalesStatDtoV1 dto = ResSalesStatDtoV1.of(date, 0L);

            assertThat(dto.getTotalAmount()).isZero();
        }
    }

    @Nested
    @DisplayName("SalesData 테스트")
    class SalesDataTest {

        @Test
        @DisplayName("SalesData.of() 테스트")
        void of() {
            LocalDate date = LocalDate.of(2025, 1, 15);
            SalesData data = SalesData.of(date, 500000L, 50L);

            assertThat(data.getDate()).isEqualTo(date);
            assertThat(data.getTotalAmount()).isEqualTo(500000L);
            assertThat(data.getOrderCount()).isEqualTo(50L);
        }

        @Test
        @DisplayName("SalesData.empty() 테스트")
        void empty() {
            LocalDate date = LocalDate.of(2025, 1, 15);
            SalesData data = SalesData.empty(date);

            assertThat(data.getDate()).isEqualTo(date);
            assertThat(data.getTotalAmount()).isZero();
            assertThat(data.getOrderCount()).isZero();
        }

        @Test
        @DisplayName("Builder 테스트")
        void builder() {
            LocalDate date = LocalDate.of(2025, 2, 1);
            SalesData data = SalesData.builder()
                    .date(date)
                    .totalAmount(1000000L)
                    .orderCount(100L)
                    .build();

            assertThat(data.getDate()).isEqualTo(date);
            assertThat(data.getTotalAmount()).isEqualTo(1000000L);
            assertThat(data.getOrderCount()).isEqualTo(100L);
        }
    }

    @Nested
    @DisplayName("Enum 테스트")
    class EnumTest {

        @Test
        @DisplayName("UserStatus enum 테스트")
        void userStatus() {
            assertThat(UserStatus.values()).containsExactly(
                    UserStatus.ACTIVE,
                    UserStatus.BANNED,
                    UserStatus.PENDING,
                    UserStatus.WITHDRAWN
            );
        }

        @Test
        @DisplayName("OwnerStatus enum 테스트")
        void ownerStatus() {
            assertThat(OwnerStatus.values()).containsExactly(
                    OwnerStatus.PENDING,
                    OwnerStatus.APPROVED,
                    OwnerStatus.REJECTED
            );
        }

        @Test
        @DisplayName("PeriodType enum 테스트")
        void periodType() {
            assertThat(PeriodType.values()).containsExactly(
                    PeriodType.DAILY,
                    PeriodType.MONTHLY
            );
        }

        @Test
        @DisplayName("UserRole enum 테스트")
        void userRole() {
            assertThat(UserRole.values()).contains(
                    UserRole.USER,
                    UserRole.OWNER,
                    UserRole.MANAGER,
                    UserRole.MASTER
            );
        }
    }
}
