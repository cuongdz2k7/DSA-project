package com.familygraph.input.model;

/** Syntactically parsed input that has not yet passed domain validation. */
public record ParsedInput(
        ParsedFamilyGraph graph,
        ParsedQuery query
) {
}
