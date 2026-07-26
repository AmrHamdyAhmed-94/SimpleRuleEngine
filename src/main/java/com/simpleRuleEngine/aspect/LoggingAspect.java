package com.simpleRuleEngine.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    @Pointcut("within(com.simpleRuleEngine.controller.*)")
    public void controllerLayer() {}

    @Pointcut("within(com.simpleRuleEngine.service.*)")
    public void serviceLayer() {}

    @Around("controllerLayer() || serviceLayer()")
    public Object logMethodCall(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        log.info("[ENTRY] {}.{}()", className, methodName);
        try {
            Object result = joinPoint.proceed();
            log.info("[EXIT]  {}.{}()", className, methodName);
            return result;
        } catch (Exception ex) {
            log.error("[EXCEPTION] {}.{}() - {}: {}",
                    className, methodName, ex.getClass().getSimpleName(), ex.getMessage());
            throw ex;
        }
    }
}
