package com.familygraph.model.query;

import java.util.Objects;

public record FamilyTreeQuery(
        String targetId,
        int numberOfGenerations,
        Direction direction
) implements ProjectQuery {
    public FamilyTreeQuery {
        Objects.requireNonNull(targetId, "targetId must not be null");
        Objects.requireNonNull(direction, "direction must not be null");
        if (targetId.isBlank()) {
            throw new IllegalArgumentException("targetId must not be blank");
        }
        if (numberOfGenerations < 1) {
            throw new IllegalArgumentException("numberOfGenerations must be at least 1");
        }
    }
}
