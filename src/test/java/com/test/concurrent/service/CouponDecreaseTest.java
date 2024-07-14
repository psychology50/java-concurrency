package com.test.concurrent.service;

import com.test.concurrent.domain.AtomicCoupon;
import com.test.concurrent.domain.Coupon;
import com.test.concurrent.domain.OptimisticCoupon;
import com.test.concurrent.repository.AtomicCouponRepository;
import com.test.concurrent.repository.CouponRepository;
import com.test.concurrent.repository.OptimisticCouponRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
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
    private CouponRepository couponRepository;
    @Autowired
    private AtomicCouponRepository atomicCouponRepository;
    @Autowired
    private OptimisticCouponRepository optimisticCouponRepository;
    @Autowired
    private MessageQueueCouponDecreaseService messageQueueCouponDecreaseService;

    @Autowired
    private CouponTransactionSaveService couponTransactionSaveService;

    private static final int THREAD_COUNT = 700;
    private static final long COUPON_COUNT = 700L;

    private Coupon coupon;

    @Test
    @DisplayName("실패 케이스: 동시성 환경에서 300명 쿠폰 차감 테스트")
    void 쿠폰차감_동시성_300명_테스트() throws InterruptedException {
        Coupon coupon = new Coupon("COUPON_001", COUPON_COUNT);
        couponRepository.save(coupon);

        performConcurrencyTest(
                THREAD_COUNT,
                coupon.getId(),
                couponDecreaseService::decreaseStock
        );

        Coupon persistedCoupon = couponRepository.findById(coupon.getId()).orElseThrow(IllegalArgumentException::new);
        assertThat(persistedCoupon.getAvailableStock()).isNotZero();
        log.debug("잔여 쿠폰 수량: " + persistedCoupon.getAvailableStock());
    }

    @Test
    @DisplayName("synchronized<Non Tx>: 동시성 환경에서 300명 쿠폰 차감 테스트")
    void 선언적_트랜잭션_없이_synchronized_쿠폰차감_동시성_300명_테스트() throws InterruptedException {
        Coupon coupon = new Coupon("COUPON_001", COUPON_COUNT);
        couponRepository.save(coupon);

        performConcurrencyTest(
                THREAD_COUNT,
                coupon.getId(),
                couponDecreaseService::decreaseStockWithSynchronized
        );

        Coupon persistedCoupon = couponRepository.findById(coupon.getId()).orElseThrow(IllegalArgumentException::new);
        assertThat(persistedCoupon.getAvailableStock()).isZero();
        log.debug("잔여 쿠폰 수량: " + persistedCoupon.getAvailableStock());
    }

    @Test
    @DisplayName("synchronized<외부 호출>: 동시성 환경에서 300명 쿠폰 차감 테스트")
    void 외부에서_synchronized_쿠폰차감_동시성_300명_테스트() throws InterruptedException {
        Coupon coupon = new Coupon("COUPON_001", COUPON_COUNT);
        couponRepository.save(coupon);

        performConcurrencyTest(
                THREAD_COUNT,
                coupon.getId(),
                couponService::decreaseStockWithSynchronized
        );

        Coupon persistedCoupon = couponRepository.findById(coupon.getId()).orElseThrow(IllegalArgumentException::new);
        assertThat(persistedCoupon.getAvailableStock()).isZero();
        log.debug("잔여 쿠폰 수량: " + persistedCoupon.getAvailableStock());
    }

    @Test
    @DisplayName("ReentrantLock: 동시성 환경에서 300명 쿠폰 차감 테스트")
    void 내부에서_synchronized_쿠폰차감_동시성_300명_테스트() throws InterruptedException {
        Coupon coupon = new Coupon("COUPON_001", COUPON_COUNT);
        couponRepository.save(coupon);

        performConcurrencyTest(
                THREAD_COUNT,
                coupon.getId(),
                couponService::decreaseStockWithReentrantLock
        );

        Coupon persistedCoupon = couponRepository.findById(coupon.getId()).orElseThrow(IllegalArgumentException::new);
        assertThat(persistedCoupon.getAvailableStock()).isZero();
        log.debug("잔여 쿠폰 수량: " + persistedCoupon.getAvailableStock());
    }

    @Test
    @DisplayName("실패 테스트: Atomic & Timestamp: 동시성 환경에서 300명 쿠폰 차감 테스트")
    void Atomic_쿠폰차감_동시성_300명_테스트() throws InterruptedException {
        AtomicCoupon coupon = new AtomicCoupon("COUPON_001", COUPON_COUNT);
        atomicCouponRepository.save(coupon);

        performConcurrencyTest(
                THREAD_COUNT,
                coupon.getId(),
                couponService::decreaseStockWithAtomic
        );

        AtomicCoupon persistedCoupon = atomicCouponRepository.findById(coupon.getId()).orElseThrow(IllegalArgumentException::new);
        assertThat(persistedCoupon.getAvailableStock().get()).isNotZero();
        log.debug("잔여 쿠폰 수량: " + persistedCoupon.getAvailableStock());
    }

    @Test
    @DisplayName("OLock & CAS: 동시성 환경에서 300명 쿠폰 차감 테스트")
    void OLock_CAS_쿠폰차감_동시성_300명_테스트() throws InterruptedException {
        OptimisticCoupon coupon = new OptimisticCoupon("COUPON_001", COUPON_COUNT);
        optimisticCouponRepository.save(coupon);

        performConcurrencyTest(
                THREAD_COUNT,
                coupon.getId(),
                couponService::decreaseStockWithOLockAndCAS
        );

        OptimisticCoupon persistedCoupon = optimisticCouponRepository.findById(coupon.getId()).orElseThrow(IllegalArgumentException::new);
        assertThat(persistedCoupon.getAvailableStock()).isZero();
        log.debug("잔여 쿠폰 수량: " + persistedCoupon.getAvailableStock());
    }

    @Test
    @DisplayName("PLock: 동시성 환경에서 300명 쿠폰 차감 테스트")
    void 비관적_락_쿠폰차감_동시성_300명_테스트() throws InterruptedException {
        Coupon coupon = new Coupon("COUPON_001", COUPON_COUNT);
        couponRepository.save(coupon);

        performConcurrencyTest(
                THREAD_COUNT,
                coupon.getId(),
                couponDecreaseService::decreaseStockWithPLock
        );

        Coupon persistedCoupon = couponRepository.findById(coupon.getId()).orElseThrow(IllegalArgumentException::new);
        assertThat(persistedCoupon.getAvailableStock()).isZero();
        log.debug("잔여 쿠폰 수량: " + persistedCoupon.getAvailableStock());
    }

    @Test
    @DisplayName("DistributedLock: 동시성 환경에서 300명 쿠폰 차감 테스트")
    void 분산_락_쿠폰차감_동시성_300명_테스트() throws InterruptedException {
        Coupon coupon = new Coupon("COUPON_001", COUPON_COUNT);
        couponRepository.save(coupon);

        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            executorService.submit(() -> {
                try {
                    couponDecreaseService.decreaseStockWithDistributedLock(coupon.getId(), coupon.getName());
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();

        Coupon persistedCoupon = couponRepository.findById(coupon.getId()).orElseThrow(IllegalArgumentException::new);
        assertThat(persistedCoupon.getAvailableStock()).isZero();
        log.debug("잔여 쿠폰 수량: " + persistedCoupon.getAvailableStock());
    }

    @Test
    @DisplayName("Sorted Set: 동시성 환경에서 400명 쿠폰 차감 테스트")
    void 정렬_집합_쿠폰차감_동시성_400명_테스트() throws InterruptedException {
        Coupon coupon = new Coupon("COUPON_001", COUPON_COUNT);
        couponRepository.save(coupon);

        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            executorService.submit(() -> {
                try {
                    couponDecreaseService.registerCouponRequest(coupon.getId(), coupon.getName());
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();

        couponTransactionSaveService.saveAll(coupon.getId(), coupon.getName());

        Coupon persistedCoupon = couponRepository.findById(coupon.getId()).orElseThrow(IllegalArgumentException::new);
        assertThat(persistedCoupon.getAvailableStock()).isZero();
        log.debug("잔여 쿠폰 수량: " + persistedCoupon.getAvailableStock());
    }

    @Test
    @DisplayName("Messaging Queue: 동시성 환경에서 300명 쿠폰 차감 테스트")
    void 메시징_큐_쿠폰차감_동시성_300명_테스트() throws InterruptedException {
        Coupon coupon = new Coupon("COUPON_001", COUPON_COUNT);
        couponRepository.save(coupon);

        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            executorService.submit(() -> {
                try {
                    messageQueueCouponDecreaseService.decreaseStockWithMessagingQueue(coupon.getId());
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();

        messageQueueCouponDecreaseService.waitForCompletion();

        Coupon persistedCoupon = couponRepository.findById(coupon.getId()).orElseThrow(IllegalArgumentException::new);
        assertThat(persistedCoupon.getAvailableStock()).isZero();
        log.debug("잔여 쿠폰 수량: " + persistedCoupon.getAvailableStock());
    }

    private void performConcurrencyTest(int threadCount, Long couponId, Consumer<Long> method) throws InterruptedException {
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
    }
}
