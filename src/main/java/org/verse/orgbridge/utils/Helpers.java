package org.verse.orgbridge.utils;

import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;


public class Helpers {

    public static Mono<Authentication> doAuthenticate(ServerRequest request) {
        return request.principal()
                .switchIfEmpty(
                        Mono.error(new InsufficientAuthenticationException("Authentication required"))
                )
                .cast(Authentication.class);
    }

    public static String transformToActualEndpoint(String soapEndpoint) {
        return soapEndpoint.replaceAll(
                "/Soap/[uc]/(\\d+\\.\\d+).*$",
                "/Soap/u/$1"
        );
    }

    public static String transformToDataEndpoint(String soapEndpoint) {
        return soapEndpoint.replaceAll(
                "/Soap/[uc]/(\\d+\\.\\d+).*$",
                "/Soap/m/$1"
        );
    }

}
