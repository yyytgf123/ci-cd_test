package com.groom.user;

import static org.assertj.core.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.groom.common.enums.UserRole;
import com.groom.user.domain.entity.user.UserEntity;
import com.groom.user.domain.entity.user.UserStatus;

@DisplayName("UserEntity 테스트")
class UserEntityTest {

    private UserEntity userEntity;
    private UUID userId;

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
    }

    @Nested
    @DisplayName("updateNickname() 테스트")
    class UpdateNicknameTest {

        @Test
        @DisplayName("닉네임 수정 성공")
        void updateNickname_Success() {
            userEntity.updateNickname("newNickname");

            assertThat(userEntity.getNickname()).isEqualTo("newNickname");
        }

        @Test
        @DisplayName("닉네임 공백 제거 후 저장")
        void updateNickname_Trim() {
            userEntity.updateNickname("  trimmedNickname  ");

            assertThat(userEntity.getNickname()).isEqualTo("trimmedNickname");
        }

        @Test
        @DisplayName("null 닉네임으로 수정 시 예외 발생")
        void updateNickname_Null_ThrowsException() {
            assertThatThrownBy(() -> userEntity.updateNickname(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("nickname must not be blank");
        }

        @Test
        @DisplayName("빈 문자열 닉네임으로 수정 시 예외 발생")
        void updateNickname_Empty_ThrowsException() {
            assertThatThrownBy(() -> userEntity.updateNickname(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("nickname must not be blank");
        }

        @Test
        @DisplayName("공백만 있는 닉네임으로 수정 시 예외 발생")
        void updateNickname_Whitespace_ThrowsException() {
            assertThatThrownBy(() -> userEntity.updateNickname("   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("nickname must not be blank");
        }
    }

    @Nested
    @DisplayName("updatePhoneNumber() 테스트")
    class UpdatePhoneNumberTest {

        @Test
        @DisplayName("전화번호 수정 성공")
        void updatePhoneNumber_Success() {
            userEntity.updatePhoneNumber("010-9999-8888");

            assertThat(userEntity.getPhoneNumber()).isEqualTo("010-9999-8888");
        }

        @Test
        @DisplayName("전화번호 공백 제거 후 저장")
        void updatePhoneNumber_Trim() {
            userEntity.updatePhoneNumber("  010-1111-2222  ");

            assertThat(userEntity.getPhoneNumber()).isEqualTo("010-1111-2222");
        }

        @Test
        @DisplayName("null 전화번호로 수정 시 예외 발생")
        void updatePhoneNumber_Null_ThrowsException() {
            assertThatThrownBy(() -> userEntity.updatePhoneNumber(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("phoneNumber must not be blank");
        }

        @Test
        @DisplayName("빈 문자열 전화번호로 수정 시 예외 발생")
        void updatePhoneNumber_Empty_ThrowsException() {
            assertThatThrownBy(() -> userEntity.updatePhoneNumber(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("phoneNumber must not be blank");
        }
    }

    @Nested
    @DisplayName("updatePassword() 테스트")
    class UpdatePasswordTest {

        @Test
        @DisplayName("비밀번호 수정 성공")
        void updatePassword_Success() {
            userEntity.updatePassword("newEncodedPassword");

            assertThat(userEntity.getPassword()).isEqualTo("newEncodedPassword");
        }

        @Test
        @DisplayName("null 비밀번호로 수정 시 예외 발생")
        void updatePassword_Null_ThrowsException() {
            assertThatThrownBy(() -> userEntity.updatePassword(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("password must not be blank");
        }

        @Test
        @DisplayName("빈 문자열 비밀번호로 수정 시 예외 발생")
        void updatePassword_Empty_ThrowsException() {
            assertThatThrownBy(() -> userEntity.updatePassword(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("password must not be blank");
        }
    }

    @Nested
    @DisplayName("withdraw() 테스트")
    class WithdrawTest {

        @Test
        @DisplayName("회원 탈퇴 성공")
        void withdraw_Success() {
            userEntity.withdraw();

            assertThat(userEntity.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
            assertThat(userEntity.isWithdrawn()).isTrue();
        }

        @Test
        @DisplayName("이미 탈퇴한 사용자가 탈퇴해도 상태 유지 (멱등성)")
        void withdraw_Idempotent() {
            userEntity.withdraw();
            userEntity.withdraw();

            assertThat(userEntity.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
        }

        @Test
        @DisplayName("deletedBy 파라미터와 함께 탈퇴")
        void withdraw_WithDeletedBy() {
            userEntity.withdraw("admin");

            assertThat(userEntity.isWithdrawn()).isTrue();
        }
    }

    @Nested
    @DisplayName("ban() 테스트")
    class BanTest {

        @Test
        @DisplayName("제재 성공")
        void ban_Success() {
            userEntity.ban();

            assertThat(userEntity.getStatus()).isEqualTo(UserStatus.BANNED);
            assertThat(userEntity.isBanned()).isTrue();
        }
    }

    @Nested
    @DisplayName("activate() 테스트")
    class ActivateTest {

        @Test
        @DisplayName("제재된 사용자 활성화 성공")
        void activate_FromBanned_Success() {
            userEntity.ban();
            userEntity.activate();

            assertThat(userEntity.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(userEntity.isBanned()).isFalse();
        }

        @Test
        @DisplayName("탈퇴한 사용자 활성화 시 예외 발생")
        void activate_FromWithdrawn_ThrowsException() {
            userEntity.withdraw();

            assertThatThrownBy(() -> userEntity.activate())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("withdrawn user must use reactivate()");
        }
    }

    @Nested
    @DisplayName("reactivate() 테스트")
    class ReactivateTest {

        @Test
        @DisplayName("탈퇴한 사용자 재활성화 성공")
        void reactivate_Success() {
            userEntity.withdraw();

            userEntity.reactivate("newPassword", "newNickname", "010-0000-0000");

            assertThat(userEntity.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(userEntity.getPassword()).isEqualTo("newPassword");
            assertThat(userEntity.getNickname()).isEqualTo("newNickname");
            assertThat(userEntity.getPhoneNumber()).isEqualTo("010-0000-0000");
        }

        @Test
        @DisplayName("활성 상태 사용자 재활성화 시 예외 발생")
        void reactivate_FromActive_ThrowsException() {
            assertThatThrownBy(() -> userEntity.reactivate("pw", "nick", "phone"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("only withdrawn user can be reactivated");
        }

        @Test
        @DisplayName("제재된 사용자 재활성화 시 예외 발생")
        void reactivate_FromBanned_ThrowsException() {
            userEntity.ban();

            assertThatThrownBy(() -> userEntity.reactivate("pw", "nick", "phone"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("only withdrawn user can be reactivated");
        }

        @Test
        @DisplayName("null 비밀번호로 재활성화 시 예외 발생")
        void reactivate_NullPassword_ThrowsException() {
            userEntity.withdraw();

            assertThatThrownBy(() -> userEntity.reactivate(null, "nick", "phone"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("password must not be blank");
        }

        @Test
        @DisplayName("null 닉네임으로 재활성화 시 예외 발생")
        void reactivate_NullNickname_ThrowsException() {
            userEntity.withdraw();

            assertThatThrownBy(() -> userEntity.reactivate("pw", null, "phone"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("nickname must not be blank");
        }

        @Test
        @DisplayName("null 전화번호로 재활성화 시 예외 발생")
        void reactivate_NullPhone_ThrowsException() {
            userEntity.withdraw();

            assertThatThrownBy(() -> userEntity.reactivate("pw", "nick", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("phoneNumber must not be blank");
        }
    }

    @Nested
    @DisplayName("상태 확인 메서드 테스트")
    class StatusCheckTest {

        @Test
        @DisplayName("isWithdrawn() - 활성 상태")
        void isWithdrawn_Active() {
            assertThat(userEntity.isWithdrawn()).isFalse();
        }

        @Test
        @DisplayName("isWithdrawn() - 탈퇴 상태")
        void isWithdrawn_Withdrawn() {
            userEntity.withdraw();

            assertThat(userEntity.isWithdrawn()).isTrue();
        }

        @Test
        @DisplayName("isBanned() - 활성 상태")
        void isBanned_Active() {
            assertThat(userEntity.isBanned()).isFalse();
        }

        @Test
        @DisplayName("isBanned() - 제재 상태")
        void isBanned_Banned() {
            userEntity.ban();

            assertThat(userEntity.isBanned()).isTrue();
        }
    }

    @Nested
    @DisplayName("Builder 테스트")
    class BuilderTest {

        @Test
        @DisplayName("필수 필드만으로 빌드 성공")
        void builder_MinimalFields() {
            UserEntity user = UserEntity.builder()
                    .email("builder@example.com")
                    .password("password")
                    .nickname("builder")
                    .phoneNumber("010-0000-0000")
                    .role(UserRole.USER)
                    .status(UserStatus.ACTIVE)
                    .build();

            assertThat(user.getEmail()).isEqualTo("builder@example.com");
            assertThat(user.getRole()).isEqualTo(UserRole.USER);
            assertThat(user.getAddresses()).isEmpty();
        }

        @Test
        @DisplayName("모든 역할로 빌드 가능")
        void builder_AllRoles() {
            for (UserRole role : UserRole.values()) {
                UserEntity user = UserEntity.builder()
                        .email(role.name().toLowerCase() + "@example.com")
                        .password("password")
                        .nickname(role.name().toLowerCase())
                        .phoneNumber("010-0000-0000")
                        .role(role)
                        .status(UserStatus.ACTIVE)
                        .build();

                assertThat(user.getRole()).isEqualTo(role);
            }
        }
    }
}
