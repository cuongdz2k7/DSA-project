package com.familygraph.input;

import com.familygraph.input.model.ParsedInput;
import com.familygraph.input.model.ParsedQuery;
import com.familygraph.input.model.ValidatedInput;
import com.familygraph.model.graph.FamilyGraph;
import com.familygraph.model.graph.Gender;
import com.familygraph.model.graph.ParentChildEdge;
import com.familygraph.model.graph.Person;
import com.familygraph.model.query.CheckRelationshipQuery;
import com.familygraph.model.query.Direction;
import com.familygraph.model.query.FamilyTreeQuery;
import com.familygraph.model.query.ProjectQuery;
import com.familygraph.model.validation.NormalizedInput;

import java.util.List;

/** Converts validated syntax objects into the project's official domain model. */
public final class InputNormalizer {
    public NormalizedInput normalize(ValidatedInput input) {
        ParsedInput parsed = input.parsedInput();
        List<Person> persons = parsed.graph().persons().stream()
                .map(person -> new Person(person.id(),
                        Gender.valueOf(person.genderToken()), person.name()))
                .toList();
        List<ParentChildEdge> edges = parsed.graph().edges().stream()
                .map(edge -> new ParentChildEdge(edge.parentId(), edge.childId()))
                .toList();

        ParsedQuery parsedQuery = parsed.query();
        ProjectQuery query = parsedQuery.type().equals("FAMILY_TREE")
                ? new FamilyTreeQuery(parsedQuery.arguments().get(0),
                        Integer.parseInt(parsedQuery.arguments().get(1)),
                        Direction.valueOf(parsedQuery.arguments().get(2)))
                : new CheckRelationshipQuery(parsedQuery.arguments().get(0),
                        parsedQuery.arguments().get(1));
        return new NormalizedInput(new FamilyGraph(persons, edges), query);
    }
}
