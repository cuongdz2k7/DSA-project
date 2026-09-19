package com.familygraph.model.query;

public record FamilyTreeQuery(
        String targetId,
        int numberOfGenerations,
        Direction direction
) implements ProjectQuery {}
