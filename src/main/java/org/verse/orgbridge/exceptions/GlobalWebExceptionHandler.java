package org.verse.orgbridge.exceptions;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.concurrent.TimeoutException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
@Slf4j
public class GlobalWebExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(
            ServerWebExchange exchange,
            Throwable error
    ) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(error);
        }

        HttpStatus status = status(error);
        String detail = detail(status, error);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                status,
                detail
        );
        problem.setTitle(title(status));
        problem.setInstance(URI.create(
                exchange.getRequest().getPath().value()
        ));

        String correlationId = exchange.getResponse()
                .getHeaders()
                .getFirst("X-Correlation-Id");
        if (correlationId != null) {
            problem.setProperty("correlationId", correlationId);
        }

        if (status.is5xxServerError()) {
            log.error(
                    "Request failed: method={}, path={}, correlationId={}",
                    exchange.getRequest().getMethod(),
                    exchange.getRequest().getPath().value(),
                    correlationId,
                    error
            );
        }

        byte[] body;
        try {
            body = objectMapper.writeValueAsBytes(problem);
        } catch (Exception serializationError) {
            body = (
                    "{\"title\":\"Request Failed\","
                            + "\"status\":500,"
                            + "\"detail\":\"The request could not be completed.\"}"
            ).getBytes(java.nio.charset.StandardCharsets.UTF_8);
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(
                MediaType.APPLICATION_PROBLEM_JSON
        );
        return exchange.getResponse().writeWith(Mono.just(
                exchange.getResponse()
                        .bufferFactory()
                        .wrap(body)
        ));
    }

    private HttpStatus status(Throwable error) {
        if (error instanceof ResponseStatusException responseStatus) {
            HttpStatus resolved = HttpStatus.resolve(
                    responseStatus.getStatusCode().value()
            );
            return resolved == null
                    ? HttpStatus.INTERNAL_SERVER_ERROR
                    : resolved;
        }
        if (error instanceof IllegalArgumentException
                || error instanceof ConstraintViolationException
                || error instanceof ServerWebInputException
                || error instanceof DecodingException) {
            return HttpStatus.BAD_REQUEST;
        }
        if (error instanceof WebClientException) {
            return HttpStatus.BAD_GATEWAY;
        }
        if (error instanceof TimeoutException) {
            return HttpStatus.GATEWAY_TIMEOUT;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String detail(HttpStatus status, Throwable error) {
        if (status.is4xxClientError()
                && error.getMessage() != null
                && !error.getMessage().isBlank()) {
            return error.getMessage();
        }
        if (status == HttpStatus.BAD_GATEWAY) {
            return "Salesforce or a required integration is unavailable.";
        }
        if (status == HttpStatus.GATEWAY_TIMEOUT) {
            return "The downstream operation timed out.";
        }
        return "The request could not be completed.";
    }

    private String title(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "Invalid Request";
            case UNAUTHORIZED -> "Unauthorized";
            case FORBIDDEN -> "Forbidden";
            case NOT_FOUND -> "Not Found";
            case BAD_GATEWAY -> "Integration Unavailable";
            case GATEWAY_TIMEOUT -> "Integration Timeout";
            default -> "Request Failed";
        };
    }
}
