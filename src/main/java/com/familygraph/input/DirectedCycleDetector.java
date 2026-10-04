package com.familygraph.input;

import com.familygraph.input.model.ParsedFamilyGraph;
import com.familygraph.input.model.ParsedParentChildEdge;
import com.familygraph.input.model.ParsedPerson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Finds the first deterministic directed cycle in input order. */
public final class DirectedCycleDetector {
    public List<String> findCycle(ParsedFamilyGraph graph) {
        Map<String, List<String>> childrenByParent = new LinkedHashMap<>();
        for (ParsedPerson person : graph.persons()) {
            childrenByParent.put(person.id(), new ArrayList<>());
        }
        for (ParsedParentChildEdge edge : graph.edges()) {
            childrenByParent.get(edge.parentId()).add(edge.childId());
        }

        Map<String, VisitState> states = new HashMap<>();
        List<String> activePath = new ArrayList<>();
        Map<String, Integer> activeIndexes = new HashMap<>();
        for (ParsedPerson person : graph.persons()) {
            if (states.getOrDefault(person.id(), VisitState.UNVISITED) == VisitState.UNVISITED) {
                List<String> cycle = search(
                        person.id(), childrenByParent, states, activePath, activeIndexes
                );
                if (!cycle.isEmpty()) {
                    return cycle;
                }
            }
        }
        return List.of();
    }

    private List<String> search(
            String personId,
            Map<String, List<String>> childrenByParent,
            Map<String, VisitState> states,
            List<String> activePath,
            Map<String, Integer> activeIndexes
    ) {
        states.put(personId, VisitState.VISITING);
        activeIndexes.put(personId, activePath.size());
        activePath.add(personId);

        for (String childId : childrenByParent.get(personId)) {
            VisitState state = states.getOrDefault(childId, VisitState.UNVISITED);
            if (state == VisitState.VISITING) {
                List<String> cycle = new ArrayList<>(
                        activePath.subList(activeIndexes.get(childId), activePath.size())
                );
                cycle.add(childId);
                return cycle;
            }
            if (state == VisitState.UNVISITED) {
                List<String> cycle = search(
                        childId, childrenByParent, states, activePath, activeIndexes
                );
                if (!cycle.isEmpty()) {
                    return cycle;
                }
            }
        }

        activePath.remove(activePath.size() - 1);
        activeIndexes.remove(personId);
        states.put(personId, VisitState.VISITED);
        return List.of();
    }

    private enum VisitState {
        UNVISITED,
        VISITING,
        VISITED
    }
}
