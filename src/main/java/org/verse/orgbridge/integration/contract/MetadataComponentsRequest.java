package org.verse.orgbridge.integration.contract;

import jakarta.validation.constraints.NotBlank;

public record MetadataComponentsRequest(
        @NotBlank String organizationId,
        @NotBlank String type
) {
}
