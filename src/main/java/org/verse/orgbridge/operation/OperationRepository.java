package org.verse.orgbridge.operation;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OperationRepository
        extends ReactiveCrudRepository<OperationJob, String> {

    Mono<OperationJob> findByIdAndTenantKey(String id, String tenantKey);

    Flux<OperationJob> findByTenantKeyOrderByCreatedAtDesc(String tenantKey);

    Mono<Long> countByTenantKey(String tenantKey);

    Mono<Long> countByTenantKeyAndStatus(
            String tenantKey,
            String status
    );
}
