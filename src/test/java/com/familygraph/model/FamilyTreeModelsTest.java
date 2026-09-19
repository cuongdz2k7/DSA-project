package com.familygraph.model;

import com.familygraph.model.familytree.FamilyNode;
import com.familygraph.model.familytree.FamilyTreeResult;
import com.familygraph.model.familytree.LabeledPerson;
import com.familygraph.model.familytree.OrderedFamilyGraph;
import com.familygraph.model.familytree.RelationLabel;
import com.familygraph.model.familytree.RelationType;
import com.familygraph.model.graph.Gender;
import com.familygraph.model.graph.ParentChildEdge;
import com.familygraph.model.graph.Person;
import com.familygraph.model.query.Direction;
import com.familygraph.model.query.FamilyTreeQuery;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FamilyTreeModelsTest {

    @Test
    void keepsAllRelationLevelsWithoutChoosingOneLevel() {
        List<Integer> levels = new ArrayList<>(List.of(1, 3));
        FamilyNode node = new FamilyNode(
                new Person("P10", Gender.MALE, "Nam"),
                levels
        );
        levels.add(5);

        assertEquals(List.of(1, 3), node.relationLevels());
        assertThrows(UnsupportedOperationException.class, () -> node.relationLevels().add(5));
    }

    @Test
    void rejectsInvalidRelationLevelCollections() {
        Person person = new Person("P10", Gender.MALE, "Nam");

        assertThrows(IllegalArgumentException.class, () -> new FamilyNode(person, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new FamilyNode(person, List.of(1, 1)));
        assertThrows(IllegalArgumentException.class, () -> new FamilyNode(person, List.of(3, 1)));
        assertThrows(IllegalArgumentException.class, () -> new FamilyNode(person, List.of(1, -2)));
        assertThrows(IllegalArgumentException.class, () -> new FamilyNode(person, List.of(0, 1)));
    }

    @Test
    void createsStableRelationCodesAndOffsets() {
        RelationLabel father = new RelationLabel(RelationType.FATHER, 0);
        RelationLabel greatGrandfather = new RelationLabel(RelationType.GREAT_GRANDFATHER, 2);
        RelationLabel greatGranddaughter = new RelationLabel(RelationType.GREAT_GRANDDAUGHTER, 1);

        assertEquals("FATHER", father.code());
        assertEquals(1, father.generationOffset());
        assertEquals("GREAT_GRANDFATHER_2", greatGrandfather.code());
        assertEquals(4, greatGrandfather.generationOffset());
        assertEquals(-3, greatGranddaughter.generationOffset());
        assertThrows(IllegalArgumentException.class,
                () -> new RelationLabel(RelationType.GREAT_GRANDFATHER, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new RelationLabel(RelationType.FATHER, 1));
    }

    @Test
    void buildsOrderedAndLabeledFamilyResults() {
        Person father = new Person("P01", Gender.MALE, "Nam");
        Person target = new Person("P02", Gender.FEMALE, "Lan");
        ParentChildEdge edge = new ParentChildEdge("P01", "P02");
        FamilyTreeQuery query = new FamilyTreeQuery("P02", 2, Direction.ANCESTORS);

        OrderedFamilyGraph orderedGraph = new OrderedFamilyGraph(
                query,
                List.of("P01", "P02"),
                List.of(
                        new FamilyNode(father, List.of(1)),
                        new FamilyNode(target, List.of(0))
                ),
                List.of(edge)
        );

        FamilyTreeResult result = new FamilyTreeResult(
                query,
                orderedGraph.topoOrder(),
                List.of(
                        new LabeledPerson(father, List.of(new RelationLabel(RelationType.FATHER, 0))),
                        new LabeledPerson(target, List.of(new RelationLabel(RelationType.SELF, 0)))
                ),
                orderedGraph.edges()
        );

        assertEquals(List.of("P01", "P02"), result.topoOrder());
        assertEquals(2, result.persons().size());
        assertEquals("FATHER", result.persons().get(0).relations().get(0).code());
    }

    @Test
    void requiresNodesAndPeopleToFollowTopologicalOrder() {
        Person parent = new Person("P01", Gender.MALE, "Nam");
        Person child = new Person("P02", Gender.FEMALE, "Lan");
        ParentChildEdge edge = new ParentChildEdge("P01", "P02");
        FamilyTreeQuery query = new FamilyTreeQuery("P02", 2, Direction.ANCESTORS);

        assertThrows(IllegalArgumentException.class, () -> new OrderedFamilyGraph(
                query,
                List.of("P01", "P02"),
                List.of(
                        new FamilyNode(child, List.of(0)),
                        new FamilyNode(parent, List.of(1))
                ),
                List.of(edge)
        ));
    }
}
