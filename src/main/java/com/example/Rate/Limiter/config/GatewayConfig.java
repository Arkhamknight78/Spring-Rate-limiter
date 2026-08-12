package com.example.Rate.Limiter.config;

//defined how spring cloud gateway routes the incoming requests
//client -> backend services

import com.example.Rate.Limiter.filter.TokenBucketRateLimiterFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class GatewayConfig {
    private final RateLimiterProperties rateLimiterProperties;
    private final TokenBucketRateLimiterFilter tokenBucketRateLimiterFilter;


    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("api-route", r -> r
                        .path("/api/**")
                        .filters(f -> f
                                .stripPrefix(1)
                                .filter(tokenBucketRateLimiterFilter.apply(new TokenBucketRateLimiterFilter.Config())))
                        .uri(rateLimiterProperties.getApiServerUrl())) // final api server to redirect to
                .build();
    }


    // api/users
    // api/users/123
    // request arrives - > stripPrefix(1) -> remove  /api/prefix -> tokenBuckletRateLimitFilter -> api-server-url

}
