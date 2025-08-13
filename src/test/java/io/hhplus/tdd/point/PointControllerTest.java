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
    private UserPointTable userPointTable;

    @Mock
    private PointHistoryTable pointHistoryTable;

    private PointController pointController;

    @BeforeEach
    void setUp() {
        pointController = new PointController(userPointTable, pointHistoryTable);
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
            when(userPointTable.selectById(userId)).thenReturn(expectedUserPoint);

            // when
            UserPoint result = pointController.point(userId);

            // then
            assertThat(result).isEqualTo(expectedUserPoint);
            verify(userPointTable).selectById(userId);
        }

        @Test
        @DisplayName("0 이하의 ID로 포인트 조회 시 BAD_REQUEST 예외가 발생한다")
        void point_whenInvalidId() {
            // given
            long invalidUserId = -1L;

            // when & then
            assertThatThrownBy(() -> pointController.point(invalidUserId))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                    .hasMessageContaining("유효하지 않은 사용자 ID입니다: " + invalidUserId);

            verify(userPointTable, never()).selectById(anyLong());
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
            when(pointHistoryTable.selectAllByUserId(userId)).thenReturn(expectedHistories);

            // when
            List<PointHistory> result = pointController.history(userId);

            // then
            assertThat(result).isEqualTo(expectedHistories);
            assertThat(result).hasSize(2);
            verify(pointHistoryTable).selectAllByUserId(userId);
        }

        @Test
        @DisplayName("내역이 없는 사용자 ID로 조회 시 빈 리스트를 반환한다")
        void history_whenNoHistory() {
            // given
            long userId = 999L;
            when(pointHistoryTable.selectAllByUserId(userId)).thenReturn(List.of());

            // when
            List<PointHistory> result = pointController.history(userId);

            // then
            assertThat(result).isEmpty();
            verify(pointHistoryTable).selectAllByUserId(userId);
        }

        @Test
        @DisplayName("0 이하의 ID로 사용내역 조회 시 BAD_REQUEST 예외가 발생한다")
        void history_whenInvalidId() {
            // given
            long invalidUserId = -1L;

            // when & then
            assertThatThrownBy(() -> pointController.history(invalidUserId))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                    .hasMessageContaining("유효하지 않은 사용자 ID입니다: " + invalidUserId);

            verify(pointHistoryTable, never()).selectAllByUserId(anyLong());
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

            when(userPointTable.selectById(userId)).thenReturn(currentUserPoint);
            when(userPointTable.insertOrUpdate(userId, expectedNewPoint)).thenReturn(expectedUserPoint);

            // when
            UserPoint result = pointController.charge(userId, chargeAmount);

            // then
            assertThat(result.point()).isEqualTo(expectedNewPoint);
            verify(userPointTable).selectById(userId);
            verify(userPointTable).insertOrUpdate(userId, expectedNewPoint);
            verify(pointHistoryTable).insert(eq(userId), eq(chargeAmount), eq(TransactionType.CHARGE), anyLong());
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

            verify(userPointTable, never()).selectById(anyLong());
            verify(userPointTable, never()).insertOrUpdate(anyLong(), anyLong());
            verify(pointHistoryTable, never()).insert(anyLong(), anyLong(), any(), anyLong());
        }

        @Test
        @DisplayName("0 이하의 충전 금액으로 요청 시 BAD_REQUEST 예외가 발생한다")
        void charge_whenInvalidAmount() {
            // given
            long userId = 1L;
            long invalidAmount = -100L;

            // when & then
            assertThatThrownBy(() -> pointController.charge(userId, invalidAmount))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                    .hasMessageContaining("충전 금액은 0보다 커야 합니다: " + invalidAmount);

            verify(userPointTable, never()).selectById(anyLong());
            verify(userPointTable, never()).insertOrUpdate(anyLong(), anyLong());
            verify(pointHistoryTable, never()).insert(anyLong(), anyLong(), any(), anyLong());
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

            UserPoint currentUserPoint = new UserPoint(userId, currentPoint, System.currentTimeMillis());
            UserPoint expectedUserPoint = new UserPoint(userId, expectedNewPoint, System.currentTimeMillis());

            when(userPointTable.selectById(userId)).thenReturn(currentUserPoint);
            when(userPointTable.insertOrUpdate(userId, expectedNewPoint)).thenReturn(expectedUserPoint);

            // when
            UserPoint result = pointController.use(userId, useAmount);

            // then
            assertThat(result.point()).isEqualTo(expectedNewPoint);
            verify(userPointTable).selectById(userId);
            verify(userPointTable).insertOrUpdate(userId, expectedNewPoint);
            verify(pointHistoryTable).insert(eq(userId), eq(useAmount), eq(TransactionType.USE), anyLong());
        }

        @Test
        @DisplayName("잔고 부족 시 BAD_REQUEST 예외가 발생한다")
        void use_whenInsufficientBalance() {
            // given
            long userId = 1L;
            long useAmount = 1500L;
            long currentPoint = 1000L;

            UserPoint currentUserPoint = new UserPoint(userId, currentPoint, System.currentTimeMillis());
            when(userPointTable.selectById(userId)).thenReturn(currentUserPoint);

            // when & then
            assertThatThrownBy(() -> pointController.use(userId, useAmount))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                    .hasMessageContaining("포인트가 부족합니다")
                    .hasMessageContaining("현재 포인트: " + currentPoint)
                    .hasMessageContaining("요청 포인트: " + useAmount);

            verify(userPointTable).selectById(userId);
            verify(userPointTable, never()).insertOrUpdate(anyLong(), anyLong());
            verify(pointHistoryTable, never()).insert(anyLong(), anyLong(), any(), anyLong());
        }

        @Test
        @DisplayName("0 이하의 ID로 사용 시 BAD_REQUEST 예외가 발생한다")
        void use_whenInvalidId() {
            // given
            long invalidUserId = -1L;
            long useAmount = 500L;

            // when & then
            assertThatThrownBy(() -> pointController.use(invalidUserId, useAmount))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                    .hasMessageContaining("유효하지 않은 사용자 ID입니다: " + invalidUserId);

            verify(userPointTable, never()).selectById(anyLong());
        }

        @Test
        @DisplayName("0 이하의 사용 금액으로 요청 시 BAD_REQUEST 예외가 발생한다")
        void use_whenInvalidAmount() {
            // given
            long userId = 1L;
            long invalidAmount = -100L;

            // when & then
            assertThatThrownBy(() -> pointController.use(userId, invalidAmount))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
                    .hasMessageContaining("충전 금액은 0보다 커야 합니다: " + invalidAmount);

            verify(userPointTable, never()).selectById(anyLong());
        }

        @Test
        @DisplayName("현재 포인트와 정확히 같은 금액 사용 시 포인트가 0이 된다")
        void shouldUseAllPointsSuccessfully() {
            // given
            long userId = 1L;
            long currentPoint = 1000L;
            long useAmount = currentPoint; // 전액 사용

            UserPoint currentUserPoint = new UserPoint(userId, currentPoint, System.currentTimeMillis());
            UserPoint expectedUserPoint = new UserPoint(userId, 0L, System.currentTimeMillis());

            when(userPointTable.selectById(userId)).thenReturn(currentUserPoint);
            when(userPointTable.insertOrUpdate(userId, 0L)).thenReturn(expectedUserPoint);

            // when
            UserPoint result = pointController.use(userId, useAmount);

            // then
            assertThat(result.point()).isEqualTo(0L);
            verify(userPointTable).selectById(userId);
            verify(userPointTable).insertOrUpdate(userId, 0L);
            verify(pointHistoryTable).insert(eq(userId), eq(useAmount), eq(TransactionType.USE), anyLong());
        }
    }
}