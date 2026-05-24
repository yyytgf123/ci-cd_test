// package com.groom.user;
//
// import static org.mockito.ArgumentMatchers.*;
// import static org.mockito.BDDMockito.*;
// import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
// import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
// import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
// import java.util.List;
// import java.util.UUID;
//
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Nested;
// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
// import org.springframework.boot.test.mock.mockito.MockBean;
// import org.springframework.data.domain.Pageable;
// import org.springframework.http.MediaType;
// import org.springframework.security.test.context.support.WithMockUser;
// import org.springframework.test.web.servlet.MockMvc;
//
// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.groom.common.enums.UserRole;
// import com.groom.common.infrastructure.config.security.JwtUtil;
// import com.groom.user.application.service.AdminServiceV1;
// import com.groom.user.domain.entity.owner.OwnerStatus;
// import com.groom.user.domain.entity.user.UserStatus;
// import com.groom.user.presentation.controller.AdminControllerV1;
// import com.groom.user.presentation.dto.request.admin.ReqCreateManagerDtoV1;
// import com.groom.user.presentation.dto.request.owner.ReqRejectOwnerDtoV1;
// import com.groom.user.presentation.dto.response.admin.ResOwnerApprovalListDtoV1;
// import com.groom.user.presentation.dto.response.owner.ResOwnerApprovalDtoV1;
// import com.groom.user.presentation.dto.response.user.ResUserDtoV1;
// import com.groom.user.presentation.dto.response.user.ResUserListDtoV1;
//
// @WebMvcTest(AdminControllerV1.class)
// @DisplayName("AdminControllerV1 테스트")
// class AdminControllerV1Test {
//
//     @Autowired
//     private MockMvc mockMvc;
//
//     @Autowired
//     private ObjectMapper objectMapper;
//
//     // ✅ jwtAuthenticationFilter 의존성 충족용
//     @MockBean
//     private JwtUtil jwtUtil;
//
//     @MockBean
//     private AdminServiceV1 adminService;
//
//     private UUID userId;
//     private UUID ownerId;
//     private ResUserDtoV1 userResponse;
//     private ResOwnerApprovalDtoV1 ownerResponse;
//
//     @BeforeEach
//     void setUp() {
//         userId = UUID.randomUUID();
//         ownerId = UUID.randomUUID();
//
//         userResponse = ResUserDtoV1.builder()
//             .id(userId)
//             .email("user@example.com")
//             .nickname("testUser")
//             .phoneNumber("010-1234-5678")
//             .role(UserRole.USER)
//             .status(UserStatus.ACTIVE)
//             .build();
//
//         ownerResponse = ResOwnerApprovalDtoV1.builder()
//             .ownerId(ownerId)
//             .userId(userId)
//             .email("owner@example.com")
//             .nickname("ownerUser")
//             .storeName("테스트 스토어")
//             .ownerStatus(OwnerStatus.PENDING)
//             .build();
//     }
//
//     @Nested
//     @DisplayName("GET /api/v1/admin/users")
//     class GetUserListTest {
//
//         @Test
//         @WithMockUser(roles = "MANAGER")
//         @DisplayName("회원 목록 조회 성공 - MANAGER")
//         void getUserList_Manager_Success() throws Exception {
//             ResUserListDtoV1 response = ResUserListDtoV1.builder()
//                 .users(List.of(userResponse))
//                 .page(0)
//                 .size(20)
//                 .totalElements(1)
//                 .totalPages(1)
//                 .build();
//
//             given(adminService.getUserList(any(Pageable.class))).willReturn(response);
//
//             mockMvc.perform(get("/api/v1/admin/users"))
//                 .andDo(print())
//                 .andExpect(status().isOk())
//                 .andExpect(jsonPath("$.users[0].email").value("user@example.com"))
//                 .andExpect(jsonPath("$.totalElements").value(1));
//         }
//
//         @Test
//         @WithMockUser(roles = "MASTER")
//         @DisplayName("회원 목록 조회 성공 - MASTER")
//         void getUserList_Master_Success() throws Exception {
//             ResUserListDtoV1 response = ResUserListDtoV1.builder()
//                 .users(List.of())
//                 .page(0)
//                 .size(20)
//                 .totalElements(0)
//                 .totalPages(0)
//                 .build();
//
//             given(adminService.getUserList(any(Pageable.class))).willReturn(response);
//
//             mockMvc.perform(get("/api/v1/admin/users"))
//                 .andExpect(status().isOk());
//         }
//
//         @Test
//         @WithMockUser(roles = "USER")
//         @DisplayName("일반 사용자 접근 시 403")
//         void getUserList_User_Forbidden() throws Exception {
//             mockMvc.perform(get("/api/v1/admin/users"))
//                 .andExpect(status().isForbidden());
//         }
//     }
//
//     @Nested
//     @DisplayName("POST /api/v1/admin/users/{userId}/ban")
//     class BanUserTest {
//
//         @Test
//         @WithMockUser(roles = "MANAGER")
//         @DisplayName("회원 제재 성공")
//         void banUser_Success() throws Exception {
//             willDoNothing().given(adminService).banUser(any(UUID.class));
//
//             mockMvc.perform(post("/api/v1/admin/users/{userId}/ban", userId))
//                 .andDo(print())
//                 .andExpect(status().isOk());
//
//             verify(adminService).banUser(userId);
//         }
//     }
//
//     @Nested
//     @DisplayName("POST /api/v1/admin/users/{userId}/unban")
//     class UnbanUserTest {
//
//         @Test
//         @WithMockUser(roles = "MANAGER")
//         @DisplayName("회원 제재 해제 성공")
//         void unbanUser_Success() throws Exception {
//             willDoNothing().given(adminService).unbanUser(any(UUID.class));
//
//             mockMvc.perform(post("/api/v1/admin/users/{userId}/unban", userId))
//                 .andDo(print())
//                 .andExpect(status().isOk());
//
//             verify(adminService).unbanUser(userId);
//         }
//     }
//
//     @Nested
//     @DisplayName("POST /api/v1/admin/managers")
//     class CreateManagerTest {
//
//         @Test
//         @WithMockUser(roles = "MASTER")
//         @DisplayName("Manager 계정 생성 성공")
//         void createManager_Success() throws Exception {
//             ReqCreateManagerDtoV1 request = new ReqCreateManagerDtoV1();
//             request.setEmail("newmanager@example.com");
//             request.setPassword("password123");
//             request.setNickname("newManager");
//             request.setPhoneNumber("010-9999-8888");
//
//             ResUserDtoV1 managerResponse = ResUserDtoV1.builder()
//                 .id(UUID.randomUUID())
//                 .email("newmanager@example.com")
//                 .nickname("newManager")
//                 .role(UserRole.MANAGER)
//                 .status(UserStatus.ACTIVE)
//                 .build();
//
//             given(adminService.createManager(any(ReqCreateManagerDtoV1.class))).willReturn(managerResponse);
//
//             mockMvc.perform(post("/api/v1/admin/managers")
//                     .contentType(MediaType.APPLICATION_JSON)
//                     .content(objectMapper.writeValueAsString(request)))
//                 .andDo(print())
//                 .andExpect(status().isCreated())
//                 .andExpect(jsonPath("$.email").value("newmanager@example.com"))
//                 .andExpect(jsonPath("$.role").value("MANAGER"));
//         }
//
//         @Test
//         @WithMockUser(roles = "MANAGER")
//         @DisplayName("MANAGER가 Manager 생성 시도 시 403")
//         void createManager_ManagerRole_Forbidden() throws Exception {
//             ReqCreateManagerDtoV1 request = new ReqCreateManagerDtoV1();
//             request.setEmail("test@example.com");
//             request.setPassword("password123");
//             request.setNickname("test");
//             request.setPhoneNumber("010-1234-5678");
//
//             mockMvc.perform(post("/api/v1/admin/managers")
//                     .contentType(MediaType.APPLICATION_JSON)
//                     .content(objectMapper.writeValueAsString(request)))
//                 .andExpect(status().isForbidden());
//         }
//     }
//
//     @Nested
//     @DisplayName("DELETE /api/v1/admin/managers/{managerId}")
//     class DeleteManagerTest {
//
//         @Test
//         @WithMockUser(roles = "MASTER")
//         @DisplayName("Manager 계정 삭제 성공")
//         void deleteManager_Success() throws Exception {
//             UUID managerId = UUID.randomUUID();
//             willDoNothing().given(adminService).deleteManager(any(UUID.class));
//
//             mockMvc.perform(delete("/api/v1/admin/managers/{managerId}", managerId))
//                 .andDo(print())
//                 .andExpect(status().isNoContent());
//
//             verify(adminService).deleteManager(managerId);
//         }
//     }
//
//     @Nested
//     @DisplayName("GET /api/v1/admin/managers")
//     class GetManagerListTest {
//
//         @Test
//         @WithMockUser(roles = "MASTER")
//         @DisplayName("Manager 목록 조회 성공")
//         void getManagerList_Success() throws Exception {
//             ResUserListDtoV1 response = ResUserListDtoV1.builder()
//                 .users(List.of())
//                 .page(0)
//                 .size(20)
//                 .totalElements(0)
//                 .totalPages(0)
//                 .build();
//
//             given(adminService.getManagerList(any(Pageable.class))).willReturn(response);
//
//             mockMvc.perform(get("/api/v1/admin/managers"))
//                 .andExpect(status().isOk());
//         }
//     }
//
//     @Nested
//     @DisplayName("GET /api/v1/admin/owners/pending")
//     class GetPendingOwnerListTest {
//
//         @Test
//         @WithMockUser(roles = "MANAGER")
//         @DisplayName("승인 대기 Owner 목록 조회 성공")
//         void getPendingOwnerList_Success() throws Exception {
//             ResOwnerApprovalListDtoV1 response = ResOwnerApprovalListDtoV1.builder()
//                 .content(List.of(ownerResponse))
//                 .page(0)
//                 .size(20)
//                 .totalElements(1)
//                 .totalPages(1)
//                 .first(true)
//                 .last(true)
//                 .build();
//
//             given(adminService.getPendingOwnerList(any(Pageable.class))).willReturn(response);
//
//             mockMvc.perform(get("/api/v1/admin/owners/pending"))
//                 .andDo(print())
//                 .andExpect(status().isOk())
//                 .andExpect(jsonPath("$.content[0].storeName").value("테스트 스토어"));
//         }
//     }
//
//     @Nested
//     @DisplayName("GET /api/v1/admin/owners")
//     class GetOwnerListByStatusTest {
//
//         @Test
//         @WithMockUser(roles = "MANAGER")
//         @DisplayName("상태별 Owner 목록 조회 성공")
//         void getOwnerListByStatus_Success() throws Exception {
//             ResOwnerApprovalListDtoV1 response = ResOwnerApprovalListDtoV1.builder()
//                 .content(List.of())
//                 .page(0)
//                 .size(20)
//                 .totalElements(0)
//                 .totalPages(0)
//                 .first(true)
//                 .last(true)
//                 .build();
//
//             given(adminService.getOwnerListByStatus(any(OwnerStatus.class), any(Pageable.class)))
//                 .willReturn(response);
//
//             mockMvc.perform(get("/api/v1/admin/owners")
//                     .param("status", "APPROVED"))
//                 .andExpect(status().isOk());
//         }
//     }
//
//     @Nested
//     @DisplayName("GET /api/v1/admin/owners/{ownerId}")
//     class GetOwnerApprovalDetailTest {
//
//         @Test
//         @WithMockUser(roles = "MANAGER")
//         @DisplayName("Owner 상세 조회 성공")
//         void getOwnerApprovalDetail_Success() throws Exception {
//             given(adminService.getOwnerApprovalDetail(any(UUID.class))).willReturn(ownerResponse);
//
//             mockMvc.perform(get("/api/v1/admin/owners/{ownerId}", ownerId))
//                 .andDo(print())
//                 .andExpect(status().isOk())
//                 .andExpect(jsonPath("$.storeName").value("테스트 스토어"));
//         }
//     }
//
//     @Nested
//     @DisplayName("POST /api/v1/admin/owners/{ownerId}/approve")
//     class ApproveOwnerTest {
//
//         @Test
//         @WithMockUser(roles = "MANAGER")
//         @DisplayName("Owner 승인 성공")
//         void approveOwner_Success() throws Exception {
//             ResOwnerApprovalDtoV1 approvedResponse = ResOwnerApprovalDtoV1.builder()
//                 .ownerId(ownerId)
//                 .storeName("테스트 스토어")
//                 .ownerStatus(OwnerStatus.APPROVED)
//                 .build();
//
//             given(adminService.approveOwner(any(UUID.class))).willReturn(approvedResponse);
//
//             mockMvc.perform(post("/api/v1/admin/owners/{ownerId}/approve", ownerId))
//                 .andDo(print())
//                 .andExpect(status().isOk())
//                 .andExpect(jsonPath("$.ownerStatus").value("APPROVED"));
//         }
//     }
//
//     @Nested
//     @DisplayName("POST /api/v1/admin/owners/{ownerId}/reject")
//     class RejectOwnerTest {
//
//         @Test
//         @WithMockUser(roles = "MANAGER")
//         @DisplayName("Owner 거절 성공")
//         void rejectOwner_Success() throws Exception {
//             ReqRejectOwnerDtoV1 request = new ReqRejectOwnerDtoV1();
//             request.setRejectedReason("서류 미비");
//
//             ResOwnerApprovalDtoV1 rejectedResponse = ResOwnerApprovalDtoV1.builder()
//                 .ownerId(ownerId)
//                 .storeName("테스트 스토어")
//                 .ownerStatus(OwnerStatus.REJECTED)
//                 .rejectedReason("서류 미비")
//                 .build();
//
//             given(adminService.rejectOwner(any(UUID.class), anyString())).willReturn(rejectedResponse);
//
//             mockMvc.perform(post("/api/v1/admin/owners/{ownerId}/reject", ownerId)
//                     .contentType(MediaType.APPLICATION_JSON)
//                     .content(objectMapper.writeValueAsString(request)))
//                 .andDo(print())
//                 .andExpect(status().isOk())
//                 .andExpect(jsonPath("$.ownerStatus").value("REJECTED"))
//                 .andExpect(jsonPath("$.rejectedReason").value("서류 미비"));
//         }
//
//         @Test
//         @WithMockUser(roles = "MANAGER")
//         @DisplayName("거절 사유 없이 거절 시도 시 400")
//         void rejectOwner_NoReason_BadRequest() throws Exception {
//             ReqRejectOwnerDtoV1 request = new ReqRejectOwnerDtoV1();
//
//             mockMvc.perform(post("/api/v1/admin/owners/{ownerId}/reject", ownerId)
//                     .contentType(MediaType.APPLICATION_JSON)
//                     .content(objectMapper.writeValueAsString(request)))
//                 .andExpect(status().isBadRequest());
//         }
//     }
// }
