package org.verse.orgbridge.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IntegrationRoutesTest {

    private IntegrationHandler handler;
    private WebTestClient client;

    @BeforeEach
    void setUp() {
        handler = mock(IntegrationHandler.class);
        when(handler.dashboard(any()))
                .thenReturn(ServerResponse.ok().bodyValue(Map.of()));
        when(handler.organizations(any()))
                .thenReturn(ServerResponse.ok().bodyValue(List.of()));
        when(handler.createOrganization(any()))
                .thenReturn(ServerResponse.status(201).bodyValue(
                        Map.of("id", "organization-one")
                ));
        when(handler.updateOrganization(any()))
                .thenReturn(ServerResponse.ok().bodyValue(
                        Map.of("id", "organization-one")
                ));
        when(handler.deleteOrganization(any()))
                .thenReturn(ServerResponse.noContent().build());
        when(handler.activateOrganization(any()))
                .thenReturn(ServerResponse.ok().bodyValue(
                        Map.of("active", true)
                ));
        when(handler.testOrganization(any()))
                .thenReturn(ServerResponse.ok().bodyValue(
                        Map.of("status", "Needs Verification")
                ));
        when(handler.metadataTypes(any()))
                .thenReturn(ServerResponse.ok().bodyValue(List.of()));
        when(handler.metadataComponents(any()))
                .thenReturn(ServerResponse.ok().bodyValue(List.of()));
        when(handler.validateCsv(any()))
                .thenReturn(ServerResponse.ok().bodyValue(Map.of()));
        when(handler.submitOperation(any()))
                .thenReturn(ServerResponse.accepted().bodyValue(Map.of()));
        when(handler.operations(any()))
                .thenReturn(ServerResponse.ok().bodyValue(List.of()));
        when(handler.operation(any()))
                .thenReturn(ServerResponse.ok().bodyValue(Map.of()));

        client = WebTestClient
                .bindToRouterFunction(
                        new IntegrationRoutes().integrationApi(handler)
                )
                .build();
    }

    @Test
    void exposesOrganizationOperationsUnderVersionedApi() {
        client.get()
                .uri("/api/v1/integration/organizations")
                .exchange()
                .expectStatus().isOk();

        client.post()
                .uri("/api/v1/integration/organizations")
                .bodyValue("{}")
                .exchange()
                .expectStatus().isCreated();

        client.put()
                .uri("/api/v1/integration/organizations/organization-one")
                .bodyValue("{}")
                .exchange()
                .expectStatus().isOk();

        client.post()
                .uri(
                        "/api/v1/integration/organizations/"
                                + "organization-one/activate"
                )
                .exchange()
                .expectStatus().isOk();

        client.post()
                .uri(
                        "/api/v1/integration/organizations/"
                                + "organization-one/test"
                )
                .exchange()
                .expectStatus().isOk();

        client.delete()
                .uri("/api/v1/integration/organizations/organization-one")
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void doesNotExposeTheOldIntegrationPath() {
        client.get()
                .uri("/api/integration/v1/organizations")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void exposesDashboardMetadataCsvAndOperationRoutes() {
        client.get()
                .uri("/api/v1/integration/dashboard")
                .exchange()
                .expectStatus().isOk();

        client.get()
                .uri(
                        "/api/v1/integration/metadata/types"
                                + "?organizationId=organization-one"
                )
                .exchange()
                .expectStatus().isOk();

        client.post()
                .uri("/api/v1/integration/metadata/components")
                .bodyValue("{}")
                .exchange()
                .expectStatus().isOk();

        client.post()
                .uri("/api/v1/integration/csv/validate")
                .bodyValue("{}")
                .exchange()
                .expectStatus().isOk();

        client.post()
                .uri("/api/v1/integration/operations")
                .bodyValue("{}")
                .exchange()
                .expectStatus().isAccepted();

        client.get()
                .uri("/api/v1/integration/operations")
                .exchange()
                .expectStatus().isOk();

        client.get()
                .uri("/api/v1/integration/operations/operation-one")
                .exchange()
                .expectStatus().isOk();
    }
}
