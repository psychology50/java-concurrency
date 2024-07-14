package com.test.concurrent.service;

import com.test.concurrent.domain.Coupon;
import com.test.concurrent.repository.CouponRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageQueueCouponDecreaseService {
    private final CouponRepository couponRepository;
    private final Queue<String> couponQueue = new ConcurrentLinkedQueue<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor(); // 워커 스레드
    private final AtomicInteger messageCount = new AtomicInteger(0);

    @PostConstruct
    public void init() {
        executorService.submit(this::processQueue);
    }

    public void decreaseStockWithMessagingQueue(Long couponId) {
        couponQueue.offer(couponId + ":" + Thread.currentThread().getId());
        messageCount.incrementAndGet();
    }

    private void processQueue() {
        while (!Thread.currentThread().isInterrupted()) {
            String message = couponQueue.poll();

            Long couponId = null;
            if (message != null)
                couponId = Long.parseLong(message.split(":")[0]);

            if (couponId != null) {
                try {
                    Coupon coupon = couponRepository.findById(couponId)
                            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다."));
                    coupon.decreaseStock();
                    couponRepository.save(coupon);
                    messageCount.decrementAndGet();
                    log.info("쿠폰 차감을 완료했습니다. couponId={}", couponId);
                } catch (Exception e) {
                    log.error("쿠폰 차감 중 에러 발생", e);
                    throw e;
                }
            }
        }
    }

    public void waitForCompletion() throws InterruptedException {
        while (messageCount.get() > 0) {
            Thread.sleep(100L);
        }
    }
}
