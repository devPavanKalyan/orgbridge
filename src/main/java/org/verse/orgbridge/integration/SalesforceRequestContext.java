package org.verse.orgbridge.integration;

import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

public record SalesforceRequestContext(
        String organizationId,
        String userId,
        String correlationId
) {
    public String tenantKey() {
        return organizationId + ":" + userId;
    }

    public static Mono<SalesforceRequestContext> from(
            Authentication authentication
    ) {
        if (authentication.getDetails() instanceof SalesforceRequestContext context) {
            return Mono.just(context);
        }
        return Mono.error(
                new IllegalStateException("Salesforce request context is missing")
        );
    }
}
