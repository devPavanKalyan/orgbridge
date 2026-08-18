package org.verse.orgbridge.csv;

import org.springframework.stereotype.Service;
import org.verse.orgbridge.operation.contract.CsvFieldDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class CsvFieldValidationService {

    private static final Pattern API_NAME =
            Pattern.compile("^[A-Za-z][A-Za-z0-9_]*$");

    private static final Set<String> SUPPORTED_TYPES = Set.of(
            "text",
            "textarea",
            "picklist",
            "email",
            "phone",
            "url",
            "date",
            "datetime"
    );

    public CsvValidationResult validate(
            List<CsvFieldDefinition> fields
    ) {
        List<CsvFieldError> errors = new ArrayList<>();
        List<CsvFieldDefinition> safeFields =
                fields == null ? List.of() : fields;

        for (int index = 0; index < safeFields.size(); index++) {
            validateField(safeFields.get(index), index + 2, errors);
        }

        int invalidRows = (int) errors.stream()
                .map(CsvFieldError::row)
                .distinct()
                .count();

        return new CsvValidationResult(
                errors.isEmpty() && !safeFields.isEmpty(),
                safeFields.size(),
                Math.max(0, safeFields.size() - invalidRows),
                errors.size(),
                List.copyOf(errors)
        );
    }

    private void validateField(
            CsvFieldDefinition field,
            int row,
            List<CsvFieldError> errors
    ) {
        if (field == null) {
            errors.add(new CsvFieldError(row, "row", "Field definition is required"));
            return;
        }

        validateApiName(field.sobject(), "sobject", row, errors);
        validateApiName(field.fieldName(), "fieldName", row, errors);

        if (field.fieldName() != null
                && !field.fieldName().endsWith("__c")) {
            errors.add(new CsvFieldError(
                    row,
                    "fieldName",
                    "Custom field API names must end with __c"
            ));
        }

        if (blank(field.label())) {
            errors.add(new CsvFieldError(row, "label", "Label is required"));
        }

        if (blank(field.type())) {
            errors.add(new CsvFieldError(row, "type", "Type is required"));
            return;
        }

        String type = field.type().toLowerCase(Locale.ROOT);

        if (!SUPPORTED_TYPES.contains(type)) {
            errors.add(new CsvFieldError(
                    row,
                    "type",
                    "Unsupported field type: " + field.type()
            ));
            return;
        }

        if ("text".equals(type)
                && (field.length() == null
                || field.length() < 1
                || field.length() > 255)) {
            errors.add(new CsvFieldError(
                    row,
                    "length",
                    "Text length must be between 1 and 255"
            ));
        }

        if ("picklist".equals(type) && blank(field.values())) {
            errors.add(new CsvFieldError(
                    row,
                    "values",
                    "Picklist values are required"
            ));
        }
    }

    private void validateApiName(
            String value,
            String field,
            int row,
            List<CsvFieldError> errors
    ) {
        if (blank(value)) {
            errors.add(new CsvFieldError(row, field, field + " is required"));
        } else if (!API_NAME.matcher(value).matches()) {
            errors.add(new CsvFieldError(row, field, field + " is invalid"));
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
