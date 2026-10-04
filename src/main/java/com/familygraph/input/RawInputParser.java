package com.familygraph.input;

import com.familygraph.model.graph.FamilyGraph;
import com.familygraph.model.graph.Gender;
import com.familygraph.model.graph.ParentChildEdge;
import com.familygraph.model.graph.Person;
import com.familygraph.model.query.CheckRelationshipQuery;
import com.familygraph.model.query.Direction;
import com.familygraph.model.query.FamilyTreeQuery;
import com.familygraph.model.query.ProjectQuery;
import com.familygraph.model.validation.NormalizedInput;
import com.familygraph.model.validation.ValidationError;
import com.familygraph.model.validation.ValidationErrorCode;
import com.familygraph.model.validation.ValidationResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads, validates, and normalizes the project's raw text input. */
public final class RawInputParser {
    private static final Pattern PERSON_PATTERN = Pattern.compile(
            "^PERSON\\s+(\\S+)\\s+(\\S+)(?:\\s+(?:\"([^\"]*)\"|(\\S+)))?$"
    );

    /**
     * Parses the complete input and returns either normalized data or one primary error.
     */
    public ValidationResult parseAndValidate(String rawInput) {
        RawDocument document;
        try {
            document = parseDocument(rawInput);
        } catch (MalformedInputException exception) {
            return failure(ValidationErrorCode.MALFORMED_INPUT, exception.getMessage());
        }

        ValidationResult validationFailure = validateDocument(document);
        if (validationFailure != null) {
            return validationFailure;
        }

        return ValidationResult.success(normalize(document));
    }

    private RawDocument parseDocument(String rawInput) {
        if (rawInput == null) {
            throw malformed("rawInput is null");
        }

        List<String> lines = rawInput.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();
        if (lines.isEmpty()) {
            throw malformed("rawInput is empty");
        }

        int declaredPersonCount = parseCountLine(lines.get(0), "PERSON_COUNT", 1);
        int edgeCountIndex = findMarker(lines, "EDGE_COUNT", 1);
        if (edgeCountIndex < 0) {
            throw malformed("missing EDGE_COUNT");
        }

        List<PersonDraft> persons = new ArrayList<>();
        for (int index = 1; index < edgeCountIndex; index++) {
            persons.add(parsePersonLine(lines.get(index), index + 1));
        }

        int declaredEdgeCount = parseCountLine(
                lines.get(edgeCountIndex),
                "EDGE_COUNT",
                edgeCountIndex + 1
        );
        int queryIndex = findMarker(lines, "QUERY", edgeCountIndex + 1);
        if (queryIndex < 0) {
            throw malformed("missing QUERY");
        }
        if (queryIndex != lines.size() - 1) {
            throw malformed("unexpected content after QUERY");
        }

        List<EdgeDraft> edges = new ArrayList<>();
        for (int index = edgeCountIndex + 1; index < queryIndex; index++) {
            edges.add(parseEdgeLine(lines.get(index), index + 1));
        }

        QueryDraft query = parseQueryLine(lines.get(queryIndex), queryIndex + 1);
        return new RawDocument(declaredPersonCount, persons, declaredEdgeCount, edges, query);
    }

    private ValidationResult validateDocument(RawDocument document) {
        ValidationResult result;

        result = validateCounts(document);
        if (result != null) {
            return result;
        }

        result = validateDuplicateIds(document.persons());
        if (result != null) {
            return result;
        }

        result = validateGenders(document.persons());
        if (result != null) {
            return result;
        }

        Set<String> personIds = new HashSet<>();
        for (PersonDraft person : document.persons()) {
            personIds.add(person.id());
        }

        result = validateKnownIds(document, personIds);
        if (result != null) {
            return result;
        }

        result = validateDuplicateEdges(document.edges());
        if (result != null) {
            return result;
        }

        result = validateSelfParent(document.edges());
        if (result != null) {
            return result;
        }

        List<String> cycle = findDirectedCycle(document.persons(), document.edges());
        if (!cycle.isEmpty()) {
            return failure(
                    ValidationErrorCode.DIRECTED_CYCLE,
                    String.join(" -> ", cycle),
                    cycle
            );
        }

        result = validateQuery(document.query());
        if (result != null) {
            return result;
        }

        return null;
    }

