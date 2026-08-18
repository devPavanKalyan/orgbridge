package org.verse.orgbridge.operation;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.verse.orgbridge.vault.VaultService;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class OperationWorker {

    private final OperationJobStore jobStore;
    private final OperationExecutor executor;
    private final VaultService vaultService;

    private Disposable subscription;

    @PostConstruct
    void start() {
        if (!vaultService.operationWorkerEnabled()) {
            return;
        }

        subscription = Flux.interval(Duration.ZERO, Duration.ofSeconds(1))
                .flatMap(
                        tick -> jobStore.claimNext()
                                .flatMap(executor::process)
                                .onErrorResume(error -> {
                                    log.error(
                                            "Operation worker polling failed",
                                            error
                                    );
                                    return Mono.empty();
                                }),
                        vaultService.operationWorkerConcurrency()
                )
                .subscribe();
    }

    @PreDestroy
    void stop() {
        if (subscription != null) {
            subscription.dispose();
        }
    }
}
