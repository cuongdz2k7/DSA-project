package com.familygraph.model.familytree;

import com.familygraph.model.graph.ParentChildEdge;
import com.familygraph.model.query.FamilyTreeQuery;
import com.familygraph.model.result.ProjectResult;

import java.util.List;

public record FamilyTreeResult(
        FamilyTreeQuery query,
        List<String> topoOrder,
        List<LabeledPerson> persons,
        List<ParentChildEdge> edges
) implements ProjectResult {
    public FamilyTreeResult {
        topoOrder = List.copyOf(topoOrder);
        persons = List.copyOf(persons);
        edges = List.copyOf(edges);
    }
}
