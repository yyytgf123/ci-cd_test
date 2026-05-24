package com.groom.user.domain.event;

import java.util.UUID;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UserWithdrawnEvent {
	private final UUID userId;
}
