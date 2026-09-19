package com.familygraph.model.relationship;

import com.familygraph.model.graph.ParentChildEdge;
import com.familygraph.model.graph.Person;
import com.familygraph.model.query.CheckRelationshipQuery;
import com.familygraph.model.result.ProjectResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public record RelationshipResult(
        CheckRelationshipQuery query,
        List<String> commonAncestorIds,
        List<RelationshipPath> paths,
        List<Person> evidencePersons,
        List<ParentChildEdge> evidenceEdges
) implements ProjectResult {
    public RelationshipResult {
        Objects.requireNonNull(query, "query must not be null");
        commonAncestorIds = List.copyOf(
                Objects.requireNonNull(commonAncestorIds, "commonAncestorIds must not be null")
        );
        paths = List.copyOf(Objects.requireNonNull(paths, "paths must not be null"));
        evidencePersons = List.copyOf(
                Objects.requireNonNull(evidencePersons, "evidencePersons must not be null")
        );
        evidenceEdges = List.copyOf(
                Objects.requireNonNull(evidenceEdges, "evidenceEdges must not be null")
        );

        validateCommonAncestors(commonAncestorIds);
        validateEvidencePersons(evidencePersons);
        validatePaths(query, commonAncestorIds, paths);
        validatePathOrder(query, commonAncestorIds, paths);
        validatePathEvidence(paths, evidencePersons, evidenceEdges);
    }

    private static void validatePathOrder(
            CheckRelationshipQuery query,
            List<String> commonAncestorIds,
            List<RelationshipPath> paths
    ) {
        int expectedSize = commonAncestorIds.size() * 2;
        if (paths.size() != expectedSize) {
            throw new IllegalArgumentException("each common ancestor must have exactly two paths");
        }
        for (int index = 0; index < commonAncestorIds.size(); index++) {
            String ancestorId = commonAncestorIds.get(index);
            RelationshipPath firstPath = paths.get(index * 2);
            RelationshipPath secondPath = paths.get(index * 2 + 1);
            if (!firstPath.ancestorId().equals(ancestorId)
                    || !firstPath.sourceId().equals(query.firstPersonId())) {
                throw new IllegalArgumentException("paths must list the first person before the second person");
            }
            if (!secondPath.ancestorId().equals(ancestorId)
                    || !secondPath.sourceId().equals(query.secondPersonId())) {
                throw new IllegalArgumentException("paths must be grouped by common ancestor");
            }
        }
    }

    public boolean related() {
        return !commonAncestorIds.isEmpty();
    }

    private static void validateCommonAncestors(List<String> commonAncestorIds) {
        if (commonAncestorIds.stream().anyMatch(id -> id == null || id.isBlank())) {
            throw new IllegalArgumentException("commonAncestorIds must contain non-blank ids");
        }
        if (new HashSet<>(commonAncestorIds).size() != commonAncestorIds.size()) {
            throw new IllegalArgumentException("commonAncestorIds must not contain duplicates");
        }
        List<String> sortedIds = new ArrayList<>(commonAncestorIds);
        sortedIds.sort(String::compareTo);
        if (!sortedIds.equals(commonAncestorIds)) {
            throw new IllegalArgumentException("commonAncestorIds must be sorted by id");
        }
    }

    private static void validateEvidencePersons(List<Person> evidencePersons) {
        Set<String> personIds = evidencePersons.stream()
                .map(Person::id)
                .collect(Collectors.toSet());
        if (personIds.size() != evidencePersons.size()) {
            throw new IllegalArgumentException("evidencePersons must not contain duplicate people");
        }
    }

    private static void validatePaths(
            CheckRelationshipQuery query,
            List<String> commonAncestorIds,
            List<RelationshipPath> paths
    ) {
        if (commonAncestorIds.isEmpty() && !paths.isEmpty()) {
            throw new IllegalArgumentException("paths must be empty when there is no common ancestor");
        }

        Set<String> sourceIds = Set.of(query.firstPersonId(), query.secondPersonId());
        Map<String, Set<String>> sourcesByAncestor = new HashMap<>();
        for (RelationshipPath path : paths) {
            if (!sourceIds.contains(path.sourceId())) {
                throw new IllegalArgumentException("path source must belong to the query");
            }
            if (!commonAncestorIds.contains(path.ancestorId())) {
                throw new IllegalArgumentException("path ancestor must be a common ancestor");
            }
            Set<String> sources = sourcesByAncestor.computeIfAbsent(
                    path.ancestorId(),
                    ignored -> new HashSet<>()
            );
            if (!sources.add(path.sourceId())) {
                throw new IllegalArgumentException("each source may have only one path to an ancestor");
            }
        }

        for (String ancestorId : commonAncestorIds) {
            if (!sourceIds.equals(sourcesByAncestor.get(ancestorId))) {
                throw new IllegalArgumentException("each common ancestor must have one path from each query person");
            }
        }
    }

    private static void validatePathEvidence(
            List<RelationshipPath> paths,
            List<Person> evidencePersons,
            List<ParentChildEdge> evidenceEdges
    ) {
        Set<String> personIds = evidencePersons.stream()
                .map(Person::id)
                .collect(Collectors.toSet());
        Set<ParentChildEdge> edgeSet = new HashSet<>(evidenceEdges);
        if (edgeSet.size() != evidenceEdges.size()) {
            throw new IllegalArgumentException("evidenceEdges must not contain duplicates");
        }

        for (RelationshipPath path : paths) {
            if (!personIds.containsAll(path.nodeIds())) {
                throw new IllegalArgumentException("evidencePersons must cover every path node");
            }
            for (int index = 0; index < path.nodeIds().size() - 1; index++) {
                String childId = path.nodeIds().get(index);
                String parentId = path.nodeIds().get(index + 1);
                if (!edgeSet.contains(new ParentChildEdge(parentId, childId))) {
                    throw new IllegalArgumentException("evidenceEdges must cover every path step");
                }
            }
        }
    }
}
