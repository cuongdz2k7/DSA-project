package com.familygraph.familytree;

import com.familygraph.model.familytree.FamilyTreeResult;
import com.familygraph.model.familytree.RelationType;
import com.familygraph.model.graph.FamilyGraph;
import com.familygraph.model.graph.Gender;
import com.familygraph.model.graph.ParentChildEdge;
import com.familygraph.model.graph.Person;
import com.familygraph.model.query.Direction;
import com.familygraph.model.query.FamilyTreeQuery;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FamilyTreePipelineTest {
    @Test
    void createsLabeledFamilyTreeFromGraphAndQuery() {
        FamilyGraph graph = new FamilyGraph(
                List.of(
                        new Person("P01", Gender.MALE, "Nam"),
                        new Person("P02", Gender.FEMALE, "Hoa"),
                        new Person("P03", Gender.MALE, "Cường"),
                        new Person("P04", Gender.FEMALE, "An")
                ),
                List.of(
                        new ParentChildEdge("P01", "P03"),
                        new ParentChildEdge("P02", "P03"),
                        new ParentChildEdge("P03", "P04")
                )
        );

        FamilyTreeResult result = new FamilyTreePipeline().createFamilyTree(
                graph,
                new FamilyTreeQuery("P03", 2, Direction.BOTH)
        );

        assertEquals(List.of("P01", "P02", "P03", "P04"), result.topoOrder());
        assertEquals(result.topoOrder(), result.persons().stream().map(person -> person.person().id()).toList());
        assertEquals(RelationType.FATHER, result.persons().get(0).relations().get(0).type());
        assertEquals(RelationType.MOTHER, result.persons().get(1).relations().get(0).type());
        assertEquals(RelationType.SELF, result.persons().get(2).relations().get(0).type());
        assertEquals(RelationType.DAUGHTER, result.persons().get(3).relations().get(0).type());
        assertEquals(graph.edges(), result.edges());
    }
}
