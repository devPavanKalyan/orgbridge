package org.verse.orgbridge.records.types;

@lombok.Builder
public record ComponentItem(
        String id,
        String name,
        String type,
        String parent,
        String modifiedBy,
        String modifiedDate,
        String manageableState,
        boolean selected
) {
}