package com.karo.ratelimiterapp;

import java.time.Clock;

public class TokenBucket {
    // private fields for capacity (long), refillRate (long, tokens per second), lastRefillTimestamp (long),
    // and currentTokens (long).
    private long capacity;
    private long refillRate;
    private long lastRefillTimeStamp;
    private long currentTokens;
    private Clock clock;

    public TokenBucket(long capacity, long refillRate, Clock clock) {
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.lastRefillTimeStamp = clock.millis();
        this.clock = clock;
        currentTokens = capacity;
    }

    private void refill(){
        //Get Current Time -> Calculate time passed -> token adding formula ->
        // set current tokens -> finally lastrefill to current time
        long currentTime = clock.millis();
        long timePassed = currentTime - lastRefillTimeStamp;
        long numberOfTokensToAdd = (timePassed * refillRate) / 1000;
        currentTokens = Math.min(capacity, currentTokens + numberOfTokensToAdd);
        lastRefillTimeStamp = currentTime;
    }

    public synchronized boolean tryConsume(){
        // Refill -> if token more than 0 consume else false;
        refill();
        if(currentTokens > 0){
            currentTokens--;
            return true;
        }
        return false;
    }

    public void setClock(Clock clock) {
        this.clock = clock;
    }
}
