package com.test.concurrent.aop;

import com.test.concurrent.common.CustomSpringELParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.lang.reflect.Method;

/**
 * {@link DistributedLock} 어노테이션을 사용한 메소드에 대한 분산 락 처리를 위한 AOP
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class DistributedLockAspect {
    private static final String REDISSON_LOCK_PREFIX = "LOCK:";

    private final RedissonClient redissonClient;
    private final PlatformTransactionManager transactionManager;

    @Around("@annotation(com.test.concurrent.aop.DistributedLock)")
    public Object lock(final ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        DistributedLock distributedLock = method.getAnnotation(DistributedLock.class);

        String key = REDISSON_LOCK_PREFIX + CustomSpringELParser.getDynamicValue(signature.getParameterNames(), joinPoint.getArgs(), distributedLock.key());
        RLock rLock = redissonClient.getLock(key);

        TransactionStatus status = null;

        try {
            boolean available = rLock.tryLock(distributedLock.waitTime(), distributedLock.leaseTime(), distributedLock.timeUnit());
            if (!available) {
                log.warn("Lock 획득 실패: {} {}", method.getName(), key);
                return false;
            }
            log.info("{} : Redisson Lock 진입 : {} {}", Thread.currentThread().getId(), method.getName(), key);

            // 트랜잭션 정의 설정
            var def = new DefaultTransactionDefinition();
            if (distributedLock.needNewTransaction()) {
                def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            } else {
                def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
            }

            // 트랜잭션 타임아웃을 락의 유효 시간(leaseTime)과 동일하게 설정
            var timeoutSeconds = (int) distributedLock.timeUnit().toSeconds(distributedLock.leaseTime());
            if (timeoutSeconds > 0) {
                // 트랜잭션 타임아웃은 락 유효 시간보다 약간 짧게 설정 (롤백 여유 시간 확보)
                def.setTimeout(Math.max(1, timeoutSeconds - 1));
            }
            log.info("Distributed Lock TTL : {} ms, Transaction Timeout: {} sec", rLock.remainTimeToLive(), def.getTimeout());

            // 트랜잭션 시작
            status = transactionManager.getTransaction(def);

            // 메서드 실행
            Object result = joinPoint.proceed();

            // 트랜잭션 커밋
            transactionManager.commit(status);
            status = null;

            return result;
        } catch (InterruptedException e) {
            if (status != null) {
                transactionManager.rollback(status);
            }

            log.error("{} : Interrupted exception : {}", method.getName(), e.getMessage());
            throw new InterruptedException("Failed to acquire lock: " + key);
        } catch (Throwable e) {
            if (status != null) {
                transactionManager.rollback(status);
            }

            log.error("{} : Error during execution with lock: {} {}", Thread.currentThread().getId(), method.getName(), key);
            throw e;
        } finally {
            try {
                if (rLock.isHeldByCurrentThread()) { // 현재 스레드가 락을 보유하고 있는 경우에만 해제
                    rLock.unlock();
                    log.info("{} : Redisson Lock 해제 : {} {}", Thread.currentThread().getId(), method.getName(), key);;
                } else {
                    log.warn("{} : Redisson Lock 해제 불필요 (현재 스레드가 소유하지 않음) : {} {}", Thread.currentThread().getId(), method.getName(), key);
                }
            } catch (IllegalMonitorStateException ignored) {
                log.error("Redisson lock is already unlocked: {} {}", method.getName(), key);
            }
        }
    }
}
