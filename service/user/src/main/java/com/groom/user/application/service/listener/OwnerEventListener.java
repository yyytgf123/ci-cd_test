package com.groom.user.application.service.listener;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.groom.user.domain.event.OwnerSignedUpEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OwnerEventListener {

	@Async("eventExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleOwnerSignedUp(OwnerSignedUpEvent event) {
		log.info("Owner signed up: store={}, email={}",
			event.getStoreName(), event.getEmail());

		// TODO: 관리자에게 승인 요청 알림
		// TODO: Owner에게 가입 완료 + 승인 대기 안내 메일
	}
}

