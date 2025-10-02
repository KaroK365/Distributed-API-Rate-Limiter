package com.karo.ratelimiterapp;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scripting.ScriptSource;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Collections;

@Component
@Aspect
public class RateLimitingAspect {
    //Redis Template
    private final RedisTemplate<String, Object> redisTemplate;
    private final DefaultRedisScript<Long> redisScript;

    public RateLimitingAspect(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.redisScript = new DefaultRedisScript<>();
        this.redisScript.setLocation(new ClassPathResource("ratelimit.lua"));
        this.redisScript.setResultType(Long.class);
    }

    @Around("@annotation(rateLimit)")
    public Object rateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
         MethodSignature signature = (MethodSignature) joinPoint.getSignature();
         String methodKey = "ratelimit:" + signature.toLongString();

         Long result = redisTemplate.execute(
                 redisScript,
                 Collections.singletonList(methodKey),
                 rateLimit.refillRate(),
                 rateLimit.capacity(),
                 System.currentTimeMillis(),
                 1L
         );

         if(result!=null && result == 1){
             return joinPoint.proceed();
         } else{
             return new ResponseEntity<>("Too Many Requests",HttpStatus.TOO_MANY_REQUESTS);
         }
    }
}
