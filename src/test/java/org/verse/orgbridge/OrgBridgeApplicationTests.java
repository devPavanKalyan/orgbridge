package org.verse.orgbridge;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.verse.orgbridge.integration.IntegrationAuthenticationWebFilter;

import java.util.List;
import java.util.Map;

@SpringBootTest
@AutoConfigureWebTestClient(timeout = "30s")
@ActiveProfiles("test")
class OrgBridgeApplicationTests {

    @Autowired
    private WebTestClient client;

    @Test
    void contextLoads() {
    }

    @Test
    void healthAndPingArePublicButIntegrationRequiresHeaders() {
        client.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");

        client.get()
                .uri("/api/v1/ping")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("Pong from OrgBridge");

        client.get()
                .uri("/api/v1/integration/dashboard")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void authenticatedIntegrationRequestReachesTheApi() {
        client.post()
                .uri("/api/v1/integration/csv/validate")
                .header(
                        IntegrationAuthenticationWebFilter
                                .INTEGRATION_KEY_HEADER,
                        "01234567890123456789012345678901"
                                + "23456789012345678901234567890123"
                )
                .header(
                        IntegrationAuthenticationWebFilter
                                .SALESFORCE_ORG_HEADER,
                        "00D000000000001AAA"
                )
                .header(
                        IntegrationAuthenticationWebFilter
                                .SALESFORCE_USER_HEADER,
                        "005000000000001AAA"
                )
                .bodyValue(Map.of(
                        "fields",
                        List.of(Map.of(
                                "sobject", "Account",
                                "fieldName", "External_Reference__c",
                                "label", "External Reference",
                                "type", "Text",
                                "length", 80,
                                "required", false
                        ))
                ))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists(
                        IntegrationAuthenticationWebFilter.CORRELATION_HEADER
                )
                .expectHeader().contentTypeCompatibleWith(
                        org.springframework.http.MediaType.APPLICATION_JSON
                )
                .expectBody()
                .jsonPath("$.valid").isEqualTo(true)
                .jsonPath("$.validCount").isEqualTo(1);
    }
}
