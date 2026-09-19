package com.familygraph.model.graph;

import java.util.Objects;

public record Person(
        String id,
        Gender gender,
        String name
) {
    public Person {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(gender, "gender must not be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        name = name == null ? "" : name;
    }
}
