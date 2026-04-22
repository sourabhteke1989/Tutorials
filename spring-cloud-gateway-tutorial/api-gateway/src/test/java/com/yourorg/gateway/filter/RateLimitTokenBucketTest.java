package com.yourorg.gateway.filter;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RateLimitTokenBucketTest {

    @Test
    void shouldConsumeTokens() {
        RateLimitFilter.TokenBucket bucket = new RateLimitFilter.TokenBucket(5, 60);
        for (int i = 0; i < 5; i++) {
            assertTrue(bucket.tryConsume());
        }
        assertFalse(bucket.tryConsume());
    }

    @Test
    void shouldReturnCorrectLimit() {
        RateLimitFilter.TokenBucket bucket = new RateLimitFilter.TokenBucket(10, 60);
        assertEquals(10, bucket.getLimit());
    }

    @Test
    void shouldReturnCorrectRemaining() {
        RateLimitFilter.TokenBucket bucket = new RateLimitFilter.TokenBucket(5, 60);
        assertEquals(5, bucket.getRemaining());
        bucket.tryConsume();
        assertEquals(4, bucket.getRemaining());
    }

    @Test
    void shouldReturnPositiveResetEpoch() {
        RateLimitFilter.TokenBucket bucket = new RateLimitFilter.TokenBucket(5, 60);
        assertTrue(bucket.getResetEpochSeconds() > 0);
    }

    @Test
    void shouldReturnPositiveRetryAfter() {
        RateLimitFilter.TokenBucket bucket = new RateLimitFilter.TokenBucket(5, 60);
        assertTrue(bucket.getRetryAfterSeconds() >= 1);
    }
}
