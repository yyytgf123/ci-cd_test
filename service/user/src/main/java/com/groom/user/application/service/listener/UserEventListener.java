package com.groom.user.application.service.listener;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.groom.user.domain.event.UserSignedUpEvent;
import com.groom.user.domain.event.UserUpdateEvent;
import com.groom.user.domain.event.UserWithdrawnEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventListener {

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleUserWithdrawn(UserWithdrawnEvent event) {
		log.info("User withdrawn event: userId={}", event.getUserId());
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleUserUpdated(UserUpdateEvent event) {
		log.info("User updated event: userId={}", event.getUserId());

		if (event.getUserId() != null) {
			log.info("");
		}

		if (event.getNickname() != null) {
			log.info("닉네임 변경: {}", event.getNickname());
		}

		if (event.getPhoneNumber() != null) {
			log.info("전화번호 변경: {}", event.getPhoneNumber());
		}

		if (event.isPassword()) {
			log.info("비밀번호 변경");
		}
	}

	@Async("eventExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleUserSignedUp(UserSignedUpEvent event) {
		log.info("User signed up: userId={}, email={}, role={}",
			event.getUserId(), event.getEmail(), event.getRole());

		// TODO: 환영 메일 발송
		// TODO: 가입 쿠폰 발급
		// TODO: 가입 포인트 지급
	}
}
