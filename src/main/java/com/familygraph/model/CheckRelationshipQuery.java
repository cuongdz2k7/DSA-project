package com.familygraph.model;

public record CheckRelationshipQuery(
        String firstPersonId,
        String secondPersonId
) implements ProjectQuery {
}
