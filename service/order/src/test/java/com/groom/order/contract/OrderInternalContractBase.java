package com.groom.order.contract;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.groom.order.application.service.OrderService;
import com.groom.order.domain.status.OrderStatus;
import com.groom.order.presentation.dto.internal.OrderValidationResponse;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("contracttest")
public abstract class OrderInternalContractBase {

    @Autowired
    private WebApplicationContext context;

    @MockBean
    private OrderService orderService;

    @BeforeEach
    void setup() {
        RestAssuredMockMvc.webAppContextSetup(context);

        when(orderService.getOrderForPayment(any(UUID.class)))
            .thenReturn(new OrderValidationResponse(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                50000L,
                OrderStatus.PENDING
            ));
    }
}
