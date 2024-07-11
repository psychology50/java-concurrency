package com.test.concurrent.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class CouponService {
    private final CouponDecreaseService couponDecreaseService;
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
}
