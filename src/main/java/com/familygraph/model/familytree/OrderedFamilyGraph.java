package com.familygraph.model.familytree;

import com.familygraph.model.graph.ParentChildEdge;
import com.familygraph.model.query.FamilyTreeQuery;

import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public record OrderedFamilyGraph(
        FamilyTreeQuery query,
        List<String> topoOrder,
        List<FamilyNode> nodes,
        List<ParentChildEdge> edges
) {
    public OrderedFamilyGraph {
        Objects.requireNonNull(query, "query must not be null");
        topoOrder = List.copyOf(Objects.requireNonNull(topoOrder, "topoOrder must not be null"));
        nodes = List.copyOf(Objects.requireNonNull(nodes, "nodes must not be null"));
        edges = List.copyOf(Objects.requireNonNull(edges, "edges must not be null"));

        Set<String> topoIds = new HashSet<>(topoOrder);
        if (topoIds.size() != topoOrder.size()) {
            throw new IllegalArgumentException("topoOrder must not contain duplicate ids");
        }

        Set<String> nodeIds = nodes.stream()
                .map(node -> node.person().id())
                .collect(Collectors.toSet());
        if (nodeIds.size() != nodes.size()) {
            throw new IllegalArgumentException("nodes must not contain duplicate persons");
        }
        if (!topoIds.equals(nodeIds)) {
            throw new IllegalArgumentException("topoOrder and nodes must contain the same ids");
        }
        List<String> nodeOrder = nodes.stream()
                .map(node -> node.person().id())
                .toList();
        if (!topoOrder.equals(nodeOrder)) {
            throw new IllegalArgumentException("nodes must follow topoOrder");
        }
        if (!nodeIds.contains(query.targetId())) {
            throw new IllegalArgumentException("nodes must contain the query target");
        }

        boolean invalidEdge = edges.stream().anyMatch(edge ->
                !nodeIds.contains(edge.parentId()) || !nodeIds.contains(edge.childId()));
        if (invalidEdge) {
            throw new IllegalArgumentException("edge endpoints must belong to nodes");
        }
        validateTopologicalEdges(topoOrder, edges);
    }

    private static void validateTopologicalEdges(
            List<String> topoOrder,
            List<ParentChildEdge> edges
    ) {
        Map<String, Integer> positions = new HashMap<>();
        for (int index = 0; index < topoOrder.size(); index++) {
            positions.put(topoOrder.get(index), index);
        }
        boolean invalidOrder = edges.stream().anyMatch(edge ->
                positions.get(edge.parentId()) >= positions.get(edge.childId()));
        if (invalidOrder) {
            throw new IllegalArgumentException("topoOrder must place every parent before its child");
        }
    }
}
