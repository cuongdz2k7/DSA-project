package com.familygraph.input.model;

import java.util.List;

/** Query tokens read from text before domain validation. */
public record ParsedQuery(
        String type,
        List<String> arguments
) {
    public ParsedQuery {
        arguments = List.copyOf(arguments);
    }
}
