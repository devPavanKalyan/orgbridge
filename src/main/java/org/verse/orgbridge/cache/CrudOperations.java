package org.verse.orgbridge.cache;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CrudOperations<K, V, I> {

    Mono<Boolean> create(K key, V value);

    Mono<Boolean> update(K key, V value);

    Mono<Boolean> delete(K key, I identifier);

    Mono<V> read(K key, I identifier);

    Flux<V> getAll(K key);
}
