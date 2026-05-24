package com.groom.user;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.groom.common.enums.UserRole;
import com.groom.user.domain.event.OwnerSignedUpEvent;
import com.groom.user.domain.event.UserSignedUpEvent;
import com.groom.user.domain.event.UserUpdateEvent;
import com.groom.user.domain.event.UserWithdrawnEvent;

@DisplayName("도메인 이벤트 테스트")
class DomainEventTest {

    @Nested
    @DisplayName("UserSignedUpEvent 테스트")
    class UserSignedUpEventTest {

        @Test
        @DisplayName("이벤트 생성 및 필드 확인")
        void create_Success() {
            UUID userId = UUID.randomUUID();
            String email = "test@example.com";
            UserRole role = UserRole.USER;

            UserSignedUpEvent event = new UserSignedUpEvent(userId, email, role);

            assertThat(event.getUserId()).isEqualTo(userId);
            assertThat(event.getEmail()).isEqualTo(email);
            assertThat(event.getRole()).isEqualTo(role);
        }

        @Test
        @DisplayName("다양한 역할로 이벤트 생성")
        void create_VariousRoles() {
            UUID userId = UUID.randomUUID();

            for (UserRole role : UserRole.values()) {
                UserSignedUpEvent event = new UserSignedUpEvent(userId, role.name() + "@example.com", role);
                assertThat(event.getRole()).isEqualTo(role);
            }
        }
    }

    @Nested
    @DisplayName("UserWithdrawnEvent 테스트")
    class UserWithdrawnEventTest {

        @Test
        @DisplayName("이벤트 생성 및 필드 확인")
        void create_Success() {
            UUID userId = UUID.randomUUID();

            UserWithdrawnEvent event = new UserWithdrawnEvent(userId);

            assertThat(event.getUserId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("null userId로 이벤트 생성")
        void create_NullUserId() {
            UserWithdrawnEvent event = new UserWithdrawnEvent(null);

            assertThat(event.getUserId()).isNull();
        }
    }

    @Nested
    @DisplayName("UserUpdateEvent 테스트")
    class UserUpdateEventTest {

        @Test
        @DisplayName("모든 필드로 이벤트 생성")
        void create_AllFields() {
            UUID userId = UUID.randomUUID();
            String nickname = "newNickname";
            String phoneNumber = "010-1234-5678";
            boolean password = true;
            LocalDateTime occurredAt = LocalDateTime.now();

            UserUpdateEvent event = UserUpdateEvent.builder()
                    .userId(userId)
                    .nickname(nickname)
                    .phoneNumber(phoneNumber)
                    .password(password)
                    .occurredAt(occurredAt)
                    .build();

            assertThat(event.getUserId()).isEqualTo(userId);
            assertThat(event.getNickname()).isEqualTo(nickname);
            assertThat(event.getPhoneNumber()).isEqualTo(phoneNumber);
            assertThat(event.isPassword()).isTrue();
            assertThat(event.getOccurredAt()).isEqualTo(occurredAt);
        }

        @Test
        @DisplayName("닉네임만 변경된 이벤트 생성")
        void create_NicknameOnly() {
            UUID userId = UUID.randomUUID();

            UserUpdateEvent event = UserUpdateEvent.builder()
                    .userId(userId)
                    .nickname("onlyNickname")
                    .password(false)
                    .occurredAt(LocalDateTime.now())
                    .build();

            assertThat(event.getNickname()).isEqualTo("onlyNickname");
            assertThat(event.getPhoneNumber()).isNull();
            assertThat(event.isPassword()).isFalse();
        }

        @Test
        @DisplayName("전화번호만 변경된 이벤트 생성")
        void create_PhoneNumberOnly() {
            UUID userId = UUID.randomUUID();

            UserUpdateEvent event = UserUpdateEvent.builder()
                    .userId(userId)
                    .phoneNumber("010-9999-8888")
                    .password(false)
                    .occurredAt(LocalDateTime.now())
                    .build();

            assertThat(event.getNickname()).isNull();
            assertThat(event.getPhoneNumber()).isEqualTo("010-9999-8888");
            assertThat(event.isPassword()).isFalse();
        }

        @Test
        @DisplayName("비밀번호만 변경된 이벤트 생성")
        void create_PasswordOnly() {
            UUID userId = UUID.randomUUID();

            UserUpdateEvent event = UserUpdateEvent.builder()
                    .userId(userId)
                    .password(true)
                    .occurredAt(LocalDateTime.now())
                    .build();

            assertThat(event.getNickname()).isNull();
            assertThat(event.getPhoneNumber()).isNull();
            assertThat(event.isPassword()).isTrue();
        }
    }

    @Nested
    @DisplayName("OwnerSignedUpEvent 테스트")
    class OwnerSignedUpEventTest {

        @Test
        @DisplayName("이벤트 생성 및 필드 확인")
        void create_Success() {
            UUID userId = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID();
            String email = "owner@example.com";
            String storeName = "테스트 스토어";

            OwnerSignedUpEvent event = new OwnerSignedUpEvent(userId, ownerId, email, storeName);

            assertThat(event.getUserId()).isEqualTo(userId);
            assertThat(event.getOwnerId()).isEqualTo(ownerId);
            assertThat(event.getEmail()).isEqualTo(email);
            assertThat(event.getStoreName()).isEqualTo(storeName);
        }

        @Test
        @DisplayName("다양한 스토어명으로 이벤트 생성")
        void create_VariousStoreNames() {
            UUID userId = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID();

            // 한글 스토어명
            OwnerSignedUpEvent event1 = new OwnerSignedUpEvent(userId, ownerId, "test@example.com", "맛있는 치킨");
            assertThat(event1.getStoreName()).isEqualTo("맛있는 치킨");

            // 영문 스토어명
            OwnerSignedUpEvent event2 = new OwnerSignedUpEvent(userId, ownerId, "test@example.com", "Delicious Pizza");
            assertThat(event2.getStoreName()).isEqualTo("Delicious Pizza");

            // 숫자 포함 스토어명
            OwnerSignedUpEvent event3 = new OwnerSignedUpEvent(userId, ownerId, "test@example.com", "카페 24시");
            assertThat(event3.getStoreName()).isEqualTo("카페 24시");
        }

        @Test
        @DisplayName("null 값으로 이벤트 생성")
        void create_NullValues() {
            OwnerSignedUpEvent event = new OwnerSignedUpEvent(null, null, null, null);

            assertThat(event.getUserId()).isNull();
            assertThat(event.getOwnerId()).isNull();
            assertThat(event.getEmail()).isNull();
            assertThat(event.getStoreName()).isNull();
        }
    }
}
