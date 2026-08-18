package org.verse.orgbridge.integration.contract;

import org.verse.orgbridge.operation.contract.CsvFieldDefinition;

import java.util.List;

public record CsvValidationRequest(List<CsvFieldDefinition> fields) {
}
