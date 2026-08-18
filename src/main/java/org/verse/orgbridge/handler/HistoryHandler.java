package org.verse.orgbridge.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.verse.orgbridge.model.History;
import org.verse.orgbridge.service.HistoryService;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class HistoryHandler {

    private final HistoryService historyService;

    public Mono<ServerResponse> getAll(ServerRequest request) {

        return doAuthenticate(request)
                .flatMapMany(auth ->
                        historyService.getAll(auth.getName())
                )
                .as(histories ->
                        ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(histories, History.class)
                )
                .onErrorResume(ex ->
                        ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(Map.of(
                                        "error", "Failed to fetch history",
                                        "message", ex.getMessage()
                                ))
                );
    }


    private Mono<Authentication> doAuthenticate(ServerRequest request) {
        return request.principal()
                .switchIfEmpty(
                        Mono.error(
                                new InsufficientAuthenticationException(
                                        "Authentication is required"
                                )
                        )
                )
                .cast(Authentication.class);
    }

}
