package org.verse.orgbridge.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import org.verse.orgbridge.model.Action;
import org.verse.orgbridge.model.History;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface HistoryRepository extends ReactiveCrudRepository<History, String> {
    Flux<History> findByOrg(String org);

    Flux<History> findByEmail(String email);

    Mono<Long> countByEmail(String email);

    Mono<Long> countByEmailAndAction(String email, Action action);

    Mono<Long> countByEmailAndActionAndSuccess(String email, Action action, boolean success);
}
