package com.familygraph.model.validation;

import com.familygraph.model.graph.FamilyGraph;
import com.familygraph.model.query.ProjectQuery;

public record NormalizedInput(
        FamilyGraph graph,
        ProjectQuery query
) {}
