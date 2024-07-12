package com.test.concurrent.service;

import com.test.concurrent.domain.AtomicCoupon;
import com.test.concurrent.domain.Coupon;
import com.test.concurrent.repository.AtomicCouponRepository;
import com.test.concurrent.repository.CouponRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class CouponDecreaseTest {
    @Autowired
    private CouponService couponService;
    @Autowired
    private CouponDecreaseService couponDecreaseService;
    @Autowired
    private AtomicCouponDecreaseService atomicCouponDecreaseService;
    @Autowired
    private CouponRepository couponRepository;
    @Autowired
    private AtomicCouponRepository atomicCouponRepository;

    private Coupon coupon;

    @BeforeEach
    void setUp() {
        coupon = new Coupon("COUPON_001", 300L);
        couponRepository.save(coupon);
    }

    @Test
    @DisplayName("실패 케이스: 동시성 환경에서 300명 쿠폰 차감 테스트")
    void 쿠폰차감_동시성_300명_테스트() throws InterruptedException {
        performConcurrencyTest(
                300,
                coupon.getId(),
                couponDecreaseService::decreaseStock,
                false
        );
    }

    @Test
    @DisplayName("synchronized<Non Tx>: 동시성 환경에서 300명 쿠폰 차감 테스트")
    void 선언적_트랜잭션_없이_synchronized_쿠폰차감_동시성_300명_테스트() throws InterruptedException {
        performConcurrencyTest(
                300,
                coupon.getId(),
                couponDecreaseService::decreaseStockWithSynchronized,
                true
        );
    }

    @Test
    @DisplayName("synchronized<외부 호출>: 동시성 환경에서 300명 쿠폰 차감 테스트")
    void 외부에서_synchronized_쿠폰차감_동시성_300명_테스트() throws InterruptedException {
        performConcurrencyTest(
                300,
                coupon.getId(),
                couponService::decreaseStockWithSynchronized,
                true
        );
    }

    @Test
    @DisplayName("ReentrantLock: 동시성 환경에서 300명 쿠폰 차감 테스트")
    void 내부에서_synchronized_쿠폰차감_동시성_300명_테스트() throws InterruptedException {
        performConcurrencyTest(
                300,
                coupon.getId(),
                couponService::decreaseStockWithReentrantLock,
                true
        );
    }

    @Test
    @DisplayName("Atomic: 동시성 환경에서 300명 쿠폰 차감 테스트")
    void Atomic_쿠폰차감_동시성_300명_테스트() throws InterruptedException {
        AtomicCoupon coupon = new AtomicCoupon("COUPON_001", 300L);
        atomicCouponRepository.save(coupon);

        performConcurrencyTest(
                300,
                coupon.getId(),
                atomicCouponDecreaseService::decreaseStock,
                false
        );
    }

    private void performConcurrencyTest(int threadCount, Long couponId, Consumer<Long> method, boolean expectedZero) throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    method.accept(couponId);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();

        Coupon persistedCoupon = couponRepository.findById(couponId).orElseThrow(IllegalArgumentException::new);
        if (expectedZero) {
            assertThat(persistedCoupon.getAvailableStock()).isZero();
        } else {
            assertThat(persistedCoupon.getAvailableStock()).isNotZero();
        }
        log.debug("잔여 쿠폰 수량: " + persistedCoupon.getAvailableStock());
    }
}
