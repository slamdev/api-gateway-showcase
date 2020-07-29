package com.github.slamdev.apigatewayshowcase;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

@Component
public class ValidateJwtGatewayFilterFactory extends AbstractGatewayFilterFactory<ValidateJwtGatewayFilterFactory.Config> {

    public ValidateJwtGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            if (hasValidJwt(request)) {
                return chain.filter(exchange);
            }
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            if (!exchange.getResponse().isCommitted()) {
                exchange.getResponse().getHeaders().add("errorMessage", "jwt is not valid");
            }
            return exchange.getResponse().setComplete();
        };
    }

    private boolean hasValidJwt(ServerHttpRequest request) {
        return true;
    }

    public static class Config {
    }
}
