package com.familygraph.model;

import java.util.List;

public record FamilyGraph(
        List<Person> persons,
        List<ParentChildEdge> edges
) {
}
