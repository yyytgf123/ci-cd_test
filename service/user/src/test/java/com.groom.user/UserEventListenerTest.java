package com.groom.user;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.groom.common.enums.UserRole;
import com.groom.user.application.service.listener.UserEventListener;
import com.groom.user.domain.event.UserSignedUpEvent;
import com.groom.user.domain.event.UserUpdateEvent;
import com.groom.user.domain.event.UserWithdrawnEvent;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserEventListener 테스트")
class UserEventListenerTest {

    @InjectMocks
    private UserEventListener userEventListener;

    @Nested
    @DisplayName("handleUserWithdrawn() 테스트")
    class HandleUserWithdrawnTest {

        @Test
        @DisplayName("사용자 탈퇴 이벤트 처리 성공")
        void handleUserWithdrawn_Success() {
            UUID userId = UUID.randomUUID();
            UserWithdrawnEvent event = new UserWithdrawnEvent(userId);

            assertThatCode(() -> userEventListener.handleUserWithdrawn(event))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("null userId로 탈퇴 이벤트 처리")
        void handleUserWithdrawn_NullUserId() {
            UserWithdrawnEvent event = new UserWithdrawnEvent(null);

            assertThatCode(() -> userEventListener.handleUserWithdrawn(event))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("handleUserUpdated() 테스트")
    class HandleUserUpdatedTest {

        @Test
        @DisplayName("닉네임 변경 이벤트 처리")
        void handleUserUpdated_NicknameChanged() {
            UUID userId = UUID.randomUUID();
            UserUpdateEvent event = UserUpdateEvent.builder()
                    .userId(userId)
                    .nickname("newNickname")
                    .phoneNumber(null)
                    .password(false)
                    .occurredAt(LocalDateTime.now())
                    .build();

            assertThatCode(() -> userEventListener.handleUserUpdated(event))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("전화번호 변경 이벤트 처리")
        void handleUserUpdated_PhoneNumberChanged() {
            UUID userId = UUID.randomUUID();
            UserUpdateEvent event = UserUpdateEvent.builder()
                    .userId(userId)
                    .nickname(null)
                    .phoneNumber("010-9999-8888")
                    .password(false)
                    .occurredAt(LocalDateTime.now())
                    .build();

            assertThatCode(() -> userEventListener.handleUserUpdated(event))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("비밀번호 변경 이벤트 처리")
        void handleUserUpdated_PasswordChanged() {
            UUID userId = UUID.randomUUID();
            UserUpdateEvent event = UserUpdateEvent.builder()
                    .userId(userId)
                    .nickname(null)
                    .phoneNumber(null)
                    .password(true)
                    .occurredAt(LocalDateTime.now())
                    .build();

            assertThatCode(() -> userEventListener.handleUserUpdated(event))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("모든 필드 변경 이벤트 처리")
        void handleUserUpdated_AllFieldsChanged() {
            UUID userId = UUID.randomUUID();
            UserUpdateEvent event = UserUpdateEvent.builder()
                    .userId(userId)
                    .nickname("allChangedNickname")
                    .phoneNumber("010-0000-1111")
                    .password(true)
                    .occurredAt(LocalDateTime.now())
                    .build();

            assertThatCode(() -> userEventListener.handleUserUpdated(event))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("null userId로 업데이트 이벤트 처리")
        void handleUserUpdated_NullUserId() {
            UserUpdateEvent event = UserUpdateEvent.builder()
                    .userId(null)
                    .nickname("test")
                    .password(false)
                    .occurredAt(LocalDateTime.now())
                    .build();

            assertThatCode(() -> userEventListener.handleUserUpdated(event))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("handleUserSignedUp() 테스트")
    class HandleUserSignedUpTest {

        @Test
        @DisplayName("USER 역할 회원가입 이벤트 처리")
        void handleUserSignedUp_User() {
            UUID userId = UUID.randomUUID();
            UserSignedUpEvent event = new UserSignedUpEvent(userId, "user@example.com", UserRole.USER);

            assertThatCode(() -> userEventListener.handleUserSignedUp(event))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("OWNER 역할 회원가입 이벤트 처리")
        void handleUserSignedUp_Owner() {
            UUID userId = UUID.randomUUID();
            UserSignedUpEvent event = new UserSignedUpEvent(userId, "owner@example.com", UserRole.OWNER);

            assertThatCode(() -> userEventListener.handleUserSignedUp(event))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("MANAGER 역할 회원가입 이벤트 처리")
        void handleUserSignedUp_Manager() {
            UUID userId = UUID.randomUUID();
            UserSignedUpEvent event = new UserSignedUpEvent(userId, "manager@example.com", UserRole.MANAGER);

            assertThatCode(() -> userEventListener.handleUserSignedUp(event))
                    .doesNotThrowAnyException();
        }
    }
}
