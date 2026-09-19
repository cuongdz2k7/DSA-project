package com.familygraph.model.familytree;

import com.familygraph.model.graph.Person;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public record FamilyNode(
        Person person,
        List<Integer> relationLevels
) {
    private static final Comparator<Integer> LEVEL_ORDER = Comparator
            .comparingInt((Integer level) -> Math.abs(level))
            .thenComparingInt(Integer::intValue);

    public FamilyNode {
        Objects.requireNonNull(person, "person must not be null");
        relationLevels = List.copyOf(
                Objects.requireNonNull(relationLevels, "relationLevels must not be null")
        );

        if (relationLevels.isEmpty()) {
            throw new IllegalArgumentException("relationLevels must not be empty");
        }
        if (relationLevels.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("relationLevels must not contain null");
        }
        if (new HashSet<>(relationLevels).size() != relationLevels.size()) {
            throw new IllegalArgumentException("relationLevels must not contain duplicates");
        }
        if (relationLevels.contains(0) && relationLevels.size() != 1) {
            throw new IllegalArgumentException("SELF relation level 0 must appear alone");
        }

        int expectedSign = Integer.signum(relationLevels.get(0));
        boolean mixedSigns = relationLevels.stream()
                .anyMatch(level -> Integer.signum(level) != expectedSign);
        if (mixedSigns) {
            throw new IllegalArgumentException("relationLevels must have the same direction");
        }

        for (int index = 1; index < relationLevels.size(); index++) {
            if (LEVEL_ORDER.compare(relationLevels.get(index - 1), relationLevels.get(index)) > 0) {
                throw new IllegalArgumentException("relationLevels must be sorted by absolute value");
            }
        }
    }
}
