package com.example.Rate.Limiter.Controllers;

import com.example.Rate.Limiter.service.RateLimiterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/gateway")
@RequiredArgsConstructor
public class StatusController {

    private final RateLimiterService rateLimiterService;

    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, Object>>> getHealth(){
        return Mono.just(ResponseEntity.ok(Map.of("status","UP", "service", "rate-limiting-gateway")));
    }

    @GetMapping("rate-limit/status")
    public Mono<ResponseEntity<Map<String, Object>>> getRateLimitStatus(ServerWebExchange exchange){
        String clientId = getClientId(exchange.getRequest());
        return Mono.just(ResponseEntity.ok(Map.of("status","UP",
                "service", "rate-limiting-gateway",
                "clientId",clientId,
                "capacity",rateLimiterService.getCapacity(),
                "availableTokens",rateLimiterService.getAvailableTokens(clientId))));
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

}
