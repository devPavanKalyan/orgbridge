package org.verse.orgbridge.routes;

import lombok.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.verse.orgbridge.handler.MetadataHandler;

@Component
public class MetadataRoutes {

    @Bean
    public RouterFunction<@NonNull ServerResponse> routerFunction(MetadataHandler handler) {
        return RouterFunctions.route()
                .POST("/api/v1/metadata/types", handler::fetchMetadataTypes)
                .POST(
                        "/api/v1/metadata/components",
                        handler::fetchMetadataComponents
                )
                .POST("/api/v1/metadata/retrieve", handler::pullMetadata)
                .POST("/api/v1/metadata/deploy", handler::pushMetadata)
                .POST(
                        "/api/v1/metadata/deployment/execute",
                        handler::executeMetadataDeployment
                )
                .build();
    }

}
