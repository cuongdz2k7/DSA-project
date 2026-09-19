package com.familygraph.model;

import com.familygraph.model.graph.FamilyGraph;
import com.familygraph.model.graph.Gender;
import com.familygraph.model.graph.ParentChildEdge;
import com.familygraph.model.graph.Person;
import com.familygraph.model.query.CheckRelationshipQuery;
import com.familygraph.model.query.Direction;
import com.familygraph.model.query.FamilyTreeQuery;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GraphAndQueryModelsTest {

    @Test
    void normalizesMissingNameToEmptyString() {
        Person person = new Person("P01", Gender.UNKNOWN, null);

        assertEquals("", person.name());
    }

    @Test
    void copiesGraphCollections() {
        List<Person> persons = new ArrayList<>();
        persons.add(new Person("P01", Gender.MALE, "Nam"));
        List<ParentChildEdge> edges = new ArrayList<>();

        FamilyGraph graph = new FamilyGraph(persons, edges);
        persons.add(new Person("P02", Gender.FEMALE, "Hoa"));
        edges.add(new ParentChildEdge("P01", "P02"));

        assertEquals(1, graph.persons().size());
        assertEquals(0, graph.edges().size());
        assertThrows(UnsupportedOperationException.class,
                () -> graph.persons().add(new Person("P03", Gender.UNKNOWN, "")));
    }

    @Test
    void rejectsInvalidQueries() {
        assertThrows(IllegalArgumentException.class,
                () -> new FamilyTreeQuery("P01", 0, Direction.ANCESTORS));
        assertThrows(IllegalArgumentException.class,
                () -> new CheckRelationshipQuery("P01", "P01"));
    }
}
