package com.karo.ratelimiterapp;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class TokenBucketTest {
    @Test
    public void testTokenBucketLogic() {

        // Using clock
        Instant initialTime = Instant.parse("2025-09-30T18:30:00Z");
        Clock fixedClock = Clock.fixed(initialTime, ZoneOffset.UTC);

        //Creating a tokenbucket
        TokenBucket tokenBucket = new TokenBucket(10, 1, fixedClock);

        //Consuming 10
        for(int i = 0; i < 10; i++){
            boolean wasSuccessful = tokenBucket.tryConsume();
            Assertions.assertTrue(wasSuccessful);
        }

        Assertions.assertFalse(tokenBucket.tryConsume()); //Consuming 1 more, should fail

        //Waiting for refill for 5 sec
        // Future Clock
        Clock futureClock = Clock.fixed(initialTime.plusSeconds(5), ZoneOffset.UTC);
        tokenBucket.setClock(futureClock);
        //Consuming 5 tokens
        for(int i = 0; i < 5; i++){
            boolean wasSuccessful = tokenBucket.tryConsume();
            Assertions.assertTrue(wasSuccessful);
        }

        Assertions.assertFalse(tokenBucket.tryConsume()); // Should fail
    }
}