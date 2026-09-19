package com.familygraph.model.validation;

import com.familygraph.model.graph.FamilyGraph;
import com.familygraph.model.query.ProjectQuery;

import java.util.Objects;

public record NormalizedInput(
        FamilyGraph graph,
        ProjectQuery query
) {
    public NormalizedInput {
        Objects.requireNonNull(graph, "graph must not be null");
        Objects.requireNonNull(query, "query must not be null");
    }
}
