package com.groom.user.presentation.controller;

import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.groom.user.application.service.AdminServiceV1;
import com.groom.user.application.service.AuthServiceV1;
import com.groom.user.domain.entity.owner.OwnerStatus;
import com.groom.user.presentation.dto.request.admin.ReqCreateManagerDtoV1;
import com.groom.user.presentation.dto.request.owner.ReqRejectOwnerDtoV1;
import com.groom.user.presentation.dto.request.user.ReqConfirmSignupDtoV1;
import com.groom.user.presentation.dto.response.admin.ResOwnerApprovalListDtoV1;
import com.groom.user.presentation.dto.response.owner.ResOwnerApprovalDtoV1;
import com.groom.user.presentation.dto.response.user.ResTokenDtoV1;
import com.groom.user.presentation.dto.response.user.ResUserDtoV1;
import com.groom.user.presentation.dto.response.user.ResUserListDtoV1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Admin", description = "관리자 API")
@RestController
@RequestMapping("/api/v2/admin")
@RequiredArgsConstructor
public class AdminControllerV1 {

	private final AdminServiceV1 adminService;
	private final AuthServiceV1 authServiceV1;

	// ==================== Manager 전용 ====================

	@Operation(summary = "회원 목록 조회 (Manager)")
	// @PreAuthorize : 아래 경로로 들어오는 트래픽은 Spring Security가 검사
	@PreAuthorize("hasAnyRole('MANAGER', 'MASTER')")
	@GetMapping("/users")
	public ResponseEntity<ResUserListDtoV1> getUserList(@PageableDefault(size = 20) Pageable pageable) {
		return ResponseEntity.ok(adminService.getUserList(pageable));
	}

	@Operation(summary = "회원 제재 (Manager)")
	@PreAuthorize("hasAnyRole('MANAGER', 'MASTER')")
	@PostMapping("/users/{userId}/ban")
	public ResponseEntity<Void> banUser(@PathVariable UUID userId) {
		adminService.banUser(userId);
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "회원 제재 해제 (Manager)")
	@PreAuthorize("hasAnyRole('MANAGER', 'MASTER')")
	@PostMapping("/users/{userId}/unban")
	public ResponseEntity<Void> unbanUser(@PathVariable UUID userId) {
		adminService.unbanUser(userId);
		return ResponseEntity.ok().build();
	}

	// ==================== Master 전용 ====================

	@Operation(summary = "Manager 계정 생성 (Master only)")
	@PreAuthorize("hasRole('MASTER')")
	@PostMapping("/managers")
	public ResponseEntity<ResUserDtoV1> createManager(@Valid @RequestBody ReqCreateManagerDtoV1 request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createManager(request));
	}

	@Operation(summary = "Manager 계정 삭제 (Master only)")
	@PreAuthorize("hasRole('MASTER')")
	@DeleteMapping("/managers/{managerId}")
	public ResponseEntity<Void> deleteManager(@PathVariable UUID managerId) {
		adminService.deleteManager(managerId);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "Manager 목록 조회 (Master only)")
	@PreAuthorize("hasRole('MASTER')")
	@GetMapping("/managers")
	public ResponseEntity<ResUserListDtoV1> getManagerList(@PageableDefault(size = 20) Pageable pageable) {
		return ResponseEntity.ok(adminService.getManagerList(pageable));
	}

	// ==================== Seller 승인 관리 ====================

	@Operation(summary = "승인 대기 중인 Owner 목록 조회 (Manager)")
	@PreAuthorize("hasAnyRole('MANAGER', 'MASTER')")
	@GetMapping("/owners/pending")
	public ResponseEntity<ResOwnerApprovalListDtoV1> getPendingOwnerList(
			@PageableDefault(size = 20) Pageable pageable) {
		return ResponseEntity.ok(adminService.getPendingOwnerList(pageable));
	}

	@Operation(summary = "상태별 Owner 목록 조회 (Manager)")
	@PreAuthorize("hasAnyRole('MANAGER', 'MASTER')")
	@GetMapping("/owners")
	public ResponseEntity<ResOwnerApprovalListDtoV1> getOwnerListByStatus(
			@RequestParam(defaultValue = "PENDING") OwnerStatus status,
			@PageableDefault(size = 20) Pageable pageable) {
		return ResponseEntity.ok(adminService.getOwnerListByStatus(status, pageable));
	}

	@Operation(summary = "Owner 승인 요청 상세 조회 (Manager)")
	@PreAuthorize("hasAnyRole('MANAGER', 'MASTER')")
	@GetMapping("/owners/{ownerId}")
	public ResponseEntity<ResOwnerApprovalDtoV1> getOwnerApprovalDetail(@PathVariable UUID ownerId) {
		return ResponseEntity.ok(adminService.getOwnerApprovalDetail(ownerId));
	}

	@Operation(summary = "Owner 승인 (Manager)")
	@PreAuthorize("hasAnyRole('MANAGER', 'MASTER')")
	@PostMapping("/owners/{ownerId}/approve")
	public ResponseEntity<ResOwnerApprovalDtoV1> approveOwner(@PathVariable UUID ownerId) {
		return ResponseEntity.ok(adminService.approveOwner(ownerId));
	}

	@Operation(summary = "Owner 승인 거절 (Manager)")
	@PreAuthorize("hasAnyRole('MANAGER', 'MASTER')")
	@PostMapping("/owners/{ownerId}/reject")
	public ResponseEntity<ResOwnerApprovalDtoV1> rejectOwner(
			@PathVariable UUID ownerId,
			@Valid @RequestBody ReqRejectOwnerDtoV1 request) {
		return ResponseEntity.ok(adminService.rejectOwner(ownerId, request.getRejectedReason()));
	}
}
