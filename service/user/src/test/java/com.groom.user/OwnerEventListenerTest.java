package com.groom.user;

import static org.assertj.core.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.groom.user.application.service.listener.OwnerEventListener;
import com.groom.user.domain.event.OwnerSignedUpEvent;

@ExtendWith(MockitoExtension.class)
@DisplayName("OwnerEventListener 테스트")
class OwnerEventListenerTest {

    @InjectMocks
    private OwnerEventListener ownerEventListener;

    @Nested
    @DisplayName("handleOwnerSignedUp() 테스트")
    class HandleOwnerSignedUpTest {

        @Test
        @DisplayName("Owner 회원가입 이벤트 처리 성공")
        void handleOwnerSignedUp_Success() {
            UUID userId = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID();
            OwnerSignedUpEvent event = new OwnerSignedUpEvent(
                    userId,
                    ownerId,
                    "owner@example.com",
                    "테스트 스토어"
            );

            assertThatCode(() -> ownerEventListener.handleOwnerSignedUp(event))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("다양한 스토어명으로 이벤트 처리")
        void handleOwnerSignedUp_VariousStoreNames() {
            UUID userId = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID();

            // 한글 스토어명
            OwnerSignedUpEvent event1 = new OwnerSignedUpEvent(userId, ownerId, "test1@example.com", "맛있는 치킨집");
            assertThatCode(() -> ownerEventListener.handleOwnerSignedUp(event1)).doesNotThrowAnyException();

            // 영문 스토어명
            OwnerSignedUpEvent event2 = new OwnerSignedUpEvent(userId, ownerId, "test2@example.com", "Delicious Pizza");
            assertThatCode(() -> ownerEventListener.handleOwnerSignedUp(event2)).doesNotThrowAnyException();

            // 특수문자 포함 스토어명
            OwnerSignedUpEvent event3 = new OwnerSignedUpEvent(userId, ownerId, "test3@example.com", "카페 & 베이커리");
            assertThatCode(() -> ownerEventListener.handleOwnerSignedUp(event3)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("빈 스토어명으로 이벤트 처리")
        void handleOwnerSignedUp_EmptyStoreName() {
            UUID userId = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID();
            OwnerSignedUpEvent event = new OwnerSignedUpEvent(userId, ownerId, "owner@example.com", "");

            assertThatCode(() -> ownerEventListener.handleOwnerSignedUp(event))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("null 스토어명으로 이벤트 처리")
        void handleOwnerSignedUp_NullStoreName() {
            UUID userId = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID();
            OwnerSignedUpEvent event = new OwnerSignedUpEvent(userId, ownerId, "owner@example.com", null);

            assertThatCode(() -> ownerEventListener.handleOwnerSignedUp(event))
                    .doesNotThrowAnyException();
        }
    }
}
