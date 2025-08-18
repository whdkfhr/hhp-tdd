package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.apache.catalina.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PointService {

    private static final Logger log = LoggerFactory.getLogger(PointService.class);
    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    public PointService(UserPointTable userPointTable, PointHistoryTable pointHistoryTable) {
        this.userPointTable = userPointTable;
        this.pointHistoryTable = pointHistoryTable;
    }

    public UserPoint point(long id) {
        if (id <= 0) throw new IllegalArgumentException("잘못된 사용자 ID");

        return userPointTable.selectById(id);
    }

    public List<PointHistory> history(long id) {
        if (id <= 0) throw new IllegalArgumentException("잘못된 사용자 ID");

        return pointHistoryTable.selectAllByUserId(id);
    }

    public UserPoint charge(long id, long amount) {
        if (id <= 0) throw new IllegalArgumentException("잘못된 사용자 ID");
        if (amount <= 0) throw new IllegalArgumentException("잘못된 금액");

        UserPoint userPoint = userPointTable.selectById(id);
        long newPoint = userPoint.point() + amount;

       pointHistoryTable.insert(id, amount, TransactionType.CHARGE, System.currentTimeMillis());
       return userPointTable.insertOrUpdate(id, newPoint);
    }

    public UserPoint use(long id, long amount) {
        if (id <= 0) throw new IllegalArgumentException("잘못된 사용자 ID");
        if (amount <= 0) throw new IllegalArgumentException("잘못된 금액");

        UserPoint userPoint = userPointTable.selectById(id);
        // 잔고 부족 체크
        if(userPoint.point() < amount) {
            throw new IllegalArgumentException("포인트 부족");
        }
        // 잔고 update
        long newPoint = userPoint.point() - amount;
        pointHistoryTable.insert(id, amount, TransactionType.USE, System.currentTimeMillis());
        return userPointTable.insertOrUpdate(id, newPoint);
    }
}
