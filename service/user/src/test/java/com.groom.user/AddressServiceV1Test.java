//package com.groom.user;
//
//import static org.assertj.core.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.BDDMockito.*;
//
//import java.util.List;
//import java.util.Optional;
//import java.util.UUID;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.ArgumentCaptor;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.mock.mockito.MockBean;
//
//import com.groom.common.enums.UserRole;
//import com.groom.common.infrastructure.config.security.JwtUtil;
//import com.groom.common.presentation.advice.CustomException;
//import com.groom.common.presentation.advice.ErrorCode;
//import com.groom.user.application.service.AddressServiceV1;
//import com.groom.user.application.service.UserServiceV1;
//import com.groom.user.domain.entity.address.AddressEntity;
//import com.groom.user.domain.entity.user.UserEntity;
//import com.groom.user.domain.entity.user.UserStatus;
//import com.groom.user.domain.repository.AddressRepository;
//import com.groom.user.presentation.dto.request.address.ReqAddressDtoV1;
//import com.groom.user.presentation.dto.response.address.ResAddressDtoV1;
//
//@ExtendWith(MockitoExtension.class)
//@DisplayName("AddressServiceV1 테스트")
//@AutoConfigureMockMvc(addFilters = false) // ⭐ 핵심
//class AddressServiceV1Test {
//
//    @Mock
//    private AddressRepository addressRepository;
//
//    @MockBean
//    JwtUtil jwtUtil;
//
//    @Mock
//    private UserServiceV1 userService;
//
//    @InjectMocks
//    private AddressServiceV1 addressService;
//
//    private UUID userId;
//    private UUID addressId;
//    private UserEntity userEntity;
//    private AddressEntity addressEntity;
//
//    @BeforeEach
//    void setUp() {
//        userId = UUID.randomUUID();
//        addressId = UUID.randomUUID();
//
//        userEntity = UserEntity.builder()
//                .userId(userId)
//                .email("test@example.com")
//                .password("encodedPassword")
//                .nickname("testUser")
//                .phoneNumber("010-1234-5678")
//                .role(UserRole.USER)
//                .status(UserStatus.ACTIVE)
//                .build();
//
//        addressEntity = AddressEntity.builder()
//                .addressId(addressId)
//                .user(userEntity)
//                .zipCode("12345")
//                .address("서울시 강남구")
//                .detailAddress("101동 202호")
//                .recipient("홍길동")
//                .recipientPhone("010-9876-5432")
//                .isDefault(false)
//                .build();
//    }
//
//    @Nested
//    @DisplayName("getAddresses() 테스트")
//    class GetAddressesTest {
//
//        @Test
//        @DisplayName("배송지 목록 조회 성공")
//        void getAddresses_Success() {
//            AddressEntity address2 = AddressEntity.builder()
//                    .addressId(UUID.randomUUID())
//                    .user(userEntity)
//                    .zipCode("54321")
//                    .address("서울시 서초구")
//                    .detailAddress("201동 302호")
//                    .recipient("김철수")
//                    .recipientPhone("010-1111-2222")
//                    .isDefault(true)
//                    .build();
//
//            given(addressRepository.findByUserUserId(userId))
//                    .willReturn(List.of(addressEntity, address2));
//
//            List<ResAddressDtoV1> result = addressService.getAddresses(userId);
//
//            assertThat(result).hasSize(2);
//            assertThat(result.get(0).getZipCode()).isEqualTo("12345");
//            assertThat(result.get(1).getZipCode()).isEqualTo("54321");
//        }
//
//        @Test
//        @DisplayName("배송지가 없는 경우 빈 목록 반환")
//        void getAddresses_Empty() {
//            given(addressRepository.findByUserUserId(userId))
//                    .willReturn(List.of());
//
//            List<ResAddressDtoV1> result = addressService.getAddresses(userId);
//
//            assertThat(result).isEmpty();
//        }
//    }
//
//    @Nested
//    @DisplayName("createAddress() 테스트")
//    class CreateAddressTest {
//
//        @Test
//        @DisplayName("배송지 등록 성공")
//        void createAddress_Success() {
//            ReqAddressDtoV1 request = new ReqAddressDtoV1();
//            request.setZipCode("11111");
//            request.setAddress("서울시 마포구");
//            request.setDetailAddress("301동 401호");
//            request.setRecipient("이영희");
//            request.setRecipientPhone("010-5555-6666");
//            request.setIsDefault(false);
//
//            given(userService.findUserById(userId)).willReturn(userEntity);
//
//            addressService.createAddress(userId, request);
//
//            ArgumentCaptor<AddressEntity> captor = ArgumentCaptor.forClass(AddressEntity.class);
//            verify(addressRepository).save(captor.capture());
//
//            AddressEntity saved = captor.getValue();
//            assertThat(saved.getZipCode()).isEqualTo("11111");
//            assertThat(saved.getAddress()).isEqualTo("서울시 마포구");
//            assertThat(saved.getRecipient()).isEqualTo("이영희");
//            assertThat(saved.getIsDefault()).isFalse();
//        }
//
//        @Test
//        @DisplayName("기본 배송지로 등록 시 기존 기본 배송지 해제")
//        void createAddress_SetDefault_ClearExisting() {
//            ReqAddressDtoV1 request = new ReqAddressDtoV1();
//            request.setZipCode("22222");
//            request.setAddress("서울시 송파구");
//            request.setDetailAddress("401동 501호");
//            request.setRecipient("박민수");
//            request.setRecipientPhone("010-7777-8888");
//            request.setIsDefault(true);
//
//            given(userService.findUserById(userId)).willReturn(userEntity);
//
//            addressService.createAddress(userId, request);
//
//            verify(addressRepository).clearDefaultAddress(userId);
//
//            ArgumentCaptor<AddressEntity> captor = ArgumentCaptor.forClass(AddressEntity.class);
//            verify(addressRepository).save(captor.capture());
//            assertThat(captor.getValue().getIsDefault()).isTrue();
//        }
//
//        @Test
//        @DisplayName("isDefault가 null인 경우 false로 처리")
//        void createAddress_NullDefault() {
//            ReqAddressDtoV1 request = new ReqAddressDtoV1();
//            request.setZipCode("33333");
//            request.setAddress("서울시 영등포구");
//            request.setDetailAddress("501동 601호");
//            request.setRecipient("최지우");
//            request.setRecipientPhone("010-9999-0000");
//            request.setIsDefault(null);
//
//            given(userService.findUserById(userId)).willReturn(userEntity);
//
//            addressService.createAddress(userId, request);
//
//            verify(addressRepository, never()).clearDefaultAddress(any());
//
//            ArgumentCaptor<AddressEntity> captor = ArgumentCaptor.forClass(AddressEntity.class);
//            verify(addressRepository).save(captor.capture());
//            assertThat(captor.getValue().getIsDefault()).isFalse();
//        }
//    }
//
//    @Nested
//    @DisplayName("updateAddress() 테스트")
//    class UpdateAddressTest {
//
//        @Test
//        @DisplayName("배송지 수정 성공")
//        void updateAddress_Success() {
//            ReqAddressDtoV1 request = new ReqAddressDtoV1();
//            request.setZipCode("99999");
//            request.setAddress("부산시 해운대구");
//            request.setDetailAddress("101동 101호");
//            request.setRecipient("김수정");
//            request.setRecipientPhone("010-1234-0000");
//            request.setIsDefault(false);
//
//            given(addressRepository.findByAddressIdAndUserUserId(addressId, userId))
//                    .willReturn(Optional.of(addressEntity));
//
//            addressService.updateAddress(userId, addressId, request);
//
//            assertThat(addressEntity.getZipCode()).isEqualTo("99999");
//            assertThat(addressEntity.getAddress()).isEqualTo("부산시 해운대구");
//            assertThat(addressEntity.getRecipient()).isEqualTo("김수정");
//        }
//
//        @Test
//        @DisplayName("기본 배송지로 변경 시 기존 기본 배송지 해제")
//        void updateAddress_SetDefault_ClearExisting() {
//            ReqAddressDtoV1 request = new ReqAddressDtoV1();
//            request.setZipCode("88888");
//            request.setAddress("대전시 서구");
//            request.setDetailAddress("201동 201호");
//            request.setRecipient("정도윤");
//            request.setRecipientPhone("010-2222-3333");
//            request.setIsDefault(true);
//
//            given(addressRepository.findByAddressIdAndUserUserId(addressId, userId))
//                    .willReturn(Optional.of(addressEntity));
//
//            addressService.updateAddress(userId, addressId, request);
//
//            verify(addressRepository).clearDefaultAddress(userId);
//            assertThat(addressEntity.getIsDefault()).isTrue();
//        }
//
//        @Test
//        @DisplayName("존재하지 않는 배송지 수정 시 예외 발생")
//        void updateAddress_NotFound_ThrowsException() {
//            ReqAddressDtoV1 request = new ReqAddressDtoV1();
//            request.setZipCode("77777");
//            request.setAddress("광주시 북구");
//            request.setDetailAddress("301동 301호");
//            request.setRecipient("이서준");
//            request.setRecipientPhone("010-4444-5555");
//
//            given(addressRepository.findByAddressIdAndUserUserId(addressId, userId))
//                    .willReturn(Optional.empty());
//
//            assertThatThrownBy(() -> addressService.updateAddress(userId, addressId, request))
//                    .isInstanceOf(CustomException.class)
//                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ADDRESS_NOT_FOUND);
//        }
//    }
//
//    @Nested
//    @DisplayName("deleteAddress() 테스트")
//    class DeleteAddressTest {
//
//        @Test
//        @DisplayName("배송지 삭제 성공")
//        void deleteAddress_Success() {
//            given(addressRepository.findByAddressIdAndUserUserId(addressId, userId))
//                    .willReturn(Optional.of(addressEntity));
//
//            addressService.deleteAddress(userId, addressId);
//
//            verify(addressRepository).delete(addressEntity);
//        }
//
//        @Test
//        @DisplayName("존재하지 않는 배송지 삭제 시 예외 발생")
//        void deleteAddress_NotFound_ThrowsException() {
//            given(addressRepository.findByAddressIdAndUserUserId(addressId, userId))
//                    .willReturn(Optional.empty());
//
//            assertThatThrownBy(() -> addressService.deleteAddress(userId, addressId))
//                    .isInstanceOf(CustomException.class)
//                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ADDRESS_NOT_FOUND);
//        }
//    }
//
//    @Nested
//    @DisplayName("setDefaultAddress() 테스트")
//    class SetDefaultAddressTest {
//
//        @Test
//        @DisplayName("기본 배송지 설정 성공")
//        void setDefaultAddress_Success() {
//            given(addressRepository.findByAddressIdAndUserUserId(addressId, userId))
//                    .willReturn(Optional.of(addressEntity));
//
//            addressService.setDefaultAddress(userId, addressId);
//
//            verify(addressRepository).clearDefaultAddress(userId);
//            assertThat(addressEntity.getIsDefault()).isTrue();
//        }
//
//        @Test
//        @DisplayName("이미 기본 배송지인 경우 예외 발생")
//        void setDefaultAddress_AlreadyDefault_ThrowsException() {
//            addressEntity.setDefault(true);
//
//            given(addressRepository.findByAddressIdAndUserUserId(addressId, userId))
//                    .willReturn(Optional.of(addressEntity));
//
//            assertThatThrownBy(() -> addressService.setDefaultAddress(userId, addressId))
//                    .isInstanceOf(CustomException.class)
//                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_DEFAULT_ADDRESS);
//        }
//
//        @Test
//        @DisplayName("존재하지 않는 배송지를 기본으로 설정 시 예외 발생")
//        void setDefaultAddress_NotFound_ThrowsException() {
//            given(addressRepository.findByAddressIdAndUserUserId(addressId, userId))
//                    .willReturn(Optional.empty());
//
//            assertThatThrownBy(() -> addressService.setDefaultAddress(userId, addressId))
//                    .isInstanceOf(CustomException.class)
//                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ADDRESS_NOT_FOUND);
//        }
//    }
//
//    @Nested
//    @DisplayName("getAddress() 테스트")
//    class GetAddressTest {
//
//        @Test
//        @DisplayName("단일 배송지 조회 성공")
//        void getAddress_Success() {
//            given(addressRepository.findByAddressIdAndUserUserId(addressId, userId))
//                    .willReturn(Optional.of(addressEntity));
//
//            ResAddressDtoV1 result = addressService.getAddress(addressId, userId);
//
//            assertThat(result).isNotNull();
//            assertThat(result.getId()).isEqualTo(addressId);
//            assertThat(result.getZipCode()).isEqualTo("12345");
//            assertThat(result.getRecipient()).isEqualTo("홍길동");
//        }
//
//        @Test
//        @DisplayName("존재하지 않는 배송지 조회 시 예외 발생")
//        void getAddress_NotFound_ThrowsException() {
//            given(addressRepository.findByAddressIdAndUserUserId(addressId, userId))
//                    .willReturn(Optional.empty());
//
//            assertThatThrownBy(() -> addressService.getAddress(addressId, userId))
//                    .isInstanceOf(CustomException.class)
//                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ADDRESS_NOT_FOUND);
//        }
//    }
//}
