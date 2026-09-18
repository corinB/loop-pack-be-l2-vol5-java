package com.loopers.application.pay.point;

import com.loopers.domain.pay.point.Point;
import com.loopers.domain.pay.point.PointBill;
import com.loopers.domain.pay.point.PointBillRepository;
import com.loopers.domain.pay.point.PointRepository;
import com.loopers.domain.shared.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
// 포인트 충전 처리 서비스
public class PointService implements ChargePointUseCase {
    private final PointRepository pointRepository;
    private final PointBillRepository pointBillRepository;

    // 잔액 충전 후 충전 기록 저장
    @Override
    @Transactional
    public PointResult execute(PointCommand.Charge command) {
        Point point = pointRepository.findByUserId(command.userId()).orElseThrow();
        point.charge(Money.positive(command.amount()));
        Point saved = pointRepository.save(point);
        pointBillRepository.save(PointBill.charge(command.userId(), command.amount()));
        return new PointResult(saved.getBalance());
    }
}
