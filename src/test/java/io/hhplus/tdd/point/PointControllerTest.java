package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PointController 단위 테스트")
class PointControllerTest {

    @Mock
    private PointService pointService;

    private PointController pointController;

    @BeforeEach
    void setUp() {
        pointController = new PointController(pointService);
    }

    @Nested
    @DisplayName("포인트 조회 기능")
    class PointTest {

        @Test
        @DisplayName("정상적인 ID로 포인트 조회 시 UserPoint를 반환한다")
        void point_whenValidId() {
            // given
            long userId = 1L;
            UserPoint expectedUserPoint = new UserPoint(userId, 1000L, System.currentTimeMillis());
            when(pointService.point(userId)).thenReturn(expectedUserPoint);

            // when
            UserPoint result = pointController.point(userId);

            // then
            assertThat(result).isEqualTo(expectedUserPoint);
            verify(pointService).point(userId);
        }

        @Test
        @DisplayName("음수의 ID로 포인트 조회 시 BAD_REQUEST 예외가 발생한다")
        void point_whenNegativeId() {
            // given
            long negativeUserId = -1L;

            // when & then
            assertThatThrownBy(() -> pointController.point(negativeUserId))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                    .hasMessageContaining("유효하지 않은 사용자 ID입니다: " + negativeUserId);

            verify(pointService, never()).point(anyLong());
        }
    }

    @Nested
    @DisplayName("포인트 사용내역 조회 기능")
    class HistoryTest {

        @Test
        @DisplayName("정상적인 ID로 사용내역 조회 시 PointHistory 리스트를 반환한다")
        void history_whenValidId() {
            // given
            long userId = 1L;
            List<PointHistory> expectedHistories = Arrays.asList(
                    new PointHistory(1L, userId, 500L, TransactionType.CHARGE, System.currentTimeMillis()),
                    new PointHistory(2L, userId, 200L, TransactionType.USE, System.currentTimeMillis())
            );
            when(pointService.history(userId)).thenReturn(expectedHistories);

            // when
            List<PointHistory> result = pointController.history(userId);

            // then
            assertThat(result).isEqualTo(expectedHistories);
            assertThat(result).hasSize(2);
            verify(pointService).history(userId);
        }

        @Test
        @DisplayName("내역이 없는 사용자 ID로 조회 시 빈 리스트를 반환한다")
        void history_whenNoHistory() {
            // given
            long userId = 999L;
            when(pointService.history(userId)).thenReturn(List.of());

            // when
            List<PointHistory> result = pointController.history(userId);

            // then
            assertThat(result).isEmpty();
            verify(pointService).history(userId);
        }

        @Test
        @DisplayName("음수의 ID로 사용내역 조회 시 BAD_REQUEST 예외가 발생한다")
        void history_whenNegativeId() {
            // given
            long negativeUserId = -1L;

            // when & then
            assertThatThrownBy(() -> pointController.history(negativeUserId))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                    .hasMessageContaining("유효하지 않은 사용자 ID입니다: " + negativeUserId);

            verify(pointService, never()).history(anyLong());
        }
    }

    @Nested
    @DisplayName("포인트 충전 기능")
    class ChargeTest {

        @Test
        @DisplayName("정상적인 충전 요청 시 포인트가 충전되고 내역이 저장된다")
        void charge_whenValidIdAndAmount() {
            // given
            long userId = 1L;
            long chargeAmount = 500L;
            long currentPoint = 1000L;
            long expectedNewPoint = currentPoint + chargeAmount;

            UserPoint currentUserPoint = new UserPoint(userId, currentPoint, System.currentTimeMillis());
            UserPoint expectedUserPoint = new UserPoint(userId, expectedNewPoint, System.currentTimeMillis());

            when(pointService.point(userId)).thenReturn(currentUserPoint);
            when(pointService.charge(userId, expectedNewPoint)).thenReturn(expectedUserPoint);

            // when
            UserPoint result = pointController.charge(userId, chargeAmount);

            // then
            assertThat(result.point()).isEqualTo(expectedNewPoint);
            verify(pointService).charge(userId, chargeAmount);

        }

