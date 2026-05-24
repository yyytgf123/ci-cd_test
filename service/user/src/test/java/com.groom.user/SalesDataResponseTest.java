package com.groom.user;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.groom.user.infrastructure.client.SalesDataResponse;

@DisplayName("SalesDataResponse 테스트")
class SalesDataResponseTest {

    @Nested
    @DisplayName("생성자 테스트")
    class ConstructorTest {

        @Test
        @DisplayName("AllArgsConstructor로 생성")
        void allArgsConstructor() {
            LocalDate date = LocalDate.of(2025, 1, 15);
            SalesDataResponse response = new SalesDataResponse(date, 100000L, 10L);

            assertThat(response.getDate()).isEqualTo(date);
            assertThat(response.getTotalAmount()).isEqualTo(100000L);
            assertThat(response.getOrderCount()).isEqualTo(10L);
        }

        @Test
        @DisplayName("NoArgsConstructor로 생성")
        void noArgsConstructor() {
            SalesDataResponse response = new SalesDataResponse();

            assertThat(response.getDate()).isNull();
            assertThat(response.getTotalAmount()).isNull();
            assertThat(response.getOrderCount()).isNull();
        }
    }

    @Nested
    @DisplayName("Getter 테스트")
    class GetterTest {

        @Test
        @DisplayName("모든 필드 Getter")
        void allGetters() {
            LocalDate date = LocalDate.of(2025, 2, 20);
            SalesDataResponse response = new SalesDataResponse(date, 500000L, 50L);

            assertThat(response.getDate()).isEqualTo(LocalDate.of(2025, 2, 20));
            assertThat(response.getTotalAmount()).isEqualTo(500000L);
            assertThat(response.getOrderCount()).isEqualTo(50L);
        }

        @Test
        @DisplayName("0 값 처리")
        void zeroValues() {
            LocalDate date = LocalDate.of(2025, 3, 1);
            SalesDataResponse response = new SalesDataResponse(date, 0L, 0L);

            assertThat(response.getTotalAmount()).isZero();
            assertThat(response.getOrderCount()).isZero();
        }

        @Test
        @DisplayName("큰 숫자 처리")
        void largeValues() {
            LocalDate date = LocalDate.of(2025, 12, 31);
            SalesDataResponse response = new SalesDataResponse(date, 999999999999L, 1000000L);

            assertThat(response.getTotalAmount()).isEqualTo(999999999999L);
            assertThat(response.getOrderCount()).isEqualTo(1000000L);
        }
    }
}
