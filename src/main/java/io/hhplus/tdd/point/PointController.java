package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/point")
public class PointController {

    private static final Logger log = LoggerFactory.getLogger(PointController.class);
    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    public PointController(UserPointTable userPointTable, PointHistoryTable pointHistoryTable) {
        this.userPointTable = userPointTable;
        this.pointHistoryTable = pointHistoryTable;
    }

    /**
     * TODO - 특정 유저의 포인트를 조회하는 기능을 작성해주세요.
     */
    @GetMapping("{id}")
    public UserPoint point(
            @PathVariable long id
    ) {
        // id 유효성 체크
        validateUserId(id);

        return userPointTable.selectById(id);
    }

    /**
     * TODO - 특정 유저의 포인트 충전/이용 내역을 조회하는 기능을 작성해주세요.
     */
    @GetMapping("{id}/histories")
    public List<PointHistory> history(
            @PathVariable long id
    ) {
        // id 유효성 체크
        validateUserId(id);

        return pointHistoryTable.selectAllByUserId(id);
    }

    /**
     * TODO - 특정 유저의 포인트를 충전하는 기능을 작성해주세요.
     */
    @PatchMapping("{id}/charge")
    public UserPoint charge(
            @PathVariable long id,
            @RequestBody long amount
    ) {
        // id 유효성 체크
        validateUserId(id);
        // amount 유효성 체크
        validateChargeAmount(amount);

        UserPoint currentUserPoint = userPointTable.selectById(id);
        // 포인트 합산
        long newPoint = currentUserPoint.point() + amount;

        // 충전 내역 update
        pointHistoryTable.insert(id, amount, TransactionType.CHARGE, System.currentTimeMillis());

        return userPointTable.insertOrUpdate(id, newPoint);
    }

    /**
     * TODO - 특정 유저의 포인트를 사용하는 기능을 작성해주세요.
     */
    @PatchMapping("{id}/use")
    public UserPoint use(
            @PathVariable long id,
            @RequestBody long amount
    ) {
        // id 유효성 체크
        validateUserId(id);
        // amount 유효성 체크
        validateChargeAmount(amount);

        UserPoint currentUserPoint = userPointTable.selectById(id);
        // 잔고 부족 체크
        if(currentUserPoint.point() < amount) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    String.format("포인트가 부족합니다. 현재 포인트: %d, 요청 포인트: %d",
                            currentUserPoint.point(), amount)
            );
        }
        // 잔고 update
        long newPoint = currentUserPoint.point() - amount;

        // 사용 내역 update
        pointHistoryTable.insert(id, amount, TransactionType.USE, System.currentTimeMillis());

        return userPointTable.insertOrUpdate(id, newPoint);
    }

    private void validateUserId(long id) {
        if (id <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "유효하지 않은 사용자 ID입니다: " + id
            );
        }
    }

    private void validateChargeAmount(long amount) {
        if (amount <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "충전 금액은 0보다 커야 합니다: " + amount
            );
        }
    }
}
