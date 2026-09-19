package com.familygraph.model.relationship;

import java.util.List;

public record RelationshipPath(
        String sourceId,
        String ancestorId,
        List<String> nodeIds
) {
    public RelationshipPath {
        nodeIds = List.copyOf(nodeIds);
    }

    public int edgeCount() {
        return nodeIds.size() - 1;
    }
}
