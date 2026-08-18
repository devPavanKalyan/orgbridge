package org.verse.orgbridge.vault;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VaultService {

    private final AppSecrets secrets;

    public String jwtSecret() {
        return secrets.getJwt().secret();
    }

    public String frontendUrl() {
        return secrets.getFrontend().url();
    }

    public long jwtExpiration() {
        return secrets.getJwt().expiration();
    }

    public long jwtRefreshExpiration() {
        return secrets.getJwt().refreshExpiration();
    }

    public String redisKeyPrefix() {
        return secrets.getRedis().prefix();
    }

    public long redisSessionTtlSeconds() {
        long configured = secrets.getRedis().sessionTtlSeconds();
        return configured <= 0 ? 7200 : configured;
    }

    public String sandbox() {
        return secrets.getSalesforce().sandbox();
    }

    public String production() {
        return secrets.getSalesforce().production();
    }

    public String salesforceApiVersion() {
        String apiVersion = secrets.getSalesforce().apiVersion();
        return apiVersion == null || apiVersion.isBlank()
                ? "67.0"
                : apiVersion;
    }

    public String integrationApiKey() {
        String apiKey = secrets.getIntegration().apiKey();

        if (apiKey == null || apiKey.length() < 32) {
            throw new IllegalStateException(
                    "Integration API key must contain at least 32 characters"
            );
        }

        return apiKey;
    }

    public boolean operationWorkerEnabled() {
        return secrets.getOperations() != null
                && secrets.getOperations().workerEnabled();
    }

    public int operationWorkerConcurrency() {
        if (secrets.getOperations() == null) {
            return 2;
        }
        return Math.max(1, secrets.getOperations().workerConcurrency());
    }

}
