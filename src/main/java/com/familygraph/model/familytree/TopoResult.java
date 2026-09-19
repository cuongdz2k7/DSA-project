package com.familygraph.model.familytree;

import java.util.List;

public record TopoResult(
        List<String> topoOrder
) {
    public TopoResult {
        topoOrder = List.copyOf(topoOrder);
    }
}
