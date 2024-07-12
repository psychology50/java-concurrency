package com.test.concurrent.service;

import com.test.concurrent.domain.AtomicCoupon;
import com.test.concurrent.domain.OptimisticCoupon;
import com.test.concurrent.repository.AtomicCouponRepository;
import com.test.concurrent.repository.OptimisticCouponRepository;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class AtomicCouponDecreaseService {
    private final AtomicCouponRepository atomicCouponRepository;
    private final OptimisticCouponRepository optimisticCouponRepository;

    private AtomicReference<LocalDateTime> lastUpdatedTime = new AtomicReference<>(LocalDateTime.now().minusSeconds(100L));

    @Transactional
    public void decreaseStock(Long couponId) {
        LocalDateTime now = LocalDateTime.now();

        if (Duration.between(lastUpdatedTime.get(), now).toMillis() < 100L) {
            throw new IllegalStateException("중복 요청 방지");
        }

        if (!lastUpdatedTime.compareAndSet(lastUpdatedTime.get(), now)) {
            throw new IllegalStateException("동시 요청 감지");
        }

        AtomicCoupon coupon = atomicCouponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다."));

        coupon.decreaseStock();
    }

    @Transactional
    public void decreaseStockOLockWithCAS(Long couponId) {
        OptimisticCoupon coupon = optimisticCouponRepository.findByIdWithOLock(couponId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다."));

        coupon.decreaseStock();
    }
}
