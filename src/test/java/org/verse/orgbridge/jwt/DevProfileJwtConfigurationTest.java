package org.verse.orgbridge.jwt;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.r2dbc.url=r2dbc:h2:mem:///orgbridge-dev;MODE=PostgreSQL",
        "spring.r2dbc.username=sa",
        "spring.r2dbc.password=",
        "spring.flyway.enabled=false",
        "app.integration.api-key="
                + "0123456789012345678901234567890123456789",
        "app.salesforce.production="
                + "https://login.salesforce.com/services/Soap/u/67.0",
        "app.salesforce.sandbox="
                + "https://test.salesforce.com/services/Soap/u/67.0",
        "app.operations.worker-enabled=false"
})
@ActiveProfiles("dev")
class DevProfileJwtConfigurationTest {

    @Autowired
    private JwtService jwtService;

    @Test
    void localDefaultSecretSignsUsableTokens() {
        String token = jwtService.generateToken(Map.of(), "dev@example.com");

        assertThat(jwtService.extractUsername(token))
                .isEqualTo("dev@example.com");
        assertThat(jwtService.validateToken(token, "dev@example.com"))
                .isTrue();
    }
}
