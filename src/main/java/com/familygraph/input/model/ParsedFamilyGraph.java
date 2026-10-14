package com.familygraph.input.model;

import java.util.List;

/** Raw graph records together with their declared counts. */
public record ParsedFamilyGraph(
        int declaredPersonCount,
        List<ParsedPerson> persons,
        int declaredEdgeCount,
        List<ParsedParentChildEdge> edges
) {
    public ParsedFamilyGraph {
        persons = List.copyOf(persons);
        edges = List.copyOf(edges);
    }
}
