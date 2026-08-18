package org.verse.orgbridge.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import org.verse.orgbridge.records.session.SalesforceSession;
import org.verse.orgbridge.vault.VaultService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class OrganizationCacheService
        implements CrudOperations<String, SalesforceSession, String> {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final VaultService vaultService;

    public OrganizationCacheService(
            ReactiveStringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            VaultService vaultService
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.vaultService = vaultService;
    }

    @Override
    public Mono<Boolean> create(String tenantKey, SalesforceSession session) {
        if (session == null
                || session.getOrganizationId() == null
                || session.getOrganizationId().isBlank()) {
            return Mono.error(new IllegalArgumentException(
                    "Salesforce organization ID is required"
            ));
        }

        return serialize(session)
                .flatMap(json -> hashOperations().put(
                        buildKey(tenantKey),
                        session.getOrganizationId(),
                        json
                ))
                .flatMap(ignored -> redisTemplate.expire(
                        buildKey(tenantKey),
                        Duration.ofSeconds(
                                vaultService.redisSessionTtlSeconds()
                        )
                ))
                .filter(Boolean::booleanValue)
                .switchIfEmpty(Mono.error(new IllegalStateException(
                        "Salesforce session expiration could not be set"
                )))
                .thenReturn(true);
    }

    @Override
    public Mono<Boolean> update(String tenantKey, SalesforceSession session) {
        return create(tenantKey, session);
    }

    @Override
    public Mono<Boolean> delete(String tenantKey, String identifier) {
        return read(tenantKey, identifier)
                .flatMap(session -> hashOperations().remove(
                        buildKey(tenantKey),
                        session.getOrganizationId()
                ))
                .map(deleted -> deleted > 0)
                .defaultIfEmpty(false);
    }

    @Override
    public Mono<SalesforceSession> read(
            String tenantKey,
            String identifier
    ) {
        if (identifier == null || identifier.isBlank()) {
            return Mono.empty();
        }

        return hashOperations()
                .get(buildKey(tenantKey), identifier)
                .flatMap(this::deserialize)
                .switchIfEmpty(
                        getAll(tenantKey)
                                .filter(session -> matches(session, identifier))
                                .next()
                );
    }

    @Override
    public Flux<SalesforceSession> getAll(String tenantKey) {
        return hashOperations()
                .values(buildKey(tenantKey))
                .flatMap(this::deserialize);
    }

    private Mono<SalesforceSession> deserialize(String value) {
        if (value == null || value.isBlank()) {
            return Mono.empty();
        }
        return Mono.fromCallable(() ->
                objectMapper.readValue(
                        value,
                        SalesforceSession.class
                )
        );
    }

    private Mono<String> serialize(SalesforceSession session) {
        return Mono.fromCallable(() ->
                objectMapper.writeValueAsString(session)
        );
    }

    private boolean matches(
            SalesforceSession session,
            String identifier
    ) {
        return identifier.equalsIgnoreCase(session.getOrganizationId())
                || (
                session.getUserId() != null
                        && identifier.equalsIgnoreCase(session.getUserId())
        );
    }

    private String buildKey(String tenantKey) {
        return vaultService.redisKeyPrefix() + tenantKey;
    }

    private ReactiveHashOperations<String, String, String> hashOperations() {
        return redisTemplate.opsForHash();
    }
}
