package com.familygraph.familytree;

import com.familygraph.model.familytree.FamilyNode;
import com.familygraph.model.familytree.OrderedFamilyGraph;
import com.familygraph.model.familytree.TopoResult;
import com.familygraph.model.graph.FamilyGraph;
import com.familygraph.model.graph.ParentChildEdge;
import com.familygraph.model.graph.Person;
import com.familygraph.model.query.Direction;
import com.familygraph.model.query.FamilyTreeQuery;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/** Builds the filtered family graph that is passed to the labeling step. */
public class FamilyTreeBuilder {
    /**
     * Creates the requested ancestor, descendant, or two-way family view.
     * The graph and query are assumed to have been validated by the input module.
     */
    public OrderedFamilyGraph buildFamilyView(
            FamilyGraph graph,
            FamilyTreeQuery query,
            TopoResult topoResult
    ) {
        Map<String, Person> personsById = new LinkedHashMap<>();
        Map<String, List<ParentChildEdge>> parentsByChild = new HashMap<>();
        Map<String, List<ParentChildEdge>> childrenByParent = new HashMap<>();

        for (Person person : graph.persons()) {
            personsById.put(person.id(), person);
            parentsByChild.put(person.id(), new ArrayList<>());
            childrenByParent.put(person.id(), new ArrayList<>());
        }

        for (ParentChildEdge edge : graph.edges()) {
            parentsByChild.get(edge.childId()).add(edge);
            childrenByParent.get(edge.parentId()).add(edge);
        }

        Map<String, Set<Integer>> relationLevelsById = new LinkedHashMap<>();
        relationLevelsById.put(query.targetId(), new LinkedHashSet<>(List.of(0)));
        Set<ParentChildEdge> selectedEdges = new HashSet<>();
        int maxEdges = query.numberOfGenerations() - 1;

        if (query.direction() == Direction.ANCESTORS || query.direction() == Direction.BOTH) {
            collectRelationLevels(
                    query.targetId(),
                    maxEdges,
                    parentsByChild,
                    1,
                    true,
                    relationLevelsById,
                    selectedEdges
            );
        }

        if (query.direction() == Direction.DESCENDANTS || query.direction() == Direction.BOTH) {
            collectRelationLevels(
                    query.targetId(),
                    maxEdges,
                    childrenByParent,
                    -1,
                    false,
                    relationLevelsById,
                    selectedEdges
            );
        }

        List<String> filteredTopoOrder = topoResult.topoOrder().stream()
                .filter(relationLevelsById::containsKey)
                .toList();

        List<FamilyNode> nodes = filteredTopoOrder.stream()
                .map(personId -> new FamilyNode(
                        personsById.get(personId),
                        sortedLevels(relationLevelsById.get(personId))
                ))
                .toList();

        List<ParentChildEdge> edges = graph.edges().stream()
                .filter(selectedEdges::contains)
                .toList();

        return new OrderedFamilyGraph(query, filteredTopoOrder, nodes, edges);
    }

    private void collectRelationLevels(
            String targetId,
            int maxEdges,
            Map<String, List<ParentChildEdge>> adjacency,
            int levelStep,
            boolean movesToParent,
            Map<String, Set<Integer>> relationLevelsById,
            Set<ParentChildEdge> selectedEdges
    ) {
        Queue<TraversalState> queue = new ArrayDeque<>();
        Set<TraversalState> visitedStates = new HashSet<>();
        TraversalState start = new TraversalState(targetId, 0);
        queue.offer(start);
        visitedStates.add(start);

        while (!queue.isEmpty()) {
            TraversalState current = queue.poll();
            if (Math.abs(current.level()) == maxEdges) {
                continue;
            }

            for (ParentChildEdge edge : adjacency.get(current.personId())) {
                String nextPersonId = movesToParent ? edge.parentId() : edge.childId();
                int nextLevel = current.level() + levelStep;
                TraversalState next = new TraversalState(nextPersonId, nextLevel);

                selectedEdges.add(edge);
                relationLevelsById
                        .computeIfAbsent(nextPersonId, ignored -> new LinkedHashSet<>())
                        .add(nextLevel);

                if (visitedStates.add(next)) {
                    queue.offer(next);
                }
            }
        }
    }

    private List<Integer> sortedLevels(Set<Integer> levels) {
        return levels.stream()
                .sorted(Comparator.comparingInt((Integer level) -> Math.abs(level))
                        .thenComparingInt(Integer::intValue))
                .toList();
    }

    private record TraversalState(String personId, int level) {
    }
}
