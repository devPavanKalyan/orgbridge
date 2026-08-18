package org.verse.orgbridge.integration.contract;

import org.verse.orgbridge.organization.OrganizationProfile;

import java.time.Instant;

public record OrganizationSummary(
        String id,
        String organizationId,
        String name,
        String username,
        String environment,
        String loginUrl,
        String status,
        Instant lastVerifiedAt,
        boolean active
) {
    public static OrganizationSummary from(
            OrganizationProfile profile
    ) {
        return new OrganizationSummary(
                profile.getId(),
                profile.getSalesforceOrganizationId(),
                profile.getName(),
                profile.getUsername(),
                profile.getEnvironment(),
                profile.getLoginUrl(),
                profile.getStatus(),
                profile.getLastVerifiedAt(),
                profile.isActive()
        );
    }
}
