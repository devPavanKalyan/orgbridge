package org.verse.orgbridge.organization;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface OrganizationProfileRepository
        extends ReactiveCrudRepository<OrganizationProfile, String> {

    Flux<OrganizationProfile> findAllByTenantKeyOrderByCreatedAtAsc(
            String tenantKey
    );

    Mono<OrganizationProfile> findByIdAndTenantKey(
            String id,
            String tenantKey
    );

    Mono<Long> countByTenantKey(String tenantKey);
}
