package com.test.concurrent.service;

import com.test.concurrent.domain.Coupon;
import com.test.concurrent.repository.CouponRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

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
    @DisplayName("Happy Path: 두 작업 모두 락 방출 시간 이내에 처리되는 경우 재고 없음")
    void should_have_zero_stock_when_coupons_equal_requests() throws InterruptedException {
        // given
        var couponCount = 2;
        var coupon = couponRepository.save(new Coupon("COUPON_001", couponCount));

        // when
        executeTasksConcurrently(
                Arrays.asList(
                        couponId -> sut.run(couponId),
                        couponId -> sut.run(couponId)
                ),
                0, // 즉시 모든 태스크 실행
                coupon.getId()
        );

        // then
        var result = couponRepository.findById(coupon.getId()).get();

        assertThat(result.getAvailableStock()).isZero();
    }

    @Test
    @DisplayName("두 작업 모두 락 점유 시간 내에 처리하지 못한 경우 재고 유지")
    void should_have_two_stock_when_all_tasks_exceeding_timeout_limit() throws InterruptedException {
        // given
        var couponCount = 2;
        var coupon = couponRepository.save(new Coupon("COUPON_001", couponCount));

        // when
        executeTasksConcurrently(
                Arrays.asList(
                        couponId -> sut.mustFailRun(couponId),
                        couponId -> sut.mustFailRun(couponId)
                ),
                5000, // 5초 간격으로 태스크 실행
                coupon.getId()
        );

        // then
        var result = couponRepository.findById(coupon.getId()).get();

        assertEquals(2, result.getAvailableStock(), "두 작업 모두 반드시 실패해야 합니다.");
    }

    @Test
    @DisplayName("선행 작업이 락 점유 시간 내에 처리 실패 시 롤백 후, 후행 작업은 성공하여 재고는 1개")
    void should_fail_tasks_exceeding_timeout_limit() throws InterruptedException {
        // given
        var couponCount = 2;
        var coupon = couponRepository.save(new Coupon("COUPON_001", couponCount));

        // when
        executeTasksConcurrently(
                Arrays.asList(
                        couponId -> sut.mustFailRun(couponId),
                        couponId -> sut.run(couponId)
                ),
                5000, // 5초 간격으로 태스크 실행
                coupon.getId()
        );

        // then
        var result = couponRepository.findById(coupon.getId()).get();

        assertEquals(1, result.getAvailableStock(), "하나의 작업만이 성공해야 합니다.");
    }

    private void executeTasksConcurrently(List<Consumer<Long>> tasks, long delayMillis, Long couponId)
            throws InterruptedException {
        int threadCount = tasks.size();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final Consumer<Long> task = tasks.get(i);

            executor.submit(() -> {
                try {
                    task.accept(couponId);
                } catch (Exception e) {
                    log.error("Task execution failed", e);
                } finally {
                    latch.countDown();
                }
            });

            if (delayMillis > 0 && i < threadCount - 1) {
                Thread.sleep(delayMillis); // 다음 태스크 시작 전 지정된 시간만큼 대기
            }
        }

        latch.await();
        executor.shutdown();
    }
}