        @Test
        @DisplayName("0 이하의 ID로 충전 시 BAD_REQUEST 예외가 발생한다")
        void charge_whenInvalidId() {
            // given
            long invalidUserId = -1L;
            long chargeAmount = 500L;

            // when & then
            assertThatThrownBy(() -> pointController.charge(invalidUserId, chargeAmount))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                    .hasMessageContaining("유효하지 않은 사용자 ID입니다: " + invalidUserId);

            verify(pointService, never()).charge(invalidUserId, chargeAmount);
        }

        @Test
        @DisplayName("0원 충전 요청 시 BAD_REQUEST 예외가 발생한다")
        void charge_whenAmountIsZero() {
            // given
            long userId = 1L;
            long invalidAmount = 0L;

            // when & then
            assertThatThrownBy(() -> pointController.charge(userId, invalidAmount))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                    .hasMessageContaining("충전 금액은 0보다 커야 합니다: " + invalidAmount);

            verify(pointService, never()).charge(userId, invalidAmount);
        }

        @Test
        @DisplayName("음수의 충전 금액으로 요청 시 BAD_REQUEST 예외가 발생한다")
        void charge_whenNegativeAmount() {
            // given
            long userId = 1L;
            long negativeAmount = -100L;

            // when & then
            assertThatThrownBy(() -> pointController.charge(userId, negativeAmount))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                    .hasMessageContaining("충전 금액은 0보다 커야 합니다: " + negativeAmount);

            verify(pointService, never()).charge(userId, negativeAmount);
        }

        @Test
        @DisplayName("최대값으로 충전 시 오버플로우 예외가 발생한다")
        void charge_whenAmountCausesOverflow() {
            // given
            long userId = 1L;
            long currentPoint = Long.MAX_VALUE - 1000L; // 거의 최대값
            long chargeAmount = 2000L; // 오버플로우를 일으킬 충전량

            // 오버플로우 상황에서 서비스가 예외를 던진다고 가정
            when(pointService.charge(userId, chargeAmount))
                    .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "충전 후 포인트가 최대값을 초과합니다"));

            // when & then
            assertThatThrownBy(() -> pointController.charge(userId, chargeAmount))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                    .hasMessageContaining("충전 후 포인트가 최대값을 초과합니다");

