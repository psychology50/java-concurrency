package com.test.concurrent.service;

import com.test.concurrent.domain.Coupon;
import com.test.concurrent.repository.CouponRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class DistributedLockTimeOverTest {
    @Autowired
    private CouponRepository couponRepository;
    @Autowired
    private CouponTooLongTimeDecreaseService sut;

    @Test
    void should_have_zero_stock_when_coupons_equal_requests() throws InterruptedException {
        // given
        var couponCount = 2;
        var coupon = couponRepository.save(new Coupon("COUPON_001", couponCount));

        var threadCount = 2;
        var executor = Executors.newFixedThreadPool(threadCount);
        var latch = new CountDownLatch(threadCount);

        // when
        for (int i = 0; i < threadCount; ++i) {
            executor.submit(() -> {
                try {
                    sut.run(coupon.getId());
                } finally {
                    latch.countDown();
                }
            });

            Thread.sleep(5000); // 다음 스레드 시작 전 5초 대기
        }
        latch.await();

        // then
        var result = couponRepository.findById(coupon.getId()).get();

        assertThat(result.getAvailableStock()).isZero();
    }
}
