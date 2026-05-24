package com.groom.user.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserUpdateEvent {
	private final UUID userId;
	private final String nickname;
	private final String phoneNumber;
	private final boolean password;
	private final LocalDateTime occurredAt;
}
