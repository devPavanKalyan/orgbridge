package org.verse.orgbridge.config;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.verse.orgbridge.repository.ClientRepository;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@NullMarked
public class UserDetailsService {

    private final ClientRepository repository;

    public Mono<UserDetails> findByUsername(String username) {
        return repository.findByUsername(username)
                .cast(UserDetails.class);
    }
}
