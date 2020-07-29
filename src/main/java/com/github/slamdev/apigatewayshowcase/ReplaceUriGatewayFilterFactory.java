package com.github.slamdev.apigatewayshowcase;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.RouteToRequestUrlFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

@Component
public class ReplaceUriGatewayFilterFactory extends AbstractGatewayFilterFactory<ReplaceUriGatewayFilterFactory.Config> {

    public ReplaceUriGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return new OrderedGatewayFilter((exchange, chain) -> {
            Optional<URI> uri = buildUri(exchange);
            if (uri.isPresent()) {
                exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR, uri.get());
                return chain.filter(exchange);
            }

            exchange.getResponse().setStatusCode(HttpStatus.BAD_GATEWAY);
            if (!exchange.getResponse().isCommitted()) {
                exchange.getResponse().getHeaders().add("errorMessage", "uri is undefined");
            }
            return exchange.getResponse().setComplete();
        }, RouteToRequestUrlFilter.ROUTE_TO_URL_FILTER_ORDER + 1);
    }

    /**
     * Build host based on "service" and optional "namespace" params from path predicate
     * in a form of: http://{service}.{namespace} and replaces resulting host in the original URI
     * keeping the rest of the original path untouched:
     * http://no-op:80/... -> http://{service}.{namespace}/...
     */
    private Optional<URI> buildUri(ServerWebExchange exchange) {
        Map<String, String> templateVars = ServerWebExchangeUtils.getUriTemplateVariables(exchange);
        if (!templateVars.containsKey("service")) {
            return Optional.empty();
        }

        StringBuilder host = new StringBuilder("http://");
        host.append(templateVars.get("service"));
        if (templateVars.containsKey("namespace")) {
            host.append(".");
            host.append(templateVars.get("namespace"));
        }
        host.append("/");

        URI originalUri = exchange.getRequiredAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
        String resultingUri = originalUri.toASCIIString().replaceFirst("http://no-op:80/", host.toString());

        return Optional.of(URI.create(resultingUri));
    }

    public static class Config {
    }
}
