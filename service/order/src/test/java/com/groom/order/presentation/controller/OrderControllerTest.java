// package com.groom.order.presentation.controller;

// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.BDDMockito.given;
// import static org.mockito.BDDMockito.then;
// import static org.mockito.Mockito.times;
// import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
// import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
// import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
// import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// import java.time.LocalDateTime;
// import java.util.List;
// import java.util.UUID;

// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Nested;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;
// import org.springframework.data.domain.Page;
// import org.springframework.data.domain.PageImpl;
// import org.springframework.data.domain.PageRequest;
// import org.springframework.data.domain.Pageable;
// import org.springframework.http.MediaType;
// import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
// import org.springframework.security.core.context.SecurityContextHolder;
// import org.springframework.test.web.servlet.MockMvc;
// import org.springframework.test.web.servlet.setup.MockMvcBuilders;

// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.groom.common.infrastructure.config.security.CustomUserDetails;
// import com.groom.order.application.service.OrderService;
// import com.groom.order.domain.status.OrderStatus;
// import com.groom.order.presentation.dto.request.OrderCreateItemRequest;
// import com.groom.order.presentation.dto.request.OrderCreateRequest;
// import com.groom.order.presentation.dto.response.OrderResponse;

// /**
//  * OrderController 단위 테스트
//  * 
//  * Standalone MockMvc를 사용하여 Spring Context 없이 순수 컨트롤러 로직만 테스트합니다.
//  */
// @ExtendWith(MockitoExtension.class)
// @DisplayName("OrderController 테스트")
// class OrderControllerTest {

//     private MockMvc mockMvc;

//     private ObjectMapper objectMapper;

//     @Mock
//     private OrderService orderService;

//     @InjectMocks
//     private OrderController orderController;

//     private UUID userId;
//     private UUID orderId;
//     private UUID productId;
//     private CustomUserDetails mockUserDetails;

//     @BeforeEach
//     void setUp() {
//         userId = UUID.randomUUID();
//         orderId = UUID.randomUUID();
//         productId = UUID.randomUUID();
//         mockUserDetails = new CustomUserDetails(userId, "test@example.com", "ROLE_USER");
//         objectMapper = new ObjectMapper();

//         // Standalone MockMvc setup (no Spring context)
//         mockMvc = MockMvcBuilders.standaloneSetup(orderController).build();

//         // Mock Security Context
//         SecurityContextHolder.getContext().setAuthentication(
//                 new UsernamePasswordAuthenticationToken(mockUserDetails, null, mockUserDetails.getAuthorities()));
//     }

//     @Nested
//     @DisplayName("POST /api/v2/orders - 주문 생성")
//     class CreateOrderTest {

//         @Test
//         @DisplayName("정상적인 주문 생성 요청 시 orderId를 반환해야 한다")
//         void createOrder_Success_ShouldReturn_OrderId() throws Exception {
//             // given
//             OrderCreateRequest request = createOrderRequest();
//             given(orderService.createOrder(any(UUID.class), any(OrderCreateRequest.class)))
//                     .willReturn(orderId);

//             // when & then
//             mockMvc.perform(post("/api/v2/orders")
//                     .contentType(MediaType.APPLICATION_JSON)
//                     .content(objectMapper.writeValueAsString(request)))
//                     .andExpect(status().isOk())
//                     .andExpect(jsonPath("$.orderId").value(orderId.toString()));

//             then(orderService).should(times(1)).createOrder(any(UUID.class), any(OrderCreateRequest.class));
//         }
//     }

//     @Nested
//     @DisplayName("GET /api/v2/orders - 내 주문 목록 조회")
//     class GetMyOrdersTest {

//         @Test
//         @DisplayName("로그인한 사용자의 주문 목록을 페이징하여 반환해야 한다")
//         void getMyOrders_Success_ShouldReturn_PagedOrders() throws Exception {
//             // given
//             OrderResponse order1 = createOrderResponse();
//             OrderResponse order2 = createOrderResponse();
//             Page<OrderResponse> page = new PageImpl<>(List.of(order1, order2), PageRequest.of(0, 10), 2);

