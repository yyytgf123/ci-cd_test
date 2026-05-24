package com.groom.user.presentation.dto.request.user;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReqUpdateUserDtoV1 {

	private String nickname;
	private String phoneNumber;
	private String password;
	private String address;
}
