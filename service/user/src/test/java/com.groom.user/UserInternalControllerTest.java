// package com.groom.user;
//
// import static org.mockito.BDDMockito.*;
// import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
// import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
// import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
// import java.util.Optional;
// import java.util.UUID;
//
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Nested;
// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
// import org.springframework.boot.test.mock.mockito.MockBean;
// import org.springframework.test.web.servlet.MockMvc;
//
// import com.groom.common.enums.UserRole;
// import com.groom.common.infrastructure.config.security.JwtUtil;
// import com.groom.user.domain.entity.address.AddressEntity;
// import com.groom.user.domain.entity.user.UserEntity;
// import com.groom.user.domain.entity.user.UserStatus;
// import com.groom.user.domain.repository.AddressRepository;
// import com.groom.user.domain.repository.UserRepository;
// import com.groom.user.presentation.controller.UserInternalController;
//
// @WebMvcTest(UserInternalController.class)
// @DisplayName("UserInternalController 테스트")
// class UserInternalControllerTest {
//
//     @Autowired
//     private MockMvc mockMvc;
//
//     // ✅ jwtAuthenticationFilter 의존성 충족용
//     @MockBean
//     private JwtUtil jwtUtil;
//
//     @MockBean
//     private UserRepository userRepository;
//
//     @MockBean
//     private AddressRepository addressRepository;
//
//     private UUID userId;
//     private UserEntity userEntity;
//     private AddressEntity addressEntity;
//
//     @BeforeEach
//     void setUp() {
//         userId = UUID.randomUUID();
//
//         userEntity = UserEntity.builder()
//             .userId(userId)
//             .email("test@example.com")
//             .password("encodedPassword")
//             .nickname("testUser")
//             .phoneNumber("010-1234-5678")
//             .role(UserRole.USER)
//             .status(UserStatus.ACTIVE)
//             .build();
//
//         addressEntity = AddressEntity.builder()
//             .addressId(UUID.randomUUID())
//             .user(userEntity)
//             .zipCode("12345")
//             .address("서울시 강남구")
//             .detailAddress("101동 202호")
//             .recipient("홍길동")
//             .recipientPhone("010-9876-5432")
//             .isDefault(true)
//             .build();
//     }
//
//     @Nested
//     @DisplayName("GET /internal/users/{userId}/validate")
//     class ValidateUserTest {
//
//         @Test
//         @DisplayName("사용자 유효성 검증 성공")
//         void validateUser_Success() throws Exception {
//             given(userRepository.findByUserIdAndDeletedAtIsNull(userId))
//                 .willReturn(Optional.of(userEntity));
//
//             mockMvc.perform(get("/internal/users/{userId}/validate", userId))
//                 .andDo(print())
//                 .andExpect(status().isOk());
//         }
//
//         @Test
//         @DisplayName("존재하지 않는 사용자 검증 시 404")
//         void validateUser_NotFound() throws Exception {
//             given(userRepository.findByUserIdAndDeletedAtIsNull(userId))
//                 .willReturn(Optional.empty());
//
//             mockMvc.perform(get("/internal/users/{userId}/validate", userId))
//                 .andExpect(status().isNotFound());
//         }
//
//         @Test
//         @DisplayName("탈퇴한 사용자 검증 시 400")
//         void validateUser_Withdrawn() throws Exception {
//             userEntity.withdraw();
//             given(userRepository.findByUserIdAndDeletedAtIsNull(userId))
//                 .willReturn(Optional.of(userEntity));
//
//             mockMvc.perform(get("/internal/users/{userId}/validate", userId))
//                 .andExpect(status().isBadRequest());
//         }
//     }
//
//     @Nested
//     @DisplayName("GET /internal/users/{userId}/address")
//     class GetUserAddressTest {
//
//         @Test
//         @DisplayName("사용자 배송지 조회 성공")
//         void getUserAddress_Success() throws Exception {
//             given(addressRepository.findByUserUserIdAndIsDefaultTrue(userId))
//                 .willReturn(Optional.of(addressEntity));
//
//             mockMvc.perform(get("/internal/users/{userId}/address", userId))
//                 .andDo(print())
//                 .andExpect(status().isOk())
//                 .andExpect(jsonPath("$.recipientName").value("홍길동"))
//                 .andExpect(jsonPath("$.recipientPhone").value("010-9876-5432"))
//                 .andExpect(jsonPath("$.zipCode").value("12345"))
//                 .andExpect(jsonPath("$.address").value("서울시 강남구"))
//                 .andExpect(jsonPath("$.detailAddress").value("101동 202호"));
//         }
//
//         @Test
//         @DisplayName("기본 배송지가 없는 경우 404")
//         void getUserAddress_NotFound() throws Exception {
//             given(addressRepository.findByUserUserIdAndIsDefaultTrue(userId))
//                 .willReturn(Optional.empty());
//
//             mockMvc.perform(get("/internal/users/{userId}/address", userId))
//                 .andExpect(status().isNotFound());
//         }
//     }
// }
