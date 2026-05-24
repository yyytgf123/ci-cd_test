// package com.groom.user;
//
// import static org.mockito.ArgumentMatchers.*;
// import static org.mockito.BDDMockito.*;
// import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
// import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
// import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Nested;
// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
// import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
// import org.springframework.boot.test.mock.mockito.MockBean;
// import org.springframework.http.MediaType;
// import org.springframework.security.test.context.support.WithMockUser;
// import org.springframework.test.web.servlet.MockMvc;
//
// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.groom.common.enums.UserRole;
// import com.groom.common.infrastructure.config.security.JwtUtil;
// import com.groom.user.application.service.AuthServiceV1;
// import com.groom.user.presentation.controller.AuthControllerV1;
// import com.groom.user.presentation.dto.request.user.ReqLoginDtoV1;
// import com.groom.user.presentation.dto.request.user.ReqSignupDtoV1;
// import com.groom.user.presentation.dto.response.user.ResTokenDtoV1;
//
// @WebMvcTest(AuthControllerV1.class)
// @DisplayName("AuthControllerV1 테스트")
// @AutoConfigureMockMvc(addFilters = false)
// class AuthControllerV1Test {
//
//     @Autowired
//     private MockMvc mockMvc;
//
//     @MockBean
//     JwtUtil jwtUtil;
//
//     @Autowired
//     private ObjectMapper objectMapper;
//
//     @MockBean
//     private AuthServiceV1 authService;
//
//     @Nested
//     @DisplayName("POST /api/v1/auth/signup")
//     class SignupTest {
//
//         @Test
//         @DisplayName("USER 회원가입 성공")
//         void signup_User_Success() throws Exception {
//             ReqSignupDtoV1 request = new ReqSignupDtoV1();
//             request.setEmail("newuser@example.com");
//             request.setPassword("password123");
//             request.setNickname("newUser");
//             request.setPhoneNumber("010-1234-5678");
//             request.setRole(UserRole.USER);
//
//             willDoNothing().given(authService).signup(any(ReqSignupDtoV1.class));
//
//             mockMvc.perform(post("/api/v1/auth/signup")
//                             .contentType(MediaType.APPLICATION_JSON)
//                             .content(objectMapper.writeValueAsString(request)))
//                     .andDo(print())
//                     .andExpect(status().isCreated());
//
//             verify(authService).signup(any(ReqSignupDtoV1.class));
//         }
//
//         @Test
//         @DisplayName("OWNER 회원가입 성공")
//         void signup_Owner_Success() throws Exception {
//             ReqSignupDtoV1 request = new ReqSignupDtoV1();
//             request.setEmail("owner@example.com");
//             request.setPassword("password123");
//             request.setNickname("ownerUser");
//             request.setPhoneNumber("010-9876-5432");
//             request.setRole(UserRole.OWNER);
//             request.setStore("테스트 스토어");
//             request.setZipCode("12345");
//             request.setAddress("서울시 강남구");
//             request.setDetailAddress("테헤란로 123");
//
//             willDoNothing().given(authService).signup(any(ReqSignupDtoV1.class));
//
//             mockMvc.perform(post("/api/v1/auth/signup")
//                             .contentType(MediaType.APPLICATION_JSON)
//                             .content(objectMapper.writeValueAsString(request)))
//                     .andExpect(status().isCreated());
//         }
//
//         @Test
//         @DisplayName("필수 필드 누락 시 400")
//         void signup_MissingRequired_BadRequest() throws Exception {
//             ReqSignupDtoV1 request = new ReqSignupDtoV1();
//             request.setEmail("test@example.com");
//             // password, nickname, phoneNumber, role 누락
//
//             mockMvc.perform(post("/api/v1/auth/signup")
//                             .contentType(MediaType.APPLICATION_JSON)
//                             .content(objectMapper.writeValueAsString(request)))
//                     .andExpect(status().isBadRequest());
//         }
//
//         @Test
//         @DisplayName("잘못된 이메일 형식 시 400")
//         void signup_InvalidEmail_BadRequest() throws Exception {
//             ReqSignupDtoV1 request = new ReqSignupDtoV1();
//             request.setEmail("invalid-email");
//             request.setPassword("password123");
//             request.setNickname("testUser");
//             request.setPhoneNumber("010-1234-5678");
//             request.setRole(UserRole.USER);
//
//             mockMvc.perform(post("/api/v1/auth/signup")
//                             .contentType(MediaType.APPLICATION_JSON)
//                             .content(objectMapper.writeValueAsString(request)))
//                     .andExpect(status().isBadRequest());
//         }
//
//         @Test
//         @DisplayName("짧은 비밀번호 시 400")
//         void signup_ShortPassword_BadRequest() throws Exception {
//             ReqSignupDtoV1 request = new ReqSignupDtoV1();
//             request.setEmail("test@example.com");
//             request.setPassword("short");  // 8자 미만
//             request.setNickname("testUser");
//             request.setPhoneNumber("010-1234-5678");
//             request.setRole(UserRole.USER);
//
//             mockMvc.perform(post("/api/v1/auth/signup")
//                             .contentType(MediaType.APPLICATION_JSON)
//                             .content(objectMapper.writeValueAsString(request)))
//                     .andExpect(status().isBadRequest());
//         }
//     }
//
//     @Nested
//     @DisplayName("POST /api/v1/auth/login")
//     class LoginTest {
//
//         @Test
//         @DisplayName("로그인 성공")
//         void login_Success() throws Exception {
//             ReqLoginDtoV1 request = new ReqLoginDtoV1();
//             request.setEmail("test@example.com");
//             request.setPassword("password123");
//
//             ResTokenDtoV1 response = ResTokenDtoV1.of("accessToken123", "refreshToken123");
//
//             given(authService.login(any(ReqLoginDtoV1.class))).willReturn(response);
//
//             mockMvc.perform(post("/api/v1/auth/login")
//                             .contentType(MediaType.APPLICATION_JSON)
//                             .content(objectMapper.writeValueAsString(request)))
//                     .andDo(print())
//                     .andExpect(status().isOk())
//                     .andExpect(jsonPath("$.accessToken").value("accessToken123"))
//                     .andExpect(jsonPath("$.refreshToken").value("refreshToken123"))
//                     .andExpect(jsonPath("$.tokenType").value("Bearer"));
//         }
//
//         @Test
//         @DisplayName("필수 필드 누락 시 400")
//         void login_MissingRequired_BadRequest() throws Exception {
//             ReqLoginDtoV1 request = new ReqLoginDtoV1();
//             request.setEmail("test@example.com");
//             // password 누락
//
//             mockMvc.perform(post("/api/v1/auth/login")
//                             .contentType(MediaType.APPLICATION_JSON)
//                             .content(objectMapper.writeValueAsString(request)))
//                     .andExpect(status().isBadRequest());
//         }
//
//         @Test
//         @DisplayName("잘못된 이메일 형식 시 400")
//         void login_InvalidEmail_BadRequest() throws Exception {
//             ReqLoginDtoV1 request = new ReqLoginDtoV1();
//             request.setEmail("invalid-email");
//             request.setPassword("password123");
//
//             mockMvc.perform(post("/api/v1/auth/login")
//                             .contentType(MediaType.APPLICATION_JSON)
//                             .content(objectMapper.writeValueAsString(request)))
//                     .andExpect(status().isBadRequest());
//         }
//     }
//
//     @Nested
//     @DisplayName("POST /api/v1/auth/logout")
//     class LogoutTest {
//
//         @Test
//         @WithMockUser
//         @DisplayName("로그아웃 성공")
//         void logout_Success() throws Exception {
//             willDoNothing().given(authService).logout();
//
//             mockMvc.perform(post("/api/v1/auth/logout"))
//                     .andDo(print())
//                     .andExpect(status().isOk());
//
//             verify(authService).logout();
//         }
//     }
// }
