package com.familygraph.familytree;

import com.familygraph.model.familytree.FamilyTreeResult;
import com.familygraph.model.familytree.OrderedFamilyGraph;
import com.familygraph.model.familytree.TopoResult;
import com.familygraph.model.graph.FamilyGraph;
import com.familygraph.model.query.FamilyTreeQuery;

/** Coordinates the complete in-memory family-tree flow. */
public class FamilyTreePipeline {
    private final TopologicalSorter topologicalSorter;
    private final FamilyTreeBuilder familyTreeBuilder;
    private final LabelFamilyGraph labelFamilyGraph;

    public FamilyTreePipeline() {
        this(new TopologicalSorter(), new FamilyTreeBuilder(), new LabelFamilyGraph());
    }

    public FamilyTreePipeline(
            TopologicalSorter topologicalSorter,
            FamilyTreeBuilder familyTreeBuilder,
            LabelFamilyGraph labelFamilyGraph
    ) {
        this.topologicalSorter = topologicalSorter;
        this.familyTreeBuilder = familyTreeBuilder;
        this.labelFamilyGraph = labelFamilyGraph;
    }

    public FamilyTreeResult createFamilyTree(FamilyGraph graph, FamilyTreeQuery query) {
        TopoResult topoResult = topologicalSorter.topologicalSort(graph);
        OrderedFamilyGraph orderedGraph = familyTreeBuilder.buildFamilyView(graph, query, topoResult);
        return labelFamilyGraph.labelFamilyGraph(orderedGraph);
    }
}
