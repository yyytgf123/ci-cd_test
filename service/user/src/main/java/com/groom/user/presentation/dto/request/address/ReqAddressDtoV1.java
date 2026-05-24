package com.groom.user.presentation.dto.request.address;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReqAddressDtoV1 {

	@NotBlank(message = "우편번호는 필수입니다.")
	private String zipCode;

	@NotBlank(message = "주소는 필수입니다.")
	private String address;

	@NotBlank(message = "상세주소는 필수입니다.")
	private String detailAddress;

	// 수령인 정보
	@NotBlank(message = "수령인 이름은 필수입니다.")
	private String recipient;

	// 수령인 연락처
	@NotBlank(message = "수령인 연락처는 필수입니다.")
	private String recipientPhone;

	private Boolean isDefault;
}
