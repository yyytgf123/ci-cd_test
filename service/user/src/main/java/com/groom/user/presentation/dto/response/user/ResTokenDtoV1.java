package com.groom.user.presentation.dto.response.user;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResTokenDtoV1 {

	private String accessToken;
	private String refreshToken;
	@Builder.Default
	private String tokenType = "Bearer";
	@Builder.Default
	private Integer expiresIn = 3600;

	public static ResTokenDtoV1 of(String accessToken, String refreshToken) {
		return ResTokenDtoV1.builder()
			.accessToken(accessToken)
			.refreshToken(refreshToken)
			.build();
	}
}
