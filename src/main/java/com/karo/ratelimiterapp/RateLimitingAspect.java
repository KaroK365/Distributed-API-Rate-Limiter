package com.karo.ratelimiterapp;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Aspect
public class RateLimitingAspect {
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    @Around("@annotation(rateLimit)")
    public Object rateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
         MethodSignature signature = (MethodSignature) joinPoint.getSignature();
         String methodKey = signature.toLongString();

         TokenBucket tokenBucket = buckets.computeIfAbsent(methodKey, key->{
            return new TokenBucket(rateLimit.capacity(), rateLimit.refillRate(), Clock.systemUTC());
         });

         if(tokenBucket.tryConsume()){
             return joinPoint.proceed();
         } else{
             return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("Too Many Requests");
         }
    }
}
