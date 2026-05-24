package com.groom.user.presentation.dto.response.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAddressInternalResponse {

	private String recipientName;
	private String recipientPhone;
	private String zipCode;
	private String address;
	private String detailAddress;
}