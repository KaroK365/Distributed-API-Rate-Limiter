package com.karo.ratelimiterapp;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiController {
    @GetMapping("/api/unlimited")
    public ResponseEntity<String> getUnlimited(){
        return ResponseEntity.ok("This endpoint is Unlimited");
    }

    @GetMapping("/api/limited")
    @RateLimit(capacity = 10, refillRate = 1)
    public ResponseEntity<String> getLimited(){
        return ResponseEntity.ok("This endpoint is Limited");
    }
}
