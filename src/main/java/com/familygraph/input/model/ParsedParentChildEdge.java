package com.familygraph.input.model;

/** Parent-child edge read from text before domain validation. */
public record ParsedParentChildEdge(
        String parentId,
        String childId
) {
}
