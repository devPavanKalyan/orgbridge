package org.verse.orgbridge.sse;

import org.springframework.stereotype.Component;
import org.verse.orgbridge.records.auth.LogoutEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class LogoutEventPublisher {

    private final Sinks.Many<LogoutEvent> sink =
            Sinks.many().multicast().onBackpressureBuffer();

    public void publish(String username, String reason) {
        sink.tryEmitNext(new LogoutEvent(username, reason));
    }

    public Flux<LogoutEvent> stream() {
        return sink.asFlux();
    }
}

