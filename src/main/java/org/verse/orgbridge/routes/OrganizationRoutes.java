package org.verse.orgbridge.routes;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.verse.orgbridge.handler.OrganizationHandler;

@Component
@RequiredArgsConstructor
@NullMarked
public class OrganizationRoutes {

    private final OrganizationHandler handler;

    @Bean
    public RouterFunction<ServerResponse> orgRoutes() {

        return RouterFunctions.route()
                .path("/api/v1/organizations", builder -> builder
                        .POST("", handler::create)
                        .GET("", handler::getAll)
                        .GET("/{identifier}", handler::read)
                        .DELETE("/{identifier}", handler::delete)
                )
                .build();
    }
}
