package com.familygraph.model.familytree;

import com.familygraph.model.graph.Person;

import java.util.List;

public record LabeledPerson(
        Person person,
        List<RelationLabel> relations
) {
    public LabeledPerson {
        relations = List.copyOf(relations);
    }
}
