package com.groom.user.presentation.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.groom.user.application.service.UserServiceV1;
import com.groom.user.domain.entity.user.PeriodType;
import com.groom.user.presentation.dto.request.user.ReqUpdateUserDtoV1;
import com.groom.user.presentation.dto.response.owner.ResSalesStatDtoV1;
import com.groom.user.presentation.dto.response.user.ResUserDtoV1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "User", description = "사용자 API")
@RestController
@RequestMapping("/api/v2/users/me")
@RequiredArgsConstructor
public class UserControllerV1 {

	private final UserServiceV1 userService;

	@Operation(summary = "내 정보 조회")
	@GetMapping
	public ResponseEntity<ResUserDtoV1> getMe() {
		return ResponseEntity.ok(userService.getMe());
	}

	@Operation(summary = "내 정보 수정")
	@PatchMapping
	public ResponseEntity<Void> updateMe(
			@Valid @RequestBody ReqUpdateUserDtoV1 request) {
		userService.updateMe(request);
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "회원 탈퇴 (Soft Delete)")
	@DeleteMapping
	public ResponseEntity<Void> deleteMe() {
		userService.deleteMe();
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "매출 통계 조회 (OWNER only)")
	@GetMapping("/sales")
	public ResponseEntity<List<ResSalesStatDtoV1>> getSalesStats(
			@RequestParam PeriodType periodType,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		return ResponseEntity.ok(userService.getSalesStats(periodType, date));
	}
}
