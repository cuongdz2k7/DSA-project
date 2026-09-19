package com.familygraph.model.graph;

import java.util.Objects;

public record ParentChildEdge(
        String parentId,
        String childId
) {
    public ParentChildEdge {
        Objects.requireNonNull(parentId, "parentId must not be null");
        Objects.requireNonNull(childId, "childId must not be null");
        if (parentId.isBlank()) {
            throw new IllegalArgumentException("parentId must not be blank");
        }
        if (childId.isBlank()) {
            throw new IllegalArgumentException("childId must not be blank");
        }
    }
}