            verify(pointService).charge(userId, chargeAmount);
        }

        @Test
        @DisplayName("Long.MAX_VALUE 충전량으로 요청 시 예외가 발생한다")
        void charge_whenAmountIsMaxLongValue() {
            // given
            long userId = 1L;
            long maxAmount = Long.MAX_VALUE;

            when(pointService.charge(userId, maxAmount))
                    .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "충전 금액이 허용된 범위를 초과합니다"));

            // when & then
            assertThatThrownBy(() -> pointController.charge(userId, maxAmount))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                    .hasMessageContaining("충전 금액이 허용된 범위를 초과합니다");

            verify(pointService).charge(userId, maxAmount);
        }

        @Test
        @DisplayName("DB 장애 상황에서 INTERNAL_SERVER_ERROR 예외가 발생한다")
        void charge_WhenDatabaseFailure() {
            // given
            long userId = 1L;
            long chargeAmount = 500L;

            when(pointService.charge(userId, chargeAmount))
                    .thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database connection failed"));

            // when & then
            assertThatThrownBy(() -> pointController.charge(userId, chargeAmount))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.INTERNAL_SERVER_ERROR)
                    .hasMessageContaining("Database connection failed");

            verify(pointService).charge(userId, chargeAmount);
        }

        @Test
        @DisplayName("충전 후 포인트가 정확히 합산되는지 비즈니스 로직 검증")
        void charge_VerifyBusinessLogic() {
            // given
            long userId = 1L;
            long initialPoint = 1000L;
            long chargeAmount1 = 500L;
            long chargeAmount2 = 300L;

            // 첫 번째 충전
            UserPoint afterFirstCharge = new UserPoint(userId, initialPoint + chargeAmount1, System.currentTimeMillis());
            when(pointService.charge(userId, chargeAmount1)).thenReturn(afterFirstCharge);

            // 두 번째 충전
            UserPoint afterSecondCharge = new UserPoint(userId, initialPoint + chargeAmount1 + chargeAmount2, System.currentTimeMillis());
            when(pointService.charge(userId, chargeAmount2)).thenReturn(afterSecondCharge);

            // when
            UserPoint firstResult = pointController.charge(userId, chargeAmount1);
            UserPoint secondResult = pointController.charge(userId, chargeAmount2);

            // then
            // 첫 번째 충전 후 검증
            assertThat(firstResult.point()).isEqualTo(initialPoint + chargeAmount1);

            // 두 번째 충전 후 검증 (누적 충전)
            assertThat(secondResult.point()).isEqualTo(initialPoint + chargeAmount1 + chargeAmount2);

            // 서비스 호출 검증
            verify(pointService).charge(userId, chargeAmount1);
            verify(pointService).charge(userId, chargeAmount2);
        }
    }

    @Nested
    @DisplayName("포인트 사용 기능")
    class UseTest {

        @Test
        @DisplayName("정상적인 사용 요청 시 포인트가 차감되고 내역이 저장된다")
        void use_whenValidIdAndAmount() {
            // given
            long userId = 1L;
            long useAmount = 300L;
            long currentPoint = 1000L;
            long expectedNewPoint = currentPoint - useAmount;

            UserPoint expectedUserPoint = new UserPoint(userId, expectedNewPoint, System.currentTimeMillis());

            when(pointService.use(userId, useAmount)).thenReturn(expectedUserPoint);

            // when
            UserPoint result = pointController.use(userId, useAmount);

            // then
            assertThat(result.point()).isEqualTo(expectedNewPoint);
            verify(pointService).use(userId, useAmount);
        }

        @Test
        @DisplayName("잔고 부족 시 BAD_REQUEST 예외가 발생한다")
        void use_whenInsufficientBalance() {
            // given
            long userId = 1L;
            long useAmount = 1500L;
            long currentPoint = 1000L;

            UserPoint currentUserPoint = new UserPoint(userId, currentPoint, System.currentTimeMillis());
            when(pointService.use(userId, useAmount))
                    .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "포인트가 부족합니다."));

            // when & then
            assertThatThrownBy(() -> pointController.use(userId, useAmount))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                    .hasMessageContaining("포인트가 부족합니다");

            verify(pointService).use(userId, useAmount);
        }

        @Test
        @DisplayName("음수의 ID로 사용 시 BAD_REQUEST 예외가 발생한다")
        void use_whenNegativeId() {
            // given
            long negativeUserId = -1L;
            long useAmount = 500L;
            when(pointService.use(negativeUserId, useAmount))
                    .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "유효하지 않은 사용자 ID입니다."));

            // when & then
            assertThatThrownBy(() -> pointController.use(negativeUserId, useAmount))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                    .hasMessageContaining("유효하지 않은 사용자 ID입니다");

            verify(pointService, never()).use(negativeUserId, useAmount);
        }

        @Test
        @DisplayName("음수의 사용 금액으로 요청 시 BAD_REQUEST 예외가 발생한다")
        void use_whenNegativeAmount() {
            // given
            long userId = 1L;
            long negativeAmount = -100L;
            when(pointService.use(userId, negativeAmount))
                    .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "충전 금액은 0보다 커야 합니다."));

            // when & then
            assertThatThrownBy(() -> pointController.use(userId, negativeAmount))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                    .hasMessageContaining("충전 금액은 0보다 커야 합니다.");

            verify(pointService, never()).use(userId, negativeAmount);
        }

        @Test
        @DisplayName("현재 포인트와 정확히 같은 금액 사용 시 포인트가 0이 된다")
        void shouldUseAllPointsSuccessfully() {
            // given
            long userId = 1L;
            long useAmount = 1000L;

            UserPoint expectedUserPoint = new UserPoint(userId, 0L, System.currentTimeMillis());

            when(pointService.use(userId, useAmount)).thenReturn(expectedUserPoint);

            // when
            UserPoint result = pointController.use(userId, useAmount);

            // then
            assertThat(result.point()).isEqualTo(0L);
            verify(pointService).use(userId, useAmount);
        }
    }
}