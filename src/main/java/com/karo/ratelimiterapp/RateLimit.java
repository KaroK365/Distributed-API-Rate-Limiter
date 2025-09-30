package com.karo.ratelimiterapp;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/*
* Retention -> Annotation available at runtime
* Target -> can only be placed on methods
* */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RateLimit {
    long capacity();
    long refillRate();
}
