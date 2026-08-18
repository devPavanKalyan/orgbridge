package org.verse.orgbridge.records.deploy;

@lombok.Builder
public record DeployPayload(String userId, String base64) {
}