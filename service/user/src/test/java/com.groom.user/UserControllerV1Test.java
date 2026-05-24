package com.groom.user;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groom.common.enums.UserRole;
import com.groom.user.application.service.UserServiceV1;
import com.groom.user.domain.entity.user.PeriodType;
import com.groom.user.domain.entity.user.UserStatus;
import com.groom.user.presentation.controller.UserControllerV1;
import com.groom.user.presentation.dto.request.user.ReqUpdateUserDtoV1;
import com.groom.user.presentation.dto.response.owner.ResSalesStatDtoV1;
import com.groom.user.presentation.dto.response.user.ResUserDtoV1;

@WebMvcTest(UserControllerV1.class)
@AutoConfigureMockMvc(addFilters = false)
@org.springframework.test.context.TestPropertySource(properties = {
    "aws.cognito.jwk-set-uri=https://mock.test",
    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://mock.test",
    "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://mock.test"
})
@DisplayName("UserControllerV1 테스트")
class UserControllerV1Test {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // SecurityConfig의 JwtDecoder 빈 생성 시 실제 Cognito 연결 방지
    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private UserServiceV1 userService;

    private UUID userId;
    private ResUserDtoV1 userResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        userResponse = ResUserDtoV1.builder()
            .id(userId)
            .email("test@example.com")
            .nickname("testUser")
            .phoneNumber("010-1234-5678")
            .role(UserRole.USER)
            .status(UserStatus.ACTIVE)
            .build();
    }

    @Nested
    @DisplayName("GET /api/v2/users/me")
    class GetMeTest {

        @Test
        @WithMockUser
        @DisplayName("내 정보 조회 성공")
        void getMe_Success() throws Exception {
            given(userService.getMe()).willReturn(userResponse);

            mockMvc.perform(get("/api/v2/users/me"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.nickname").value("testUser"));
        }
    }

    @Nested
    @DisplayName("PATCH /api/v2/users/me")
    class UpdateMeTest {

        @Test
        @WithMockUser
        @DisplayName("내 정보 수정 성공")
        void updateMe_Success() throws Exception {
            ReqUpdateUserDtoV1 request = new ReqUpdateUserDtoV1();
            request.setNickname("newNickname");
            request.setPhoneNumber("010-9999-8888");

            willDoNothing().given(userService).updateMe(any(ReqUpdateUserDtoV1.class));

            mockMvc.perform(patch("/api/v2/users/me")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk());

            verify(userService).updateMe(any(ReqUpdateUserDtoV1.class));
        }

        @Test
        @WithMockUser
        @DisplayName("빈 요청으로 수정")
        void updateMe_EmptyRequest() throws Exception {
            ReqUpdateUserDtoV1 request = new ReqUpdateUserDtoV1();

            willDoNothing().given(userService).updateMe(any(ReqUpdateUserDtoV1.class));

            mockMvc.perform(patch("/api/v2/users/me")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v2/users/me")
    class DeleteMeTest {

        @Test
        @WithMockUser
        @DisplayName("회원 탈퇴 성공")
        void deleteMe_Success() throws Exception {
            willDoNothing().given(userService).deleteMe();

            mockMvc.perform(delete("/api/v2/users/me"))
                .andDo(print())
                .andExpect(status().isNoContent());

            verify(userService).deleteMe();
        }
    }

    @Nested
    @DisplayName("GET /api/v2/users/me/sales")
    class GetSalesStatsTest {

        @Test
        @WithMockUser
        @DisplayName("매출 통계 조회 성공")
        void getSalesStats_Success() throws Exception {
            LocalDate date = LocalDate.of(2025, 1, 15);
            List<ResSalesStatDtoV1> salesStats = List.of(
                ResSalesStatDtoV1.of(date, 100000L)
            );

            given(userService.getSalesStats(eq(PeriodType.DAILY), any(LocalDate.class)))
                .willReturn(salesStats);

            mockMvc.perform(get("/api/v2/users/me/sales")
                    .param("periodType", "DAILY")
                    .param("date", "2025-01-15"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].date").value("2025-01-15"))
                .andExpect(jsonPath("$[0].totalAmount").value(100000));
        }

        @Test
        @WithMockUser
        @DisplayName("날짜 없이 매출 통계 조회")
        void getSalesStats_NoDate() throws Exception {
            List<ResSalesStatDtoV1> salesStats = List.of(
                ResSalesStatDtoV1.of(LocalDate.now(), 50000L)
            );

            given(userService.getSalesStats(eq(PeriodType.MONTHLY), isNull()))
                .willReturn(salesStats);

            mockMvc.perform(get("/api/v2/users/me/sales")
                    .param("periodType", "MONTHLY"))
                .andExpect(status().isOk());
        }
    }
}
