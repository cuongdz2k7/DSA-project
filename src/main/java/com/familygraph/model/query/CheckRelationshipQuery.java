package com.familygraph.model.query;

public record CheckRelationshipQuery(
        String firstPersonId,
        String secondPersonId
) implements ProjectQuery {
    public static final int MAX_GENERATIONS = 3;
}
