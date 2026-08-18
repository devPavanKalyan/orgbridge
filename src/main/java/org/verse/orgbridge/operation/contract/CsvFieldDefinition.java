package org.verse.orgbridge.operation.contract;

public record CsvFieldDefinition(
        String sobject,
        String fieldName,
        String label,
        String type,
        Integer length,
        String values,
        Boolean required
) {
}
