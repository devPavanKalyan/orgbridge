package org.verse.orgbridge.csv;

public record CsvFieldError(
        int row,
        String field,
        String message
) {
}
