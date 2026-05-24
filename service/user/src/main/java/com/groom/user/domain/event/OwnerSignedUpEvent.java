package com.groom.user.domain.event;

import java.util.UUID;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class OwnerSignedUpEvent {
	private final UUID userId;
	private final UUID ownerId;
	private final String email;
	private final String storeName;
}
