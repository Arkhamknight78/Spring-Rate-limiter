package com.example.Rate.Limiter.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RateLimiterService {
    private final RedisTokenBucketService redisTokenBucketService;

    public boolean isAllowed(String clientId){
        return redisTokenBucketService.isAllowed(clientId);
    }

    public long getCapacity(){
        return redisTokenBucketService.getCapacity();
    }

    public long getAvailableTokens(String  clientId){
        return redisTokenBucketService.getAvailableTokens(clientId);
    }

    //Client request -> gateway filter(intercepts the request) -> checks rate limit

    //Global filter -> applied to all routes
    //route filter-> specific routes
    //custom filters-> own impl of filter // sits before api server
}
