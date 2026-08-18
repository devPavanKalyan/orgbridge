package org.verse.orgbridge.organization;

import org.springframework.http.HttpStatus;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.server.ResponseStatusException;
import org.verse.orgbridge.cache.OrganizationCacheService;
import org.verse.orgbridge.integration.SalesforceRequestContext;
import org.verse.orgbridge.integration.contract.OrganizationSummary;
import org.verse.orgbridge.records.session.SalesforceSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class OrganizationProfileService {

    private static final String NEEDS_VERIFICATION = "Needs Verification";
    private static final String CONNECTED = "Connected";
    private static final Set<String> ENVIRONMENTS =
            Set.of("Production", "Sandbox");

    private final OrganizationProfileRepository repository;
    private final OrganizationCacheService cache;
    private final DatabaseClient databaseClient;
    private final TransactionalOperator transactions;

    public OrganizationProfileService(
            OrganizationProfileRepository repository,
            OrganizationCacheService cache,
            DatabaseClient databaseClient,
            ReactiveTransactionManager transactionManager
    ) {
        this.repository = repository;
        this.cache = cache;
        this.databaseClient = databaseClient;
        this.transactions = TransactionalOperator.create(
                transactionManager
        );
    }

    public Flux<OrganizationSummary> getAll(
            SalesforceRequestContext context
    ) {
        return repository
                .findAllByTenantKeyOrderByCreatedAtAsc(context.tenantKey())
                .map(OrganizationSummary::from);
    }

    public Mono<Long> count(SalesforceRequestContext context) {
        return repository.countByTenantKey(context.tenantKey());
    }

    public Mono<OrganizationSummary> create(
            SalesforceRequestContext context,
            OrganizationProfileRequest request
    ) {
        OrganizationProfile profile = new OrganizationProfile();
        profile.setId(UUID.randomUUID().toString());
        profile.setTenantKey(context.tenantKey());
        profile.setStatus(NEEDS_VERIFICATION);
        profile.setActive(false);
        profile.setCreatedAt(Instant.now());
        apply(profile, request);

        return ensureUnique(context, request, null)
                .then(repository.save(profile))
                .flatMap(saved -> request.active()
                        ? activate(context, saved.getId())
                        : Mono.just(OrganizationSummary.from(saved)));
    }

    public Mono<OrganizationSummary> update(
            SalesforceRequestContext context,
            String id,
            OrganizationProfileRequest request
    ) {
        return find(context, id)
                .flatMap(profile -> ensureUnique(context, request, id)
                        .thenReturn(profile))
                .flatMap(profile -> {
                    apply(profile, request);
                    profile.setActive(
                            request.active() && profile.isActive()
                    );
                    return repository.save(profile);
                })
                .flatMap(saved -> request.active()
                        ? activate(context, saved.getId())
                        : Mono.just(OrganizationSummary.from(saved)));
    }

    public Mono<OrganizationSummary> activate(
            SalesforceRequestContext context,
            String id
    ) {
        return find(context, id)
                .flatMap(ignored -> databaseClient.sql("""
                                UPDATE salesforce_organizations
                                SET active = FALSE,
                                    updated_at = :updatedAt,
                                    version = version + 1
                                WHERE tenant_key = :tenantKey
                                """)
                        .bind("updatedAt", Instant.now())
                        .bind("tenantKey", context.tenantKey())
                        .fetch()
                        .rowsUpdated()
                        .then(databaseClient.sql("""
                                        UPDATE salesforce_organizations
                                        SET active = TRUE,
                                            updated_at = :updatedAt,
                                            version = version + 1
                                        WHERE tenant_key = :tenantKey
                                          AND id = :id
                                        """)
                                .bind("updatedAt", Instant.now())
                                .bind("tenantKey", context.tenantKey())
                                .bind("id", id)
                                .fetch()
                                .rowsUpdated()))
                .then(find(context, id))
                .map(OrganizationSummary::from)
                .as(transactions::transactional);
    }

    public Mono<OrganizationSummary> testConnection(
            SalesforceRequestContext context,
            String id
    ) {
        return find(context, id)
                .flatMap(profile -> matchingSession(context, profile)
                        .flatMap(session -> markConnected(profile, session))
                        .switchIfEmpty(markDisconnected(profile)))
                .map(OrganizationSummary::from);
    }

    public Mono<Void> delete(
            SalesforceRequestContext context,
            String id
    ) {
        return find(context, id)
                .flatMap(profile -> repository.delete(profile)
                        .then(deleteSession(context, profile)));
    }

    private Mono<OrganizationProfile> find(
            SalesforceRequestContext context,
            String id
    ) {
        if (id == null || id.isBlank()) {
            return Mono.error(new IllegalArgumentException(
                    "Organization ID is required"
            ));
        }

        return repository.findByIdAndTenantKey(id, context.tenantKey())
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Organization was not found"
                )));
    }

    private Mono<Void> ensureUnique(
            SalesforceRequestContext context,
            OrganizationProfileRequest request,
            String excludedId
    ) {
        String name = normalized(request.name());
        String username = normalized(request.username());

        return repository
                .findAllByTenantKeyOrderByCreatedAtAsc(context.tenantKey())
                .filter(profile -> !profile.getId().equals(excludedId))
                .filter(profile ->
                        normalized(profile.getName()).equals(name)
                                || normalized(profile.getUsername())
                                .equals(username)
                )
                .next()
                .flatMap(ignored -> Mono.<Void>error(
                        new IllegalArgumentException(
                                "Organization name and username must be unique"
                        )
                ))
                .then();
    }

    private void apply(
            OrganizationProfile profile,
            OrganizationProfileRequest request
    ) {
        validate(request);
        profile.setName(request.name().trim());
        profile.setUsername(request.username().trim());
        profile.setEnvironment(request.environment());
        profile.setLoginUrl(request.loginUrl().trim());
        profile.setUpdatedAt(Instant.now());
    }

    private void validate(OrganizationProfileRequest request) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Organization request is required"
            );
        }
        requireText(request.name(), "Organization name", 160);
        requireText(request.username(), "Salesforce username", 320);
        requireText(request.loginUrl(), "Login URL", 500);

        if (!ENVIRONMENTS.contains(request.environment())) {
            throw new IllegalArgumentException(
                    "Environment must be Production or Sandbox"
            );
        }

        URI loginUri;
        try {
            loginUri = URI.create(request.loginUrl().trim());
        } catch (IllegalArgumentException invalidUri) {
            throw new IllegalArgumentException("Login URL is invalid");
        }
        if (!"https".equalsIgnoreCase(loginUri.getScheme())
                || loginUri.getHost() == null) {
            throw new IllegalArgumentException(
                    "Login URL must be an HTTPS URL"
            );
        }
    }

    private void requireText(String value, String label, int maxLength) {
        if (value == null
                || value.isBlank()
                || value.length() > maxLength) {
            throw new IllegalArgumentException(
                    label + " is required and must not exceed "
                            + maxLength + " characters"
            );
        }
    }

    private Mono<SalesforceSession> matchingSession(
            SalesforceRequestContext context,
            OrganizationProfile profile
    ) {
        return cache.getAll(context.tenantKey())
                .filter(SalesforceSession::isAuthenticated)
                .filter(session -> matches(profile, session))
                .next();
    }

    private boolean matches(
            OrganizationProfile profile,
            SalesforceSession session
    ) {
        return profile.getSalesforceOrganizationId() != null
                && profile.getSalesforceOrganizationId().equalsIgnoreCase(
                        session.getOrganizationId()
                )
                || session.getUserEmail() != null
                && session.getUserEmail().equalsIgnoreCase(
                        profile.getUsername()
                );
    }

    private Mono<OrganizationProfile> markConnected(
            OrganizationProfile profile,
            SalesforceSession session
    ) {
        profile.setSalesforceOrganizationId(session.getOrganizationId());
        profile.setStatus(CONNECTED);
        profile.setLastVerifiedAt(Instant.now());
        profile.setUpdatedAt(Instant.now());
        return repository.save(profile);
    }

    private Mono<OrganizationProfile> markDisconnected(
            OrganizationProfile profile
    ) {
        profile.setStatus(NEEDS_VERIFICATION);
        profile.setUpdatedAt(Instant.now());
        return repository.save(profile);
    }

    private Mono<Void> deleteSession(
            SalesforceRequestContext context,
            OrganizationProfile profile
    ) {
        if (profile.getSalesforceOrganizationId() == null) {
            return Mono.empty();
        }
        return cache.delete(
                        context.tenantKey(),
                        profile.getSalesforceOrganizationId()
                )
                .then();
    }

    private String normalized(String value) {
        return value == null
                ? ""
                : value.trim().toLowerCase(Locale.ROOT);
    }
}