//             given(orderService.getMyOrders(any(UUID.class), any(Pageable.class)))
//                     .willReturn(page);

//             // when & then
//             mockMvc.perform(get("/api/v2/orders"))
//                     .andExpect(status().isOk())
//                     .andExpect(jsonPath("$.content").isArray())
//                     .andExpect(jsonPath("$.content.length()").value(2))
//                     .andExpect(jsonPath("$.totalElements").value(2));
//         }
//     }

//     @Nested
//     @DisplayName("GET /api/v2/orders/{orderId} - 주문 상세 조회")
//     class GetOrderTest {

//         @Test
//         @DisplayName("orderId로 주문 상세 정보를 조회할 수 있어야 한다")
//         void getOrder_Success_ShouldReturn_OrderDetail() throws Exception {
//             // given
//             OrderResponse response = createOrderResponse();
//             given(orderService.getOrder(orderId)).willReturn(response);

//             // when & then
//             mockMvc.perform(get("/api/v2/orders/{orderId}", orderId))
//                     .andExpect(status().isOk())
//                     .andExpect(jsonPath("$.orderId").value(response.orderId().toString()))
//                     .andExpect(jsonPath("$.orderNo").value(response.orderNo()))
//                     .andExpect(jsonPath("$.status").value(response.status().name()));

//             then(orderService).should(times(1)).getOrder(orderId);
//         }
//     }

//     @Nested
//     @DisplayName("GET /api/v2/orders/product/{productId} - 상품별 주문 목록 조회")
//     class GetOrdersByProductTest {

//         @Test
//         @DisplayName("productId로 주문 목록을 조회할 수 있어야 한다")
//         void getOrdersByProduct_Success_ShouldReturn_OrderList() throws Exception {
//             // given
//             OrderResponse order1 = createOrderResponse();
//             OrderResponse order2 = createOrderResponse();
//             given(orderService.getOrdersByProduct(productId))
//                     .willReturn(List.of(order1, order2));

//             // when & then
//             mockMvc.perform(get("/api/v2/orders/product/{productId}", productId))
//                     .andExpect(status().isOk())
//                     .andExpect(jsonPath("$").isArray())
//                     .andExpect(jsonPath("$.length()").value(2));

//             then(orderService).should(times(1)).getOrdersByProduct(productId);
//         }
//     }

//     @Nested
//     @DisplayName("POST /api/v2/orders/{orderId}/cancel - 주문 취소")
//     class CancelOrderTest {

//         @Test
//         @DisplayName("주문 취소 요청 시 성공 메시지를 반환해야 한다")
//         void cancelOrder_Success_ShouldReturn_SuccessMessage() throws Exception {
//             // when & then
//             mockMvc.perform(post("/api/v2/orders/{orderId}/cancel", orderId))
//                     .andExpect(status().isOk())
//                     .andExpect(jsonPath("$").value("주문이 성공적으로 취소되었습니다."));

//             then(orderService).should(times(1)).cancelOrder(orderId);
//         }
//     }

//     // ========== Helper Methods ==========

//     private OrderCreateRequest createOrderRequest() {
//         OrderCreateRequest request = new OrderCreateRequest();
//         request.setAddressId(UUID.randomUUID());
//         request.setTotalAmount(50000L);

//         OrderCreateItemRequest item = new OrderCreateItemRequest();
//         item.setProductId(productId);
//         item.setVariantId(UUID.randomUUID());
//         item.setQuantity(2);
//         item.setProductTitle("테스트 상품");
//         item.setProductThumbnail("https://example.com/thumbnail.jpg");
//         item.setOptionName("기본옵션");
//         item.setUnitPrice(25000L);

//         request.setItems(List.of(item));
//         return request;
//     }

//     private OrderResponse createOrderResponse() {
//         return new OrderResponse(
//                 orderId,
//                 "20260204-123456",
//                 OrderStatus.PENDING,
//                 50000L,
//                 LocalDateTime.now(),
//                 List.of());
//     }
// }
