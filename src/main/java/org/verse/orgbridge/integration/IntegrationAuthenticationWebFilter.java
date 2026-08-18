package org.verse.orgbridge.integration;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.verse.orgbridge.vault.VaultService;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

public final class IntegrationAuthenticationWebFilter implements WebFilter {

    public static final String INTEGRATION_KEY_HEADER =
            "X-OrgBridge-Integration-Key";
    public static final String SALESFORCE_ORG_HEADER =
            "X-Salesforce-Org-Id";
    public static final String SALESFORCE_USER_HEADER =
            "X-Salesforce-User-Id";
    public static final String CORRELATION_HEADER = "X-Correlation-Id";

    private static final Pattern SALESFORCE_ID =
            Pattern.compile("^[a-zA-Z0-9]{15}(?:[a-zA-Z0-9]{3})?$");

    private final byte[] expectedApiKey;

    public IntegrationAuthenticationWebFilter(VaultService vaultService) {
        this.expectedApiKey = vaultService.integrationApiKey()
                .getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            WebFilterChain chain
    ) {
        String path = exchange.getRequest().getPath().value();

        if (!path.startsWith("/api/v1/integration/")) {
            return chain.filter(exchange);
        }

        String providedApiKey = exchange.getRequest()
                .getHeaders()
                .getFirst(INTEGRATION_KEY_HEADER);
        String organizationId = exchange.getRequest()
                .getHeaders()
                .getFirst(SALESFORCE_ORG_HEADER);
        String userId = exchange.getRequest()
                .getHeaders()
                .getFirst(SALESFORCE_USER_HEADER);

        if (!validApiKey(providedApiKey)
                || !validSalesforceId(organizationId)
                || !validSalesforceId(userId)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String correlationId = correlationId(exchange);
        SalesforceRequestContext context = new SalesforceRequestContext(
                organizationId,
                userId,
                correlationId
        );
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        context.tenantKey(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_SALESFORCE"))
                );
        authentication.setDetails(context);

        exchange.getResponse()
                .getHeaders()
                .set(CORRELATION_HEADER, correlationId);

        return chain.filter(exchange)
                .contextWrite(
                        ReactiveSecurityContextHolder.withAuthentication(
                                authentication
                        )
                );
    }

    private boolean validApiKey(String providedApiKey) {
        return providedApiKey != null
                && MessageDigest.isEqual(
                expectedApiKey,
                providedApiKey.getBytes(StandardCharsets.UTF_8)
        );
    }

    private boolean validSalesforceId(String value) {
        return value != null && SALESFORCE_ID.matcher(value).matches();
    }

    private String correlationId(ServerWebExchange exchange) {
        String requested = exchange.getRequest()
                .getHeaders()
                .getFirst(CORRELATION_HEADER);

        if (requested != null
                && requested.length() <= 128
                && requested.matches("^[a-zA-Z0-9._-]+$")) {
            return requested;
        }
        return UUID.randomUUID().toString();
    }
}
