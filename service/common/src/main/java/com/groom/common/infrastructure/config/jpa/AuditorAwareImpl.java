package com.groom.common.infrastructure.config.jpa;

import java.util.Optional;

import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

@Component
public class AuditorAwareImpl implements AuditorAware<String> { //user

	@Override
	public Optional<String> getCurrentAuditor() {
		// 나중에 Security 붙으면 여기서 로그인 유저 ID 꺼내면 됨
		return Optional.of("SYSTEM"); // 지금은 임시
	}
}
