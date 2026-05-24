package com.groom.common.event.payload;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdatedPayload {
	private UUID userId;
	private String nickname;
	private String phoneNumber;
	private boolean passwordChanged;
	private Instant occurredAt;
}
