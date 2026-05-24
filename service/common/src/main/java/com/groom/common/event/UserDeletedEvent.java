package com.groom.common.event;

import java.util.UUID;

/**
 * 유저 탈퇴 이벤트
 */
public record UserDeletedEvent(
    UUID userId
) {}
