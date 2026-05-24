package com.groom.order.infrastructure.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAddressResponse {

	private String recipientName;
	private String recipientPhone;
	private String zipCode;
	private String address;
	private String detailAddress;
}
