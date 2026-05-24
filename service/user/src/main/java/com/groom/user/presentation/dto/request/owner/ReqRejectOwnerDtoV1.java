package com.groom.user.presentation.dto.request.owner;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReqRejectOwnerDtoV1 {

	@NotBlank(message = "거절 사유는 필수입니다.")
	private String rejectedReason;
}
