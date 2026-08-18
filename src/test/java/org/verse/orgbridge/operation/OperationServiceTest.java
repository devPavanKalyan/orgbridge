package org.verse.orgbridge.operation;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.verse.orgbridge.integration.SalesforceRequestContext;
import org.verse.orgbridge.operation.contract.ComponentSelection;
import org.verse.orgbridge.operation.contract.OperationRequest;
import org.verse.orgbridge.operation.contract.OperationResponse;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperationServiceTest {

    @Mock
    private OperationRepository repository;

    private OperationService service;

    @BeforeEach
    void setUp() {
        Validator validator = Validation
                .buildDefaultValidatorFactory()
                .getValidator();
        service = new OperationService(
                repository,
                new ObjectMapper().findAndRegisterModules(),
                validator
        );
    }

    @Test
    void queuesTenantScopedComponentDeployment() {
        SalesforceRequestContext context = new SalesforceRequestContext(
                "00D000000000001AAA",
                "005000000000001AAA",
                "correlation-1"
        );
        OperationRequest request = new OperationRequest(
                OperationType.COMPONENT_DEPLOY,
                "source-org",
                "target-org",
                List.of(new ComponentSelection(
                        "CustomObject",
                        "Invoice__c",
                        null
                )),
                null,
                null
        );
        when(repository.save(any(OperationJob.class)))
                .thenAnswer(invocation ->
                        Mono.just(invocation.getArgument(0))
                );

        Mono<OperationResponse> result = service.submit(context, request);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.status()).isEqualTo("QUEUED");
                    assertThat(response.type())
                            .isEqualTo("COMPONENT_DEPLOY");
                    assertThat(response.totalItems()).isEqualTo(1);
                })
                .verifyComplete();

        ArgumentCaptor<OperationJob> captor =
                ArgumentCaptor.forClass(OperationJob.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getTenantKey())
                .isEqualTo("00D000000000001AAA:005000000000001AAA");
        assertThat(captor.getValue().getRequestJson())
                .contains("Invoice__c");
    }

    @Test
    void requiresTargetForComponentDeployment() {
        OperationRequest request = new OperationRequest(
                OperationType.COMPONENT_DEPLOY,
                "source-org",
                null,
                List.of(new ComponentSelection(
                        "CustomObject",
                        "Invoice__c",
                        null
                )),
                null,
                null
        );

        assertThatThrownBy(() -> service.submit(
                new SalesforceRequestContext(
                        "00D000000000001AAA",
                        "005000000000001AAA",
                        "correlation-2"
                ),
                request
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("targetOrganizationId is required");
    }
}
