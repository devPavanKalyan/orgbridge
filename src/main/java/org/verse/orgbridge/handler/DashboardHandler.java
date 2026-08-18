package org.verse.orgbridge.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.verse.orgbridge.service.DashboardService;
import org.verse.orgbridge.utils.Helpers;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class DashboardHandler {

    private final DashboardService service;

    public Mono<ServerResponse> dashboard(ServerRequest request) {
        return Helpers.doAuthenticate(request)
                .map(Authentication::getName)
                .flatMap(this::dashboardByEmail)
                .flatMap(ServerResponse.ok()::bodyValue);
    }

    private Mono<Map<String, Integer>> dashboardByEmail(String email) {
        return Mono.zip(
                service.fetchTotalDeployments(email),
                service.fetchSuccessDeployments(email),
                service.fetchFailedDeployments(email)
        ).map(t -> Map.of(
                "total", t.getT1(),
                "success", t.getT2(),
                "failed", t.getT3()
        ));
    }
}
