package com.test.concurrent.service;

import com.test.concurrent.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponTransactionSaveService {
    private final CouponRepository couponRepository;

    private final RedisTemplate<String, String> redisTemplate;

    @Transactional
    public void saveAll(Long couponId, String suffix) {
        String key = couponId + ":" + suffix;

        ZSetOperations<String, String> command = redisTemplate.opsForZSet();
        Set<String> tx = command.range(key, 0, -1);
        Long sz = command.zCard(key);

        log.info("저장된 트랜잭션(size={}): {}", sz, tx);

        if (sz != null)
            couponRepository.decreaseStock(couponId, sz.intValue());
    }
}