    private ValidationResult validateCounts(RawDocument document) {
        if (document.declaredPersonCount() < 1) {
            return failure(
                    ValidationErrorCode.INVALID_COUNT,
                    "PERSON_COUNT " + document.declaredPersonCount()
            );
        }
        if (document.declaredPersonCount() != document.persons().size()) {
            return failure(
                    ValidationErrorCode.INVALID_COUNT,
                    "PERSON_COUNT expected " + document.declaredPersonCount()
                            + " but found " + document.persons().size()
            );
        }
        if (document.declaredEdgeCount() < 0) {
            return failure(
                    ValidationErrorCode.INVALID_COUNT,
                    "EDGE_COUNT " + document.declaredEdgeCount()
            );
        }
        if (document.declaredEdgeCount() != document.edges().size()) {
            return failure(
                    ValidationErrorCode.INVALID_COUNT,
                    "EDGE_COUNT expected " + document.declaredEdgeCount()
                            + " but found " + document.edges().size()
            );
        }
        return null;
    }

    private ValidationResult validateDuplicateIds(List<PersonDraft> persons) {
        Set<String> seenIds = new HashSet<>();
        for (PersonDraft person : persons) {
            if (!seenIds.add(person.id())) {
                return failure(ValidationErrorCode.DUPLICATE_ID, person.id());
            }
        }
        return null;
    }

    private ValidationResult validateGenders(List<PersonDraft> persons) {
        for (PersonDraft person : persons) {
            if (!isEnumValue(Gender.class, person.gender())) {
                return failure(ValidationErrorCode.INVALID_GENDER, person.gender());
            }
        }
        return null;
    }

    private ValidationResult validateKnownIds(RawDocument document, Set<String> personIds) {
        for (EdgeDraft edge : document.edges()) {
            if (!personIds.contains(edge.parentId())) {
                return failure(ValidationErrorCode.UNKNOWN_ID, edge.parentId());
            }
            if (!personIds.contains(edge.childId())) {
                return failure(ValidationErrorCode.UNKNOWN_ID, edge.childId());
            }
        }

        QueryDraft query = document.query();
        if (query.type().equals("FAMILY_TREE") && !personIds.contains(query.arguments().get(0))) {
            return failure(ValidationErrorCode.UNKNOWN_ID, query.arguments().get(0));
        }
        if (query.type().equals("CHECK_RELATIONSHIP")) {
            if (!personIds.contains(query.arguments().get(0))) {
                return failure(ValidationErrorCode.UNKNOWN_ID, query.arguments().get(0));
            }
            if (!personIds.contains(query.arguments().get(1))) {
                return failure(ValidationErrorCode.UNKNOWN_ID, query.arguments().get(1));
            }
        }
        return null;
    }

    private ValidationResult validateDuplicateEdges(List<EdgeDraft> edges) {
        Set<EdgeDraft> seenEdges = new HashSet<>();
        for (EdgeDraft edge : edges) {
            if (!seenEdges.add(edge)) {
                return failure(
                        ValidationErrorCode.DUPLICATE_EDGE,
                        edge.parentId() + " " + edge.childId()
                );
            }
        }
        return null;
    }

    private ValidationResult validateSelfParent(List<EdgeDraft> edges) {
        for (EdgeDraft edge : edges) {
            if (edge.parentId().equals(edge.childId())) {
                return failure(ValidationErrorCode.SELF_PARENT, edge.parentId());
            }
        }
        return null;
    }

