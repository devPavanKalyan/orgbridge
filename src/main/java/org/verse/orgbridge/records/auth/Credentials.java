package org.verse.orgbridge.records.auth;

@lombok.Builder
public record Credentials(String org, String sessionId, String endpoint) {
}