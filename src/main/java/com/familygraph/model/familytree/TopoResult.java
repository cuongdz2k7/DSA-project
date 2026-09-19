package com.familygraph.model.familytree;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public record TopoResult(
        List<String> topoOrder
) {
    public TopoResult {
        topoOrder = List.copyOf(Objects.requireNonNull(topoOrder, "topoOrder must not be null"));
        if (topoOrder.stream().anyMatch(id -> id == null || id.isBlank())) {
            throw new IllegalArgumentException("topoOrder must contain non-blank ids");
        }
        if (new HashSet<>(topoOrder).size() != topoOrder.size()) {
            throw new IllegalArgumentException("topoOrder must not contain duplicate ids");
        }
    }
}
