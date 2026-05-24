package com.groom.user.domain.event;

import java.util.UUID;

import com.groom.common.enums.UserRole;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UserSignedUpEvent {
	private final UUID userId;
	private final String email;
	private final UserRole role;
}
