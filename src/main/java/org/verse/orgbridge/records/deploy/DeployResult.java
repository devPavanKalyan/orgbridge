package org.verse.orgbridge.records.deploy;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DeployResult {

    private String deploymentId;
    private String status;
    private boolean success;
    private boolean done;

    private Summary summary;
    private List<ComponentFailure> failures;
    private List<ComponentSuccess> successes;

    @Data
    @Builder
    public static class Summary {
        private int totalComponents;
        private int deployed;
        private int errors;
        private int testsRun;
        private int testErrors;
    }

    @Data
    @Builder
    public static class ComponentFailure {
        private String fullName;
        private String type;
        private String fileName;
        private String problemType;
        private String message;
    }

    @Data
    @Builder
    public static class ComponentSuccess {
        private String fullName;
        private String type;
        private String fileName;
        private String id;
    }
}