    private ValidationResult validateQuery(QueryDraft query) {
        if (!query.type().equals("FAMILY_TREE") && !query.type().equals("CHECK_RELATIONSHIP")) {
            return failure(ValidationErrorCode.INVALID_QUERY_TYPE, query.type());
        }

        if (query.type().equals("FAMILY_TREE")) {
            String generationToken = query.arguments().get(1);
            Integer generations = parseIntegerOrNull(generationToken);
            if (generations == null || generations < 1) {
                return failure(ValidationErrorCode.INVALID_GENERATION, generationToken);
            }

            String directionToken = query.arguments().get(2);
            if (!isEnumValue(Direction.class, directionToken)) {
                return failure(ValidationErrorCode.INVALID_DIRECTION, directionToken);
            }
        } else if (query.arguments().get(0).equals(query.arguments().get(1))) {
            return failure(ValidationErrorCode.SAME_PERSON_QUERY, query.arguments().get(0));
        }

        return null;
    }

    private NormalizedInput normalize(RawDocument document) {
        List<Person> persons = document.persons().stream()
                .map(person -> new Person(
                        person.id(),
                        Gender.valueOf(person.gender()),
                        person.name()
                ))
                .toList();
        List<ParentChildEdge> edges = document.edges().stream()
                .map(edge -> new ParentChildEdge(edge.parentId(), edge.childId()))
                .toList();

        QueryDraft rawQuery = document.query();
        ProjectQuery query;
        if (rawQuery.type().equals("FAMILY_TREE")) {
            query = new FamilyTreeQuery(
                    rawQuery.arguments().get(0),
                    Integer.parseInt(rawQuery.arguments().get(1)),
                    Direction.valueOf(rawQuery.arguments().get(2))
            );
        } else {
            query = new CheckRelationshipQuery(
                    rawQuery.arguments().get(0),
                    rawQuery.arguments().get(1)
            );
        }

        return new NormalizedInput(new FamilyGraph(persons, edges), query);
    }

    private int parseCountLine(String line, String expectedKeyword, int lineNumber) {
        String[] tokens = splitTokens(line);
        if (tokens.length != 2 || !tokens[0].equals(expectedKeyword)) {
            throw malformed("line " + lineNumber + ": expected " + expectedKeyword);
        }
        Integer count = parseIntegerOrNull(tokens[1]);
        if (count == null) {
            throw malformed("line " + lineNumber + ": invalid count " + tokens[1]);
        }
        return count;
    }

    private PersonDraft parsePersonLine(String line, int lineNumber) {
        Matcher matcher = PERSON_PATTERN.matcher(line);
        if (!matcher.matches()) {
            throw malformed("line " + lineNumber + ": invalid PERSON record");
        }

        String unquotedName = matcher.group(4);
        if (unquotedName != null && unquotedName.contains("\"")) {
            throw malformed("line " + lineNumber + ": invalid PERSON name");
        }
        String name = matcher.group(3) != null
                ? matcher.group(3)
                : unquotedName == null ? "" : unquotedName;
        return new PersonDraft(matcher.group(1), matcher.group(2), name);
    }

    private EdgeDraft parseEdgeLine(String line, int lineNumber) {
        String[] tokens = splitTokens(line);
        if (tokens.length != 3 || !tokens[0].equals("EDGE")) {
            throw malformed("line " + lineNumber + ": invalid EDGE record");
        }
        return new EdgeDraft(tokens[1], tokens[2]);
    }

    private QueryDraft parseQueryLine(String line, int lineNumber) {
        String[] tokens = splitTokens(line);
        if (tokens.length < 2 || !tokens[0].equals("QUERY")) {
            throw malformed("line " + lineNumber + ": invalid QUERY record");
        }

        String type = tokens[1];
        if (type.equals("FAMILY_TREE") && tokens.length != 5) {
            throw malformed("line " + lineNumber + ": FAMILY_TREE requires 3 parameters");
        }
        if (type.equals("CHECK_RELATIONSHIP") && tokens.length != 4) {
            throw malformed("line " + lineNumber + ": CHECK_RELATIONSHIP requires 2 parameters");
        }

        return new QueryDraft(type, List.of(tokens).subList(2, tokens.length));
    }

