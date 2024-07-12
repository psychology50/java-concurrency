package com.test.concurrent.service;

import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class CouponService {
    private final CouponDecreaseService couponDecreaseService;
    private final AtomicCouponDecreaseService atomicCouponDecreaseService;

    private final ReentrantLock lock = new ReentrantLock();

    public synchronized void decreaseStockWithSynchronized(Long couponId) {
        couponDecreaseService.decreaseStock(couponId);
    }

    public void decreaseStockWithReentrantLock(Long couponId) {
        lock.lock();
        try {
            couponDecreaseService.decreaseStock(couponId);
        } finally {
            lock.unlock();
        }
    }

    public void decreaseStockWithOLockAndCAS(Long couponId) {
        for (int attempt = 0; attempt < 100; attempt++) {
            try {
                atomicCouponDecreaseService.decreaseStockOLockWithCAS(couponId);
                return;
            } catch (ObjectOptimisticLockingFailureException | OptimisticLockException e) {
                try {
                    Thread.sleep(ThreadLocalRandom.current().nextInt(10, 100));
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        throw new IllegalStateException("재시도 횟수 초과");
    }
}
