package com.familygraph.model.validation;

import java.util.List;
public record ValidationError(
        ValidationErrorCode code,
        String detail,
        List<String> cycle
) {
    public ValidationError {
        cycle = List.copyOf(cycle);
    }
}
