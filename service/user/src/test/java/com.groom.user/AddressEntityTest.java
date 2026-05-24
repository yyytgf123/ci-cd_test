package com.groom.user;

import static org.assertj.core.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.groom.common.enums.UserRole;
import com.groom.user.domain.entity.address.AddressEntity;
import com.groom.user.domain.entity.user.UserEntity;
import com.groom.user.domain.entity.user.UserStatus;

@DisplayName("AddressEntity 테스트")
class AddressEntityTest {

    private AddressEntity addressEntity;
    private UserEntity userEntity;
    private UUID addressId;

    @BeforeEach
    void setUp() {
        addressId = UUID.randomUUID();
        
        userEntity = UserEntity.builder()
                .userId(UUID.randomUUID())
                .email("test@example.com")
                .password("encodedPassword")
                .nickname("testUser")
                .phoneNumber("010-1234-5678")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();

        addressEntity = AddressEntity.builder()
                .addressId(addressId)
                .user(userEntity)
                .zipCode("12345")
                .address("서울시 강남구")
                .detailAddress("101동 202호")
                .recipient("홍길동")
                .recipientPhone("010-9876-5432")
                .isDefault(false)
                .build();
    }

    @Nested
    @DisplayName("update() 테스트")
    class UpdateTest {

        @Test
        @DisplayName("모든 필드 업데이트 성공")
        void update_AllFields_Success() {
            addressEntity.update(
                    "54321",
                    "부산시 해운대구",
                    "201동 301호",
                    "김철수",
                    "010-1111-2222",
                    true
            );

            assertThat(addressEntity.getZipCode()).isEqualTo("54321");
            assertThat(addressEntity.getAddress()).isEqualTo("부산시 해운대구");
            assertThat(addressEntity.getDetailAddress()).isEqualTo("201동 301호");
            assertThat(addressEntity.getRecipient()).isEqualTo("김철수");
            assertThat(addressEntity.getRecipientPhone()).isEqualTo("010-1111-2222");
            assertThat(addressEntity.getIsDefault()).isTrue();
        }

        @Test
        @DisplayName("isDefault가 null인 경우 기존 값 유지")
        void update_NullIsDefault_KeepOriginal() {
            Boolean originalDefault = addressEntity.getIsDefault();

            addressEntity.update(
                    "99999",
                    "대전시 서구",
                    "301동 401호",
                    "이영희",
                    "010-3333-4444",
                    null
            );

            assertThat(addressEntity.getIsDefault()).isEqualTo(originalDefault);
        }

        @Test
        @DisplayName("isDefault를 true에서 false로 변경")
        void update_IsDefault_TrueToFalse() {
            addressEntity.setDefault(true);
            
            addressEntity.update(
                    "88888",
                    "광주시 북구",
                    "401동 501호",
                    "박민수",
                    "010-5555-6666",
                    false
            );

            assertThat(addressEntity.getIsDefault()).isFalse();
        }

        @Test
        @DisplayName("수령인 정보만 업데이트")
        void update_RecipientOnly() {
            String originalZipCode = addressEntity.getZipCode();
            String originalAddress = addressEntity.getAddress();
            String originalDetailAddress = addressEntity.getDetailAddress();

            addressEntity.update(
                    originalZipCode,
                    originalAddress,
                    originalDetailAddress,
                    "새로운 수령인",
                    "010-7777-8888",
                    null
            );

            assertThat(addressEntity.getZipCode()).isEqualTo(originalZipCode);
            assertThat(addressEntity.getAddress()).isEqualTo(originalAddress);
            assertThat(addressEntity.getRecipient()).isEqualTo("새로운 수령인");
            assertThat(addressEntity.getRecipientPhone()).isEqualTo("010-7777-8888");
        }
    }

    @Nested
    @DisplayName("setDefault() 테스트")
    class SetDefaultTest {

        @Test
        @DisplayName("기본 배송지로 설정")
        void setDefault_True() {
            addressEntity.setDefault(true);

            assertThat(addressEntity.getIsDefault()).isTrue();
        }

        @Test
        @DisplayName("기본 배송지 해제")
        void setDefault_False() {
            addressEntity.setDefault(true);
            addressEntity.setDefault(false);

            assertThat(addressEntity.getIsDefault()).isFalse();
        }

        @Test
        @DisplayName("이미 true인 상태에서 true로 설정해도 유지")
        void setDefault_TrueToTrue() {
            addressEntity.setDefault(true);
            addressEntity.setDefault(true);

            assertThat(addressEntity.getIsDefault()).isTrue();
        }

        @Test
        @DisplayName("이미 false인 상태에서 false로 설정해도 유지")
        void setDefault_FalseToFalse() {
            addressEntity.setDefault(false);
            addressEntity.setDefault(false);

            assertThat(addressEntity.getIsDefault()).isFalse();
        }
    }

    @Nested
    @DisplayName("Builder 테스트")
    class BuilderTest {

        @Test
        @DisplayName("기본값으로 isDefault가 false")
        void builder_DefaultIsDefaultFalse() {
            AddressEntity address = AddressEntity.builder()
                    .user(userEntity)
                    .zipCode("11111")
                    .address("서울시 마포구")
                    .detailAddress("501동 601호")
                    .build();

            assertThat(address.getIsDefault()).isFalse();
        }

        @Test
        @DisplayName("isDefault를 true로 설정")
        void builder_IsDefaultTrue() {
            AddressEntity address = AddressEntity.builder()
                    .user(userEntity)
                    .zipCode("22222")
                    .address("서울시 송파구")
                    .detailAddress("601동 701호")
                    .isDefault(true)
                    .build();

            assertThat(address.getIsDefault()).isTrue();
        }

        @Test
        @DisplayName("수령인 정보 없이 빌드")
        void builder_WithoutRecipientInfo() {
            AddressEntity address = AddressEntity.builder()
                    .user(userEntity)
                    .zipCode("33333")
                    .address("서울시 영등포구")
                    .detailAddress("701동 801호")
                    .build();

            assertThat(address.getRecipient()).isNull();
            assertThat(address.getRecipientPhone()).isNull();
        }

        @Test
        @DisplayName("모든 필드로 빌드")
        void builder_AllFields() {
            UUID id = UUID.randomUUID();
            AddressEntity address = AddressEntity.builder()
                    .addressId(id)
                    .user(userEntity)
                    .zipCode("44444")
                    .address("서울시 종로구")
                    .detailAddress("801동 901호")
                    .recipient("전체필드수령인")
                    .recipientPhone("010-9999-0000")
                    .isDefault(true)
                    .build();

            assertThat(address.getAddressId()).isEqualTo(id);
            assertThat(address.getUser()).isEqualTo(userEntity);
            assertThat(address.getZipCode()).isEqualTo("44444");
            assertThat(address.getAddress()).isEqualTo("서울시 종로구");
            assertThat(address.getDetailAddress()).isEqualTo("801동 901호");
            assertThat(address.getRecipient()).isEqualTo("전체필드수령인");
            assertThat(address.getRecipientPhone()).isEqualTo("010-9999-0000");
            assertThat(address.getIsDefault()).isTrue();
        }
    }

    @Nested
    @DisplayName("User 관계 테스트")
    class UserRelationTest {

        @Test
        @DisplayName("User 엔티티 연관 확인")
        void userRelation() {
            assertThat(addressEntity.getUser()).isEqualTo(userEntity);
            assertThat(addressEntity.getUser().getUserId()).isEqualTo(userEntity.getUserId());
        }
    }
}
