package com.familygraph.model;

public record FamilyTreeQuery(
        String targetId,
        int numberOfGenerations,
        Direction direction
) implements ProjectQuery {
}