    private int findMarker(List<String> lines, String keyword, int startIndex) {
        for (int index = startIndex; index < lines.size(); index++) {
            String[] tokens = splitTokens(lines.get(index));
            if (tokens.length > 0 && tokens[0].equals(keyword)) {
                return index;
            }
        }
        return -1;
    }

    private List<String> findDirectedCycle(List<PersonDraft> persons, List<EdgeDraft> edges) {
        Map<String, List<String>> childrenByParent = new LinkedHashMap<>();
        for (PersonDraft person : persons) {
            childrenByParent.put(person.id(), new ArrayList<>());
        }
        for (EdgeDraft edge : edges) {
            childrenByParent.get(edge.parentId()).add(edge.childId());
        }

        Map<String, VisitState> states = new HashMap<>();
        List<String> activePath = new ArrayList<>();
        Map<String, Integer> activeIndexes = new HashMap<>();
        for (PersonDraft person : persons) {
            if (states.getOrDefault(person.id(), VisitState.UNVISITED) == VisitState.UNVISITED) {
                List<String> cycle = depthFirstSearch(
                        person.id(),
                        childrenByParent,
                        states,
                        activePath,
                        activeIndexes
                );
                if (!cycle.isEmpty()) {
                    return cycle;
                }
            }
        }
        return List.of();
    }

    private List<String> depthFirstSearch(
            String personId,
            Map<String, List<String>> childrenByParent,
            Map<String, VisitState> states,
            List<String> activePath,
            Map<String, Integer> activeIndexes
    ) {
        states.put(personId, VisitState.VISITING);
        activeIndexes.put(personId, activePath.size());
        activePath.add(personId);

        for (String childId : childrenByParent.get(personId)) {
            VisitState childState = states.getOrDefault(childId, VisitState.UNVISITED);
            if (childState == VisitState.VISITING) {
                List<String> cycle = new ArrayList<>(
                        activePath.subList(activeIndexes.get(childId), activePath.size())
                );
                cycle.add(childId);
                return cycle;
            }
            if (childState == VisitState.UNVISITED) {
                List<String> cycle = depthFirstSearch(
                        childId,
                        childrenByParent,
                        states,
                        activePath,
                        activeIndexes
                );
                if (!cycle.isEmpty()) {
                    return cycle;
                }
            }
        }

        activePath.remove(activePath.size() - 1);
        activeIndexes.remove(personId);
        states.put(personId, VisitState.VISITED);
        return List.of();
    }

    private ValidationResult failure(ValidationErrorCode code, String detail) {
        return failure(code, detail, List.of());
    }

    private ValidationResult failure(
            ValidationErrorCode code,
            String detail,
            List<String> cycle
    ) {
        return ValidationResult.failure(new ValidationError(code, detail, cycle));
    }

    private String[] splitTokens(String line) {
        return line.trim().split("\\s+");
    }

    private Integer parseIntegerOrNull(String token) {
        try {
            return Integer.valueOf(token);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private <E extends Enum<E>> boolean isEnumValue(Class<E> enumClass, String token) {
        try {
            Enum.valueOf(enumClass, token);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private MalformedInputException malformed(String detail) {
        return new MalformedInputException(detail);
    }

    private record RawDocument(
            int declaredPersonCount,
            List<PersonDraft> persons,
            int declaredEdgeCount,
            List<EdgeDraft> edges,
            QueryDraft query
    ) {
    }

    private record PersonDraft(String id, String gender, String name) {
    }

    private record EdgeDraft(String parentId, String childId) {
    }

    private record QueryDraft(String type, List<String> arguments) {
    }

    private enum VisitState {
        UNVISITED,
        VISITING,
        VISITED
    }

    private static final class MalformedInputException extends RuntimeException {
        private MalformedInputException(String message) {
            super(message);
        }
    }
}
