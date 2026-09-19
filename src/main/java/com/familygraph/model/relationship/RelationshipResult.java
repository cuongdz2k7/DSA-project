package com.familygraph.model.relationship;

import com.familygraph.model.graph.ParentChildEdge;
import com.familygraph.model.graph.Person;
import com.familygraph.model.query.CheckRelationshipQuery;
import com.familygraph.model.result.ProjectResult;

import java.util.List;

public record RelationshipResult(
        CheckRelationshipQuery query,
        List<String> commonAncestorIds,
        List<RelationshipPath> paths,
        List<Person> evidencePersons,
        List<ParentChildEdge> evidenceEdges
) implements ProjectResult {
    public RelationshipResult {
        commonAncestorIds = List.copyOf(commonAncestorIds);
        paths = List.copyOf(paths);
        evidencePersons = List.copyOf(evidencePersons);
        evidenceEdges = List.copyOf(evidenceEdges);
    }

    public boolean related() {
        return !commonAncestorIds.isEmpty();
    }

}
