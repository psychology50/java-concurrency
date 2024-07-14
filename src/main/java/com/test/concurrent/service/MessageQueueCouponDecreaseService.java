package com.test.concurrent.service;

import com.test.concurrent.domain.Coupon;
import com.test.concurrent.repository.CouponRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageQueueCouponDecreaseService {
    private final CouponRepository couponRepository;
    private final BlockingQueue<String> couponQueue = new LinkedBlockingQueue<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor(); // 워커 스레드
    private final AtomicInteger messageCount = new AtomicInteger(300);

    @PostConstruct
    public void init() {
        executorService.submit(this::processQueue);
    }

    public void decreaseStockWithMessagingQueue(Long couponId) {
        couponQueue.offer(couponId + ":" + Thread.currentThread().getId());
    }

    private void processQueue() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                String message = couponQueue.take();
                log.info("메시지를 처리합니다. message={}", message);

                Long couponId = Long.parseLong(message.split(":")[0]);

                Coupon coupon = couponRepository.findById(couponId)
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다."));
                coupon.decreaseStock();
                couponRepository.save(coupon);

                messageCount.decrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("메시지 처리 중 에러 발생", e);
            }
        }
    }

    public void waitForCompletion() throws InterruptedException {
        while (messageCount.get() > 0) {
            Thread.sleep(100L);
        }
    }

    @PreDestroy
    public void close() {
        executorService.shutdownNow();
    }
}
