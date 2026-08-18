package org.verse.orgbridge.organization;

public record OrganizationProfileRequest(
        String name,
        String username,
        String environment,
        String loginUrl,
        boolean active
) {
}
