package com.taskbackend.taskbackend.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    @Around("execution(* com.taskbackend.taskbackend.service..*(..))")
    public Object logServiceExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long elapsedMs = System.currentTimeMillis() - startTime;
            log.info("{}.{} executed in {} ms", className, methodName, elapsedMs);
            return result;
        } catch (Throwable ex) {
            long elapsedMs = System.currentTimeMillis() - startTime;
            log.error("{}.{} failed after {} ms - {}: {}", className, methodName, elapsedMs,
                    ex.getClass().getSimpleName(), ex.getMessage());
            throw ex;
        }
    }
}
