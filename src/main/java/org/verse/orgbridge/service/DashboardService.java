package org.verse.orgbridge.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.verse.orgbridge.model.Action;
import org.verse.orgbridge.repository.HistoryRepository;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class DashboardService {

    private final HistoryRepository repository;

    public Mono<Integer> fetchTotalDeployments(String email) {
        return repository.countByEmailAndAction(email, Action.DEPLOY)
                .map(Long::intValue);
    }

    public Mono<Integer> fetchSuccessDeployments(String email) {
        return repository.countByEmailAndActionAndSuccess(email, Action.DEPLOY, true)
                .map(Long::intValue);
    }

    public Mono<Integer> fetchFailedDeployments(String email) {
        return repository.countByEmailAndActionAndSuccess(email, Action.DEPLOY, false)
                .map(Long::intValue);
    }

    public Mono<Integer> fetchTotalActions(String email) {
        return repository.countByEmail(email)
                .map(Long::intValue);
    }
    
}
