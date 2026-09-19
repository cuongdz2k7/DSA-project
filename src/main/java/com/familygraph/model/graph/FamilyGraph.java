package com.familygraph.model.graph;

import java.util.List;
import java.util.Objects;

public record FamilyGraph(
        List<Person> persons,
        List<ParentChildEdge> edges
) {
    public FamilyGraph {
        persons = List.copyOf(Objects.requireNonNull(persons, "persons must not be null"));
        edges = List.copyOf(Objects.requireNonNull(edges, "edges must not be null"));
    }
}
