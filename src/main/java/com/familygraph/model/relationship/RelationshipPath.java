package com.familygraph.model.relationship;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public record RelationshipPath(
        String sourceId,
        String ancestorId,
        List<String> nodeIds
) {
    public RelationshipPath {
        Objects.requireNonNull(sourceId, "sourceId must not be null");
        Objects.requireNonNull(ancestorId, "ancestorId must not be null");
        nodeIds = List.copyOf(Objects.requireNonNull(nodeIds, "nodeIds must not be null"));

        if (sourceId.isBlank()) {
            throw new IllegalArgumentException("sourceId must not be blank");
        }
        if (ancestorId.isBlank()) {
            throw new IllegalArgumentException("ancestorId must not be blank");
        }
        if (nodeIds.isEmpty()) {
            throw new IllegalArgumentException("nodeIds must not be empty");
        }
        if (nodeIds.stream().anyMatch(id -> id == null || id.isBlank())) {
            throw new IllegalArgumentException("nodeIds must contain non-blank ids");
        }
        if (!nodeIds.get(0).equals(sourceId)) {
            throw new IllegalArgumentException("nodeIds must start with sourceId");
        }
        if (!nodeIds.get(nodeIds.size() - 1).equals(ancestorId)) {
            throw new IllegalArgumentException("nodeIds must end with ancestorId");
        }
        if (new HashSet<>(nodeIds).size() != nodeIds.size()) {
            throw new IllegalArgumentException("nodeIds must not contain a cycle");
        }
    }

    public int edgeCount() {
        return nodeIds.size() - 1;
    }
}
