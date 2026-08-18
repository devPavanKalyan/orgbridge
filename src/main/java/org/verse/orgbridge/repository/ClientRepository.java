package org.verse.orgbridge.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import org.verse.orgbridge.model.Client;
import reactor.core.publisher.Mono;

@Repository
public interface ClientRepository extends ReactiveCrudRepository<Client, String> {
    Mono<Client> findByUsername(String username);

    Mono<Boolean> existsByUsername(String username);
}
