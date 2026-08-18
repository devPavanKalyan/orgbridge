package org.verse.orgbridge.records.types;

import lombok.Builder;

@Builder
public record TypesRequestPayload(String userId, String type) {
}