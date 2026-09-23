package com.familygraph.model.relationship;

import com.familygraph.model.graph.FamilyGraph;
import com.familygraph.model.graph.ParentChildEdge;
import com.familygraph.model.graph.Person;
import com.familygraph.model.query.CheckRelationshipQuery;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public final class RelationshipChecker {

    private RelationshipChecker() {
    }

    public static RelationshipResult checkRelationship(
            FamilyGraph graph,
            CheckRelationshipQuery query
    ) {
        Map<String, List<String>> parentsByChild = buildParentsByChild(graph);

        Map<String, RelationshipPath> firstAncestorPaths = findAncestorPaths(
                query.firstPersonId(),
                parentsByChild
        );
        Map<String, RelationshipPath> secondAncestorPaths = findAncestorPaths(
                query.secondPersonId(),
                parentsByChild
        );

        List<String> commonAncestorIds = firstAncestorPaths.keySet().stream()
                .filter(secondAncestorPaths::containsKey)
                .sorted()
                .toList();

        List<RelationshipPath> paths = new ArrayList<>();
        for (String ancestorId : commonAncestorIds) {
            paths.add(firstAncestorPaths.get(ancestorId));
            paths.add(secondAncestorPaths.get(ancestorId));
        }

        Set<String> evidencePersonIds = new HashSet<>();
        Set<EdgeKey> evidenceEdgeKeys = new HashSet<>();
        for (RelationshipPath path : paths) {
            evidencePersonIds.addAll(path.nodeIds());

            for (int i = 0; i < path.nodeIds().size() - 1; i++) {
                String childId = path.nodeIds().get(i);
                String parentId = path.nodeIds().get(i + 1);
                evidenceEdgeKeys.add(new EdgeKey(parentId, childId));
            }
        }

        List<Person> evidencePersons = graph.persons().stream()
                .filter(person -> evidencePersonIds.contains(person.id()))
                .toList();
        List<ParentChildEdge> evidenceEdges = graph.edges().stream()
                .filter(edge -> evidenceEdgeKeys.contains(
                        new EdgeKey(edge.parentId(), edge.childId())
                ))
                .toList();

        return new RelationshipResult(
                query,
                commonAncestorIds,
                paths,
                evidencePersons,
                evidenceEdges
        );
    }

    private static Map<String, List<String>> buildParentsByChild(
            FamilyGraph graph
    ) {
        Map<String, List<String>> parentsByChild = new HashMap<>();

        for (ParentChildEdge edge : graph.edges()) {
            parentsByChild
                    .computeIfAbsent(edge.childId(), ignored -> new ArrayList<>())
                    .add(edge.parentId());
        }

        return parentsByChild;
    }

    private static Map<String, RelationshipPath> findAncestorPaths(
            String sourceId,
            Map<String, List<String>> parentsByChild
    ) {
        Map<String, RelationshipPath> ancestorPaths = new LinkedHashMap<>();
        Queue<RelationshipPath> queue = new ArrayDeque<>();
        Set<String> visited = new LinkedHashSet<>();
        int maxEdges = CheckRelationshipQuery.MAX_GENERATIONS - 1;

        queue.add(new RelationshipPath(sourceId, sourceId, List.of(sourceId)));
        visited.add(sourceId);

        while (!queue.isEmpty()) {
            RelationshipPath currentPath = queue.remove();
            String currentId = currentPath.ancestorId();
            ancestorPaths.put(currentId, currentPath);

            if (currentPath.edgeCount() >= maxEdges) {
                continue;
            }

            for (String parentId : parentsByChild.getOrDefault(currentId, List.of())) {
                if (!visited.add(parentId)) {
                    continue;
                }

                List<String> nodeIds = new ArrayList<>(currentPath.nodeIds());
                nodeIds.add(parentId);
                queue.add(new RelationshipPath(sourceId, parentId, nodeIds));
            }
        }

        return ancestorPaths;
    }

    private record EdgeKey(String parentId, String childId) {
    }
}
