package org.verse.orgbridge.organization;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("salesforce_organizations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationProfile {

    @Id
    private String id;

    @Column("tenant_key")
    private String tenantKey;

    @Column("salesforce_organization_id")
    private String salesforceOrganizationId;

    private String name;
    private String username;
    private String environment;

    @Column("login_url")
    private String loginUrl;

    private String status;
    private boolean active;

    @Column("last_verified_at")
    private Instant lastVerifiedAt;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;

    @Version
    private Long version;
}
