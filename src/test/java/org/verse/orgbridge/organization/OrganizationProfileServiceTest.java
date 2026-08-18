package org.verse.orgbridge.organization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.transaction.ReactiveTransactionManager;
import org.verse.orgbridge.cache.OrganizationCacheService;
import org.verse.orgbridge.integration.SalesforceRequestContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrganizationProfileServiceTest {

    private OrganizationProfileRepository repository;
    private OrganizationProfileService service;

    @BeforeEach
    void setUp() {
        repository = mock(OrganizationProfileRepository.class);
        OrganizationCacheService cache =
                mock(OrganizationCacheService.class);
        DatabaseClient databaseClient = mock(DatabaseClient.class);
        ReactiveTransactionManager transactionManager =
                mock(ReactiveTransactionManager.class);

        service = new OrganizationProfileService(
                repository,
                cache,
                databaseClient,
                transactionManager
        );
    }

    @Test
    void createsNewProfileWithAnUnsetOptimisticLockVersion() {
        when(repository.findAllByTenantKeyOrderByCreatedAtAsc(any()))
                .thenReturn(Flux.empty());
        when(repository.save(any()))
                .thenAnswer(invocation ->
                        Mono.just(invocation.getArgument(0))
                );

        OrganizationProfileRequest request =
                new OrganizationProfileRequest(
                        "Development",
                        "developer@example.com.dev",
                        "Sandbox",
                        "https://test.salesforce.com",
                        false
                );

        StepVerifier.create(service.create(context(), request))
                .assertNext(response -> {
                    assertThat(response.id()).isNotBlank();
                    assertThat(response.status())
                            .isEqualTo("Needs Verification");
                    assertThat(response.active()).isFalse();
                })
                .verifyComplete();

        verify(repository).save(
                org.mockito.ArgumentMatchers.argThat(
                        profile -> profile.getVersion() == null
                )
        );
    }

    @Test
    void rejectsNonHttpsLoginUrlsBeforeSaving() {
        when(repository.findAllByTenantKeyOrderByCreatedAtAsc(any()))
                .thenReturn(Flux.empty());

        OrganizationProfileRequest request =
                new OrganizationProfileRequest(
                        "Development",
                        "developer@example.com.dev",
                        "Sandbox",
                        "http://test.salesforce.com",
                        false
                );

        assertThatThrownBy(() -> service.create(context(), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Login URL must be an HTTPS URL");
    }

    private SalesforceRequestContext context() {
        return new SalesforceRequestContext(
                "00D000000000001AAA",
                "005000000000001AAA",
                "correlation-id"
        );
    }
}
