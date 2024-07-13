package com.test.concurrent.aop;

import org.aspectj.lang.ProceedingJoinPoint;

public interface CallTransaction {
    Object proceed(ProceedingJoinPoint joinPoint) throws Throwable;
}

