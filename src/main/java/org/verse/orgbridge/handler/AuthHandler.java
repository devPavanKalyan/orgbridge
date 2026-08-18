package org.verse.orgbridge.handler;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.verse.orgbridge.records.auth.AuthResponse;
import org.verse.orgbridge.records.auth.LoginPayload;
import org.verse.orgbridge.records.auth.SignUpPayload;
import org.verse.orgbridge.service.OAuthService;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class AuthHandler {

    private final OAuthService authService;
    private final Validator validator;


    public Mono<ServerResponse> login(ServerRequest request) {
        return request.bodyToMono(LoginPayload.class)
                .switchIfEmpty(Mono.error(
                        new IllegalArgumentException("Request body is required")
                ))
                .doOnNext(this::validate)
                .flatMap(req ->
                        ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(authService.login(req), AuthResponse.class)
                );
    }


    public Mono<ServerResponse> signup(ServerRequest request) {
        return request.bodyToMono(SignUpPayload.class)
                .switchIfEmpty(Mono.error(
                        new IllegalArgumentException("Request body is required")
                ))
                .doOnNext(this::validate)
                .flatMap(req ->
                        ServerResponse.status(HttpStatus.CREATED)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(authService.signup(req), AuthResponse.class)
                );
    }


    private <T> void validate(T body) {
        var violations = validator.validate(body);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }
}
