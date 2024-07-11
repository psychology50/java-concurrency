package com.test.concurrent.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class TimeCountAop {
    @Around("@annotation(com.test.concurrent.aop.TimeCount)")
    public Object timeCount(final ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();

        Object result = joinPoint.proceed();

        long end = System.currentTimeMillis();
        log.debug("수행 시간 : " + (end - start) + "ms");

        return result;
    }
}
