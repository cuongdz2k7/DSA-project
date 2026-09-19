package com.familygraph.model.graph;

import java.util.List;
public record FamilyGraph(
        List<Person> persons,
        List<ParentChildEdge> edges
) {
    public FamilyGraph {
        persons = List.copyOf(persons);
        edges = List.copyOf(edges);
    }
}
