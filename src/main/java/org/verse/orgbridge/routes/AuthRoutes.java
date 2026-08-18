package org.verse.orgbridge.routes;

import lombok.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.verse.orgbridge.handler.AuthHandler;

import java.util.Map;

@Component
public class AuthRoutes {

    @Bean
    public RouterFunction<@NonNull ServerResponse> authenticationRoutes(AuthHandler handler) {
        return RouterFunctions.route()
                .path("/api/v1/auth/", builder -> builder
                        .POST("login", handler::login)
                        .POST("signup", handler::signup)
                        .GET("ping", request -> ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(Map.of("status", "up")))
                ).build();
    }

}
