package org.verse.orgbridge.jwt;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.verse.orgbridge.config.UserDetailsService;
import org.verse.orgbridge.sse.LogoutEventPublisher;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
@NullMarked
public class JwtFilter implements WebFilter {

    private static final List<String> EXCLUDED_PATHS = List.of(
            "/api/v1/auth/**",
            "/api/v1/sse/**",
            "/api/v1/integration/**",
            "/api/v1/ping",
            "/actuator/health"
    );
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final LogoutEventPublisher logoutPublisher;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        String path = exchange.getRequest().getPath().value();

        boolean excluded = EXCLUDED_PATHS.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));

        if (excluded) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        // ✅ DO NOT BLOCK IF TOKEN IS MISSING
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return chain.filter(exchange);
        }

        String jwt = authHeader.substring(7);

        return Mono.fromCallable(() -> jwtService.extractUsername(jwt))
                .flatMap(username ->
                        doAuthenticate(username, jwt)
                                .switchIfEmpty(
                                        handleInvalidToken(exchange, username)
                                                .then(Mono.error(new RuntimeException("INVALID_TOKEN")))
                                )
                )
                .flatMap(authentication ->
                        chain.filter(exchange)
                                .contextWrite(
                                        ReactiveSecurityContextHolder
                                                .withAuthentication(authentication)
                                )
                )
                .onErrorResume(ex ->
                        handleInvalidToken(exchange, "unknown")
                );

    }

    private Mono<Void> handleInvalidToken(
            ServerWebExchange exchange,
            String username
    ) {
        logoutPublisher.publish(username, "TOKEN_INVALID");

        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }


    private Mono<UsernamePasswordAuthenticationToken> doAuthenticate(
            String username,
            String jwt
    ) {
        return userDetailsService.findByUsername(username)
                .filter(user ->
                        user.isEnabled()
                                && jwtService.validateToken(
                                jwt,
                                user.getUsername()
                        )
                )
                .map(this::buildAuthentication);
    }

    private UsernamePasswordAuthenticationToken buildAuthentication(UserDetails user) {
        return new UsernamePasswordAuthenticationToken(
                user,
                null,
                user.getAuthorities()
        );
    }
}
