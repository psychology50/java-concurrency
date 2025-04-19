package com.test.concurrent.service;

import com.test.concurrent.aop.DistributedLock;
import com.test.concurrent.domain.Coupon;
import com.test.concurrent.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponTooLongTimeDecreaseService {
    private final CouponRepository couponRepository;

    @DistributedLock(key = "'like_' + #couponId", waitTime = 5L, leaseTime = 5L, needNewTransaction = true)
    public void mustFailRun(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다."));

        // 10초 대기 (락 만료 시간인 5초보다 오래 걸림)
        try {
            log.info("Sleeping for 10 seconds...");
            Thread.sleep(10000);
            log.info("Woke up after sleep");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread interrupted", e);
        }

        coupon.decreaseStock();
    }

    @DistributedLock(key = "'like_' + #couponId", waitTime = 5L, leaseTime = 5L, needNewTransaction = true)
    public void run(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다."));

        coupon.decreaseStock();
    }
}
