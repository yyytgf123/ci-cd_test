package com.groom.user.presentation.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.groom.user.application.service.AuthServiceV1;
import com.groom.user.presentation.dto.request.user.ReqConfirmSignupDtoV1;
import com.groom.user.presentation.dto.request.user.ReqLoginDtoV1;
import com.groom.user.presentation.dto.request.user.ReqSignupDtoV1;
import com.groom.user.presentation.dto.response.user.ResTokenDtoV1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Auth", description = "인증 API (Cognito)")
@RestController
@RequestMapping("/api/v2/auth")
@RequiredArgsConstructor
public class AuthControllerV1 {

	private final AuthServiceV1 authService;

	@Operation(summary = "회원가입", description = "USER 또는 OWNER만 가입 가능. 이메일로 인증코드가 발송됩니다.")
	@PostMapping("/signup")
	public ResponseEntity<Void> signup(@Valid @RequestBody ReqSignupDtoV1 request) {
		authService.signup(request);
		return ResponseEntity.status(HttpStatus.CREATED).build();
	}

	@Operation(summary = "이메일 인증", description = "이메일로 받은 인증 코드를 확인합니다.")
	@PostMapping("/confirm")
	public ResponseEntity<Void> confirmSignup(@Valid @RequestBody ReqConfirmSignupDtoV1 request) {
		authService.confirmSignup(request);
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "인증 코드 재발송", description = "이메일 인증 코드를 다시 발송합니다.")
	@PostMapping("/resend-code")
	public ResponseEntity<Void> resendCode(@RequestParam String email) {
		authService.resendConfirmationCode(email);
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "로그인", description = "Cognito 토큰(accessToken, idToken, refreshToken)을 발급합니다.")
	@PostMapping("/login")
	public ResponseEntity<ResTokenDtoV1> login(@Valid @RequestBody ReqLoginDtoV1 request) {
		return ResponseEntity.ok(authService.login(request));
	}

	@Operation(summary = "토큰 갱신")
	@PostMapping("/refresh")
	public ResponseEntity<ResTokenDtoV1> refresh(
		@RequestParam String refreshToken,
		@RequestParam String email) {  // email 추가
		return ResponseEntity.ok(authService.refreshToken(refreshToken, email));
	}

	@Operation(summary = "로그아웃", description = "모든 토큰을 무효화합니다.")
	@PostMapping("/logout")
	public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authorization) {
		String accessToken = authorization.replace("Bearer ", "");
		authService.logout(accessToken);
		return ResponseEntity.ok().build();
	}
}
