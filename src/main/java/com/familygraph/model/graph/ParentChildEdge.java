package com.familygraph.model.graph;

public record ParentChildEdge(
        String parentId,
        String childId
) {}
