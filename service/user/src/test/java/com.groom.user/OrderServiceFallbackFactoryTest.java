package com.groom.user;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.groom.user.infrastructure.client.OrderServiceClient;
import com.groom.user.infrastructure.client.SalesDataResponse;
import com.groom.user.infrastructure.fallback.OrderServiceFallbackFactory;

@DisplayName("OrderServiceFallbackFactory 테스트")
class OrderServiceFallbackFactoryTest {

    private final OrderServiceFallbackFactory fallbackFactory = new OrderServiceFallbackFactory();

    @Nested
    @DisplayName("create() 테스트")
    class CreateTest {

        @Test
        @DisplayName("Fallback 클라이언트 생성 성공")
        void create_Success() {
            RuntimeException cause = new RuntimeException("Connection refused");

            OrderServiceClient fallbackClient = fallbackFactory.create(cause);

            assertThat(fallbackClient).isNotNull();
        }

        @Test
        @DisplayName("다양한 예외로 Fallback 생성")
        void create_VariousExceptions() {
            // RuntimeException
            OrderServiceClient client1 = fallbackFactory.create(new RuntimeException("Error"));
            assertThat(client1).isNotNull();

            // IllegalStateException
            OrderServiceClient client2 = fallbackFactory.create(new IllegalStateException("State error"));
            assertThat(client2).isNotNull();

            // NullPointerException
            OrderServiceClient client3 = fallbackFactory.create(new NullPointerException("Null"));
            assertThat(client3).isNotNull();
        }
    }

    @Nested
    @DisplayName("Fallback getOwnerSales() 테스트")
    class GetOwnerSalesFallbackTest {

        @Test
        @DisplayName("Fallback 호출 시 빈 리스트 반환")
        void getOwnerSales_ReturnsEmptyList() {
            RuntimeException cause = new RuntimeException("Service unavailable");
            OrderServiceClient fallbackClient = fallbackFactory.create(cause);

            UUID ownerId = UUID.randomUUID();
            LocalDate date = LocalDate.of(2025, 1, 15);

            List<SalesDataResponse> result = fallbackClient.getOwnerSales(ownerId, "DAILY", date);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("다양한 파라미터로 Fallback 호출")
        void getOwnerSales_VariousParameters() {
            OrderServiceClient fallbackClient = fallbackFactory.create(new RuntimeException("Error"));

            // DAILY
            List<SalesDataResponse> daily = fallbackClient.getOwnerSales(
                    UUID.randomUUID(), "DAILY", LocalDate.now());
            assertThat(daily).isEmpty();

            // MONTHLY
            List<SalesDataResponse> monthly = fallbackClient.getOwnerSales(
                    UUID.randomUUID(), "MONTHLY", LocalDate.of(2025, 1, 1));
            assertThat(monthly).isEmpty();

            // null ownerId
            List<SalesDataResponse> nullOwner = fallbackClient.getOwnerSales(
                    null, "DAILY", LocalDate.now());
            assertThat(nullOwner).isEmpty();
        }
    }
}
