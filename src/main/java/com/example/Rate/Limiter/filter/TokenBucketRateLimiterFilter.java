package com.example.Rate.Limiter.filter;

import com.example.Rate.Limiter.service.RateLimiterService;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class TokenBucketRateLimiterFilter extends AbstractGatewayFilterFactory<TokenBucketRateLimiterFilter.Config> {
    private final RateLimiterService rateLimiterService;
    @Override
    public TokenBucketRateLimiterFilter.Config newConfig() {
        return new Config();
    }
    @Override
    public GatewayFilter apply(Config config) {

        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();

            String clientId = getClientId(request);
            if(!rateLimiterService.isAllowed(clientId)){
                response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                addRateLimitHeaders(response, clientId);

                String errorBody = String.format(
                        "{\" error\":\"Rate limited exceeded\". \"clientId\":\"%s\"}", clientId
                );

                return response.writeWith(Mono.just(response.bufferFactory().wrap(errorBody.getBytes(StandardCharsets.UTF_8))));
            }

            return chain.filter(exchange).then(Mono.fromRunnable(() -> addRateLimitHeaders(response, clientId)));
        };

    }

    private void addRateLimitHeaders(ServerHttpResponse response, String clientId) {
        response.getHeaders().add("X-RateLimit-limit", String.valueOf(rateLimiterService.getCapacity()));
        response.getHeaders().add("X-RateLimit-remaining", String.valueOf(rateLimiterService.getAvailableTokens(clientId)));
    }
    private String getClientId(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if(xForwardedFor != null && !xForwardedFor.isEmpty()){
            return xForwardedFor.split(",")[0];
        }

        //direct connection ip
        var remoteAddress = request.getRemoteAddress();
        if(remoteAddress != null && remoteAddress.getHostName() != null){
            return remoteAddress.getAddress().getHostAddress();
        }
        //fallback
        return "unknown";
    }

    public static class Config {

    }
}
