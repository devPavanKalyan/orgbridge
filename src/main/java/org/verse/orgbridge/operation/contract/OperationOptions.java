package org.verse.orgbridge.operation.contract;

public record OperationOptions(
        Boolean rollbackOnError,
        String testLevel
) {
    public boolean rollbackOnErrorOrDefault() {
        return rollbackOnError == null || rollbackOnError;
    }

    public String testLevelOrDefault() {
        return testLevel == null || testLevel.isBlank()
                ? "NoTestRun"
                : testLevel;
    }
}
