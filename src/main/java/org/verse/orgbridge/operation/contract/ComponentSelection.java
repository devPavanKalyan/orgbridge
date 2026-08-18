package org.verse.orgbridge.operation.contract;

import jakarta.validation.constraints.NotBlank;

public record ComponentSelection(
        @NotBlank String type,
        @NotBlank String name,
        String parent
) {
    public String fullName() {
        return parent == null || parent.isBlank()
                ? name
                : parent + "." + name;
    }
}
