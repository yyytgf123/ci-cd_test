package com.groom.user;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.groom.user.application.port.out.SalesData;
import com.groom.user.domain.entity.user.PeriodType;
import com.groom.user.infrastructure.adapter.FeignOrderAdapter;
import com.groom.user.infrastructure.client.OrderServiceClient;
import com.groom.user.infrastructure.client.SalesDataResponse;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeignOrderAdapter 테스트")
class FeignOrderAdapterTest {

    @Mock
    private OrderServiceClient orderServiceClient;

    @InjectMocks
    private FeignOrderAdapter feignOrderAdapter;

    private UUID ownerId;
    private LocalDate date;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        date = LocalDate.of(2025, 1, 15);
    }

    @Nested
    @DisplayName("getOwnerSales() 테스트")
    class GetOwnerSalesTest {

        @Test
        @DisplayName("DAILY 기간 매출 조회 성공")
        void getOwnerSales_Daily_Success() {
            List<SalesDataResponse> responses = List.of(
                    new SalesDataResponse(date, 100000L, 10L),
                    new SalesDataResponse(date.plusDays(1), 150000L, 15L)
            );

            given(orderServiceClient.getOwnerSales(ownerId, "DAILY", date))
                    .willReturn(responses);

            List<SalesData> result = feignOrderAdapter.getOwnerSales(ownerId, PeriodType.DAILY, date);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getDate()).isEqualTo(date);
            assertThat(result.get(0).getTotalAmount()).isEqualTo(100000L);
            assertThat(result.get(0).getOrderCount()).isEqualTo(10L);
            assertThat(result.get(1).getTotalAmount()).isEqualTo(150000L);
        }

        @Test
        @DisplayName("MONTHLY 기간 매출 조회 성공")
        void getOwnerSales_Monthly_Success() {
            List<SalesDataResponse> responses = List.of(
                    new SalesDataResponse(LocalDate.of(2025, 1, 1), 3000000L, 300L)
            );

            given(orderServiceClient.getOwnerSales(ownerId, "MONTHLY", date))
                    .willReturn(responses);

            List<SalesData> result = feignOrderAdapter.getOwnerSales(ownerId, PeriodType.MONTHLY, date);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTotalAmount()).isEqualTo(3000000L);
            assertThat(result.get(0).getOrderCount()).isEqualTo(300L);
        }

        @Test
        @DisplayName("빈 결과 반환")
        void getOwnerSales_EmptyResult() {
            given(orderServiceClient.getOwnerSales(ownerId, "DAILY", date))
                    .willReturn(List.of());

            List<SalesData> result = feignOrderAdapter.getOwnerSales(ownerId, PeriodType.DAILY, date);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("여러 날짜 데이터 변환")
        void getOwnerSales_MultipleDates() {
            List<SalesDataResponse> responses = List.of(
                    new SalesDataResponse(date, 50000L, 5L),
                    new SalesDataResponse(date.plusDays(1), 60000L, 6L),
                    new SalesDataResponse(date.plusDays(2), 70000L, 7L),
                    new SalesDataResponse(date.plusDays(3), 80000L, 8L)
            );

            given(orderServiceClient.getOwnerSales(ownerId, "DAILY", date))
                    .willReturn(responses);

            List<SalesData> result = feignOrderAdapter.getOwnerSales(ownerId, PeriodType.DAILY, date);

            assertThat(result).hasSize(4);
            assertThat(result).extracting(SalesData::getTotalAmount)
                    .containsExactly(50000L, 60000L, 70000L, 80000L);
            assertThat(result).extracting(SalesData::getOrderCount)
                    .containsExactly(5L, 6L, 7L, 8L);
        }

        @Test
        @DisplayName("OrderServiceClient 호출 검증")
        void getOwnerSales_VerifyClientCall() {
            given(orderServiceClient.getOwnerSales(any(), anyString(), any()))
                    .willReturn(List.of());

            feignOrderAdapter.getOwnerSales(ownerId, PeriodType.DAILY, date);

            verify(orderServiceClient).getOwnerSales(ownerId, "DAILY", date);
        }
    }
}
