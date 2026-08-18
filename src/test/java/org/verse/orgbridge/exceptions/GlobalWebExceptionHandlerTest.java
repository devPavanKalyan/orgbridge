package org.verse.orgbridge.exceptions;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalWebExceptionHandlerTest {

    private final GlobalWebExceptionHandler handler =
            new GlobalWebExceptionHandler(
                    new ObjectMapper().findAndRegisterModules()
            );

    @Test
    void rendersSafeProblemDetailForInvalidRequest() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post(
                        "/api/v1/integration/operations"
                ).build()
        );

        StepVerifier.create(handler.handle(
                        exchange,
                        new IllegalArgumentException(
                                "targetOrganizationId is required"
                        )
                ))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exchange.getResponse().getBodyAsString().block())
                .contains("\"title\":\"Invalid Request\"")
                .contains("targetOrganizationId is required");
    }

    @Test
    void hidesInternalExceptionDetails() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get(
                        "/api/v1/integration/dashboard"
                ).build()
        );

        StepVerifier.create(handler.handle(
                        exchange,
                        new IllegalStateException("database-password")
                ))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(exchange.getResponse().getBodyAsString().block())
                .doesNotContain("database-password")
                .contains("The request could not be completed.");
    }
}
