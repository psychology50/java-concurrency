package com.test.concurrent.service;

import com.test.concurrent.domain.AtomicCoupon;
import com.test.concurrent.repository.AtomicCouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AtomicCouponDecreaseService {
    private final AtomicCouponRepository atomicCouponRepository;

    @Transactional
    public void decreaseStock(Long couponId) {
        AtomicCoupon coupon = atomicCouponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다."));

        coupon.decreaseStock();
    }
}
