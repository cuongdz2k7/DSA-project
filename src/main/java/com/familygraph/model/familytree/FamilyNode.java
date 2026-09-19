package com.familygraph.model.familytree;

import com.familygraph.model.graph.Person;

import java.util.List;

public record FamilyNode(
        Person person,
        List<Integer> relationLevels
) {
    public FamilyNode {
        relationLevels = List.copyOf(relationLevels);
    }
}
