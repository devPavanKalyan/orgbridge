package org.verse.orgbridge.csv;

import java.util.List;

public record CsvValidationResult(
        boolean valid,
        int total,
        int validCount,
        int errorCount,
        List<CsvFieldError> errors
) {
}
