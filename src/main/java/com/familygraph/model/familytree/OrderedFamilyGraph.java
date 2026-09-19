package com.familygraph.model.familytree;

import com.familygraph.model.graph.ParentChildEdge;
import com.familygraph.model.query.FamilyTreeQuery;

import java.util.List;

public record OrderedFamilyGraph(
        FamilyTreeQuery query,
        List<String> topoOrder,
        List<FamilyNode> nodes,
        List<ParentChildEdge> edges
) {
    public OrderedFamilyGraph {
        topoOrder = List.copyOf(topoOrder);
        nodes = List.copyOf(nodes);
        edges = List.copyOf(edges);
    }
}
