package com.familygraph.model.familytree;

import com.familygraph.model.graph.Person;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public record LabeledPerson(
        Person person,
        List<RelationLabel> relations
) {
    private static final Comparator<RelationLabel> RELATION_ORDER = Comparator
            .comparingInt((RelationLabel relation) -> Math.abs(relation.generationOffset()))
            .thenComparing(RelationLabel::code);

    public LabeledPerson {
        Objects.requireNonNull(person, "person must not be null");
        relations = List.copyOf(Objects.requireNonNull(relations, "relations must not be null"));
        if (relations.isEmpty()) {
            throw new IllegalArgumentException("relations must not be empty");
        }
        if (new HashSet<>(relations).size() != relations.size()) {
            throw new IllegalArgumentException("relations must not contain duplicates");
        }
        for (int index = 1; index < relations.size(); index++) {
            if (RELATION_ORDER.compare(relations.get(index - 1), relations.get(index)) > 0) {
                throw new IllegalArgumentException("relations must be sorted by distance and code");
            }
        }
    }
}
