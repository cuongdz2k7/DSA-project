package com.familygraph.model.familytree;

import com.familygraph.model.graph.ParentChildEdge;
import com.familygraph.model.query.FamilyTreeQuery;
import com.familygraph.model.result.ProjectResult;

import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public record FamilyTreeResult(
        FamilyTreeQuery query,
        List<String> topoOrder,
        List<LabeledPerson> persons,
        List<ParentChildEdge> edges
) implements ProjectResult {
    public FamilyTreeResult {
        Objects.requireNonNull(query, "query must not be null");
        topoOrder = List.copyOf(Objects.requireNonNull(topoOrder, "topoOrder must not be null"));
        persons = List.copyOf(Objects.requireNonNull(persons, "persons must not be null"));
        edges = List.copyOf(Objects.requireNonNull(edges, "edges must not be null"));

        Set<String> topoIds = new HashSet<>(topoOrder);
        if (topoIds.size() != topoOrder.size()) {
            throw new IllegalArgumentException("topoOrder must not contain duplicate ids");
        }

        Set<String> personIds = persons.stream()
                .map(person -> person.person().id())
                .collect(Collectors.toSet());
        if (personIds.size() != persons.size()) {
            throw new IllegalArgumentException("persons must not contain duplicate people");
        }
        if (!topoIds.equals(personIds)) {
            throw new IllegalArgumentException("topoOrder and persons must contain the same ids");
        }
        List<String> personOrder = persons.stream()
                .map(person -> person.person().id())
                .toList();
        if (!topoOrder.equals(personOrder)) {
            throw new IllegalArgumentException("persons must follow topoOrder");
        }
        if (!personIds.contains(query.targetId())) {
            throw new IllegalArgumentException("persons must contain the query target");
        }

        boolean invalidEdge = edges.stream().anyMatch(edge ->
                !personIds.contains(edge.parentId()) || !personIds.contains(edge.childId()));
        if (invalidEdge) {
            throw new IllegalArgumentException("edge endpoints must belong to persons");
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
