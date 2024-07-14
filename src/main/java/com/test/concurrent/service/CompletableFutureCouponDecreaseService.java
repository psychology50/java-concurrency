package com.test.concurrent.service;

import com.test.concurrent.domain.Coupon;
import com.test.concurrent.repository.CouponRepository;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompletableFutureCouponDecreaseService {
    private final CouponRepository couponRepository;
    private final ExecutorService executorService = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors()
    );

    @Transactional
    public CompletableFuture<Void> decreaseStockWithCompletableFuture(Long couponId) {
        return CompletableFuture.runAsync(() -> {
            log.info("쿠폰 차감을 시작합니다. couponId={}", couponId);
            try {
                Coupon coupon = couponRepository.findById(couponId)
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다."));
                coupon.decreaseStock();
                couponRepository.save(coupon);
                log.info("쿠폰 차감을 완료했습니다. couponId={}", couponId);
            } catch (Exception e) {
                log.error("쿠폰 차감 중 에러 발생", e);
                throw e;
            }
        }, executorService);
    }

    @PreDestroy
    public void shutdown() {
        executorService.shutdownNow();
    }
}
