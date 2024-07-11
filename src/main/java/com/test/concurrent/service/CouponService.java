package com.test.concurrent.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CouponService {
    private final CouponDecreaseService couponDecreaseService;

    public synchronized void decreaseStock(Long couponId) {
        couponDecreaseService.decreaseStock(couponId);
    }
}
