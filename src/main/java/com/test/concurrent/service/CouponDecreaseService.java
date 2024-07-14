package com.test.concurrent.service;

import com.test.concurrent.aop.DistributedLock;
import com.test.concurrent.domain.Coupon;
import com.test.concurrent.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponDecreaseService {
    private final CouponRepository couponRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @Transactional
    public void decreaseStock(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다."));

        coupon.decreaseStock();
    }

    public synchronized void decreaseStockWithSynchronized(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다."));

        coupon.decreaseStock();
        couponRepository.save(coupon);
    }

    @Transactional
    public void decreaseStockWithPLock(Long couponId) {
        Coupon coupon = couponRepository.findByIdWithPLock(couponId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다."));

        coupon.decreaseStock();
    }

    @DistributedLock(key = "#key")
    public void decreaseStockWithDistributedLock(Long couponId, String key) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다."));

        coupon.decreaseStock();
    }

    public boolean registerCouponRequest(Long couponId, String key) {
        String threadId = String.valueOf(Thread.currentThread().getId());
        String maxRequestCount = "300";

        String luaScript =
                "local count = redis.call('ZCARD', KEYS[1]) " +
                        "if count < tonumber(ARGV[2]) then " +
                        "    redis.call('ZADD', KEYS[1], tonumber(ARGV[3]), ARGV[1]) " +
                        "    return 1 " +
                        "else " +
                        "    return 0 " +
                        "end";

        RedisScript<Long> script = RedisScript.of(luaScript, Long.class);
        Long result = redisTemplate.execute(script,
                Collections.singletonList(couponId + ":" + key),
                threadId,
                maxRequestCount,
                String.valueOf(System.currentTimeMillis())
        );

        if (result == 1) {
            log.info("쿠폰 {} 요청이 등록되었습니다. (Thread ID: {})", couponId, threadId);
            return true;
        } else {
            log.info("쿠폰 {} 요청 한도에 도달했습니다. (Thread ID: {})", couponId, threadId);
            return false;
        }
    }
}
