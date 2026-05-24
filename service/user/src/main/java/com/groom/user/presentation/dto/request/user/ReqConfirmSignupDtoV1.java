package com.groom.user.presentation.dto.request.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReqConfirmSignupDtoV1 {

	@NotBlank(message = "이메일은 필수입니다.")
	@Email
	private String email;

	@NotBlank(message = "인증 코드는 필수입니다.")
	private String code;
}
