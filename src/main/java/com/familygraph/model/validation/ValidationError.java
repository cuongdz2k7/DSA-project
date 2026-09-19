package com.familygraph.model.validation;

import java.util.List;
import java.util.Objects;

public record ValidationError(
        ValidationErrorCode code,
        String detail,
        List<String> cycle
) {
    public ValidationError {
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(detail, "detail must not be null");
        cycle = List.copyOf(Objects.requireNonNull(cycle, "cycle must not be null"));

        if (code == ValidationErrorCode.DIRECTED_CYCLE) {
            if (cycle.size() < 2 || !cycle.get(0).equals(cycle.get(cycle.size() - 1))) {
                throw new IllegalArgumentException("a directed cycle must be closed");
            }
        } else if (!cycle.isEmpty()) {
            throw new IllegalArgumentException("cycle is only allowed for DIRECTED_CYCLE");
        }
    }
}
