package org.verse.orgbridge.csv;

import org.junit.jupiter.api.Test;
import org.verse.orgbridge.operation.contract.CsvFieldDefinition;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CsvFieldValidationServiceTest {

    private final CsvFieldValidationService service =
            new CsvFieldValidationService();

    @Test
    void acceptsSupportedCustomFields() {
        CsvValidationResult result = service.validate(List.of(
                new CsvFieldDefinition(
                        "Account",
                        "External_Reference__c",
                        "External Reference",
                        "Text",
                        80,
                        null,
                        false
                ),
                new CsvFieldDefinition(
                        "Account",
                        "Customer_Tier__c",
                        "Customer Tier",
                        "Picklist",
                        null,
                        "Gold;Silver;Bronze",
                        false
                )
        ));

        assertThat(result.valid()).isTrue();
        assertThat(result.total()).isEqualTo(2);
        assertThat(result.validCount()).isEqualTo(2);
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void reportsRowsAndFieldsForInvalidDefinitions() {
        CsvValidationResult result = service.validate(List.of(
                new CsvFieldDefinition(
                        "Account",
                        "InvalidField",
                        "",
                        "Text",
                        500,
                        null,
                        false
                )
        ));

        assertThat(result.valid()).isFalse();
        assertThat(result.validCount()).isZero();
        assertThat(result.errors())
                .extracting(CsvFieldError::field)
                .containsExactlyInAnyOrder(
                        "fieldName",
                        "label",
                        "length"
                );
        assertThat(result.errors())
                .allMatch(error -> error.row() == 2);
    }

    @Test
    void rejectsEmptyInput() {
        CsvValidationResult result = service.validate(List.of());

        assertThat(result.valid()).isFalse();
        assertThat(result.total()).isZero();
    }
}
