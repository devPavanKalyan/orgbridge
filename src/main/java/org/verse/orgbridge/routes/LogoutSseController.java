package org.verse.orgbridge.routes;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.verse.orgbridge.records.auth.LogoutEvent;
import org.verse.orgbridge.sse.LogoutEventPublisher;
import reactor.core.publisher.Flux;

@RestController
@RequiredArgsConstructor
public class LogoutSseController {

    private final LogoutEventPublisher publisher;

    @GetMapping(
            value = "/api/v1/sse/logout",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public Flux<ServerSentEvent<LogoutEvent>> stream(
            @RequestParam("username") String username
    ) {
        return publisher.stream()
                .filter(event -> event.username().equals(username))
                .map(event ->
                        ServerSentEvent.builder(event)
                                .event("FORCE_LOGOUT")
                                .build()
                );
    }
}


