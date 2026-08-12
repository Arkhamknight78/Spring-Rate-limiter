package com.example.Rate.Limiter.service;

import com.example.Rate.Limiter.config.RateLimiterProperties;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

//Store token bucket state in redis
//manage token per client
//handle token refill based on time
@Service
@RequiredArgsConstructor
public class RedisTokenBucketService {
    private final JedisPool jedisPool;

    private final RateLimiterProperties rateLimiterProperties;

    private final String TOKENS_KEY_PREFIX = "rate_limiter:token:";
    private static String LAST_REFILL_KEY_PREFIX = "rate_limiter:last_refill:";

    //PATTERN
    //rate_limiter:{type}:{clientId}

    //rate_limiter:token:192.168... > current token count
    //rate_limiter:last_refill:192.168... > Last refill timestamp

    public long getCapacity(){
        return rateLimiterProperties.getCapacity();
    }
    public boolean isAllowed(String clientId) {
        String tokenKey = TOKENS_KEY_PREFIX + clientId;

        try (Jedis jedis = jedisPool.getResource()) {
            refillTokens(clientId, jedis);

            String tokenStr = jedis.get(tokenKey);

            long currentTokens = tokenStr != null ? Long.parseLong(tokenStr) : rateLimiterProperties.getCapacity();

            if (currentTokens <= 0) {
                return false;
            }

            long decremented = jedis.decr(tokenKey);
            return decremented >= 0;
        }
    }

    public long getAvailableTokens(String clientId) {
        String tokenKey = TOKENS_KEY_PREFIX + clientId;

        try (Jedis jedis = jedisPool.getResource()) {
            //refills token
            refillTokens(clientId, jedis);

            String tokenStr = jedis.get(tokenKey);
            return tokenStr != null ? Long.parseLong(tokenStr) : rateLimiterProperties.getCapacity();
        }
    }

    private void refillTokens(String clientId, Jedis jedis) {
        String tokenKey = TOKENS_KEY_PREFIX + clientId;
        String lastRefillKey = LAST_REFILL_KEY_PREFIX + clientId;
        long now = System.currentTimeMillis();

        String lastRefillTime = jedis.get(lastRefillKey);

        if (lastRefillTime == null) {
            jedis.set(tokenKey, String.valueOf(rateLimiterProperties.getCapacity()));
            jedis.set(lastRefillKey, String.valueOf(now));
            return;
        }
        long lastRefill = Long.parseLong(lastRefillTime);
        long elapsedTime = now - lastRefill; //in millisecs
        //refillRate in token per sec

        if (elapsedTime <= 0) return;

        long tokensToAdd = (elapsedTime * rateLimiterProperties.getRefillRate()) / 1000;
        if (tokensToAdd <= 0) {
            return;
        }

        String tokenStr = jedis.get(tokenKey);
        long currentTokens = tokenStr != null ? Long.parseLong(tokenStr) : rateLimiterProperties.getCapacity();

        //Elapsed time = 2000ms (2s)
        //RefillRate = 5 tokens per second
        //cal = (2000 * 5) /1000 = 10 token to be added at this moment
        long newToken = Math.min(currentTokens + tokensToAdd, rateLimiterProperties.getCapacity());

        jedis.set(tokenKey, String.valueOf(newToken));

        jedis.set(lastRefillKey, String.valueOf(now));

    }

}