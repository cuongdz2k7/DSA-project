package com.familygraph.model.query;

import java.util.Objects;

public record CheckRelationshipQuery(
        String firstPersonId,
        String secondPersonId
) implements ProjectQuery {
    public static final int MAX_GENERATIONS = 3;

    public CheckRelationshipQuery {
        Objects.requireNonNull(firstPersonId, "firstPersonId must not be null");
        Objects.requireNonNull(secondPersonId, "secondPersonId must not be null");
        if (firstPersonId.isBlank()) {
            throw new IllegalArgumentException("firstPersonId must not be blank");
        }
        if (secondPersonId.isBlank()) {
            throw new IllegalArgumentException("secondPersonId must not be blank");
        }
        if (firstPersonId.equals(secondPersonId)) {
            throw new IllegalArgumentException("person ids must be different");
        }
    }
}
