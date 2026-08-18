package org.verse.orgbridge.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.WebFilterChain;
import org.verse.orgbridge.vault.VaultService;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IntegrationAuthenticationWebFilterTest {

    private static final String API_KEY =
            "0123456789012345678901234567890123456789";

    private IntegrationAuthenticationWebFilter filter;

    @BeforeEach
    void setUp() {
        VaultService vaultService = mock(VaultService.class);
        when(vaultService.integrationApiKey()).thenReturn(API_KEY);
        filter = new IntegrationAuthenticationWebFilter(vaultService);
    }

    @Test
    void authenticatesValidSalesforceHeaders() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest
                        .get("/api/v1/integration/dashboard")
                        .header(
                                IntegrationAuthenticationWebFilter
                                        .INTEGRATION_KEY_HEADER,
                                API_KEY
                        )
                        .header(
                                IntegrationAuthenticationWebFilter
                                        .SALESFORCE_ORG_HEADER,
                                "00D000000000001AAA"
                        )
                        .header(
                                IntegrationAuthenticationWebFilter
                                        .SALESFORCE_USER_HEADER,
                                "005000000000001AAA"
                        )
                        .build()
        );
        AtomicReference<Authentication> authentication =
                new AtomicReference<>();
        WebFilterChain chain = ignored ->
                ReactiveSecurityContextHolder.getContext()
                        .doOnNext(context ->
                                authentication.set(
                                        context.getAuthentication()
                                )
                        )
                        .then();

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(authentication.get()).isNotNull();
        assertThat(authentication.get().getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_SALESFORCE");
        assertThat(exchange.getResponse().getHeaders()
                .getFirst("X-Correlation-Id"))
                .isNotBlank();
    }

    @Test
    void rejectsMissingIntegrationKey() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest
                        .get("/api/v1/integration/dashboard")
                        .header(
                                IntegrationAuthenticationWebFilter
                                        .SALESFORCE_ORG_HEADER,
                                "00D000000000001AAA"
                        )
                        .header(
                                IntegrationAuthenticationWebFilter
                                        .SALESFORCE_USER_HEADER,
                                "005000000000001AAA"
                        )
                        .build()
        );

        StepVerifier.create(filter.filter(
                        exchange,
                        ignored -> Mono.error(new AssertionError(
                                "Filter chain must not run"
                        ))
                ))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
