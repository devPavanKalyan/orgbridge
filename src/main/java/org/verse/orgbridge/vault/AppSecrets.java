package org.verse.orgbridge.vault;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
@Data
public class AppSecrets {

    private Jwt jwt;
    private Redis redis;
    private Frontend frontend;
    private Salesforce salesforce;
    private Integration integration;
    private Operations operations;

    public record Jwt(String secret, long expiration, long refreshExpiration) {
    }

    public record Redis(String prefix, long sessionTtlSeconds) {
    }

    public record Frontend(String url) {
    }

    public record Salesforce(
            String sandbox,
            String production,
            String apiVersion
    ) {
    }

    public record Integration(String apiKey) {
    }

    public record Operations(boolean workerEnabled, int workerConcurrency) {
    }
}


