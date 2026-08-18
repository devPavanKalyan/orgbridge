package org.verse.orgbridge.operation.contract;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.verse.orgbridge.operation.OperationType;

import java.util.List;

public record OperationRequest(
        @NotNull OperationType type,
        String sourceOrganizationId,
        String targetOrganizationId,
        List<@Valid ComponentSelection> components,
        List<CsvFieldDefinition> fields,
        OperationOptions options
) {
    public int itemCount() {
        if (components != null) {
            return components.size();
        }
        return fields == null ? 0 : fields.size();
    }
}
