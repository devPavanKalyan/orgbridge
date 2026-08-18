package org.verse.orgbridge.records.retrieve;

import lombok.Builder;

import java.util.List;


@Builder
public record RetrieveXMLPayload(
        String sessionId,
        String apiVersion,
        boolean singlePackage,
        List<RetrieveType> types
) {
}
