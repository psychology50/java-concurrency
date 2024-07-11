package com.test.concurrent.service;

import com.test.concurrent.domain.Coupon;
import com.test.concurrent.repository.CouponRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class CouponDecreaseTest {
    @Autowired
    private CouponDecreaseService couponDecreaseService;
    @Autowired
    private CouponRepository couponRepository;

    private Coupon coupon;

    @BeforeEach
    void setUp() {
        coupon = new Coupon("COUPON_001", 300L);
        couponRepository.save(coupon);
    }

    @Test
    @DisplayName("동시성 환경에서 300명 쿠폰 차감 테스트")
    void 쿠폰차감_동시성_300명_테스트() throws InterruptedException {
        // given
        int threadCount = 300;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    couponDecreaseService.decreaseStock(coupon.getId());
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();

        // then
        Coupon persistedCoupon = couponRepository.findById(coupon.getId()).orElseThrow(IllegalArgumentException::new);
        assertThat(persistedCoupon.getAvailableStock()).isNotZero(); // 잔여 쿠폰 수량은 0이 아니다.
        log.debug("잔여 쿠폰 수량: " + persistedCoupon.getAvailableStock());
    }
}
