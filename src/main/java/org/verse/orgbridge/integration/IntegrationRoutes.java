package org.verse.orgbridge.integration;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
@RequiredArgsConstructor
public class IntegrationRoutes {

    @Bean
    RouterFunction<ServerResponse> integrationApi(
            IntegrationHandler handler
    ) {
        return RouterFunctions.route()
                .path("/api/v1/integration", builder -> builder
                        .GET("/dashboard", handler::dashboard)
                        .GET("/organizations", handler::organizations)
                        .POST("/organizations", handler::createOrganization)
                        .PUT(
                                "/organizations/{id}",
                                handler::updateOrganization
                        )
                        .DELETE(
                                "/organizations/{id}",
                                handler::deleteOrganization
                        )
                        .POST(
                                "/organizations/{id}/activate",
                                handler::activateOrganization
                        )
                        .POST(
                                "/organizations/{id}/test",
                                handler::testOrganization
                        )
                        .GET("/metadata/types", handler::metadataTypes)
                        .POST(
                                "/metadata/components",
                                handler::metadataComponents
                        )
                        .POST("/csv/validate", handler::validateCsv)
                        .POST("/operations", handler::submitOperation)
                        .GET("/operations", handler::operations)
                        .GET("/operations/{id}", handler::operation)
                )
                .build();
    }
}
