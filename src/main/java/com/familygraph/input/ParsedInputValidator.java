package com.familygraph.input;

import com.familygraph.input.model.GraphValidationResult;
import com.familygraph.input.model.ParsedFamilyGraph;
import com.familygraph.input.model.ParsedInput;
import com.familygraph.input.model.ParsedParentChildEdge;
import com.familygraph.input.model.ParsedPerson;
import com.familygraph.input.model.ParsedQuery;
import com.familygraph.input.model.ValidatedInput;
import com.familygraph.model.graph.Gender;
import com.familygraph.model.query.Direction;
import com.familygraph.model.validation.ValidationError;
import com.familygraph.model.validation.ValidationErrorCode;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Validates parsed graph and query objects in the documented error-priority order. */
public final class ParsedInputValidator {
    private final DirectedCycleDetector cycleDetector;
    private final ParentGenderValidator parentGenderValidator;

    public ParsedInputValidator() {
        this(new DirectedCycleDetector(), new ParentGenderValidator());
    }

    public ParsedInputValidator(DirectedCycleDetector cycleDetector,
                                ParentGenderValidator parentGenderValidator) {
        this.cycleDetector = cycleDetector;
        this.parentGenderValidator = parentGenderValidator;
    }

    public GraphValidationResult validate(ParsedInput input) {
        ParsedFamilyGraph graph = input.graph();
        ValidationError error = validateCounts(graph);
        if (error == null) error = validateDuplicateIds(graph.persons());
        if (error == null) error = validateGenders(graph.persons());
        Set<String> personIds = collectPersonIds(graph.persons());
        if (error == null) error = validateKnownIds(input, personIds);
        if (error == null) error = validateDuplicateEdges(graph.edges());
        if (error == null) error = validateSelfParent(graph.edges());
        if (error == null) {
            List<String> cycle = findCycle(graph);
            if (!cycle.isEmpty()) {
                error = error(ValidationErrorCode.DIRECTED_CYCLE,
                        String.join(" -> ", cycle), cycle);
            }
        }
        if (error == null) error = validateParentGenders(graph);
        if (error == null) error = validateQuery(input.query());

        return error == null
                ? GraphValidationResult.success(new ValidatedInput(input))
                : GraphValidationResult.failure(error);
    }

    public List<String> findCycle(ParsedFamilyGraph graph) {
        return cycleDetector.findCycle(graph);
    }

    public ValidationError validateParentGenders(ParsedFamilyGraph graph) {
        return parentGenderValidator.validateParentGenders(graph);
    }

    private ValidationError validateCounts(ParsedFamilyGraph graph) {
        if (graph.declaredPersonCount() < 1) {
            return error(ValidationErrorCode.INVALID_COUNT,
                    "PERSON_COUNT " + graph.declaredPersonCount());
        }
        if (graph.declaredPersonCount() != graph.persons().size()) {
            return error(ValidationErrorCode.INVALID_COUNT,
                    "PERSON_COUNT expected " + graph.declaredPersonCount()
                            + " but found " + graph.persons().size());
        }
        if (graph.declaredEdgeCount() < 0) {
            return error(ValidationErrorCode.INVALID_COUNT,
                    "EDGE_COUNT " + graph.declaredEdgeCount());
        }
        if (graph.declaredEdgeCount() != graph.edges().size()) {
            return error(ValidationErrorCode.INVALID_COUNT,
                    "EDGE_COUNT expected " + graph.declaredEdgeCount()
                            + " but found " + graph.edges().size());
        }
        return null;
    }

    private ValidationError validateDuplicateIds(List<ParsedPerson> persons) {
        Set<String> seen = new HashSet<>();
        for (ParsedPerson person : persons) {
            if (!seen.add(person.id())) {
                return error(ValidationErrorCode.DUPLICATE_ID, person.id());
            }
        }
        return null;
    }

    private ValidationError validateGenders(List<ParsedPerson> persons) {
        for (ParsedPerson person : persons) {
            if (!isEnumValue(Gender.class, person.genderToken())) {
                return error(ValidationErrorCode.INVALID_GENDER, person.genderToken());
            }
        }
        return null;
    }

    private Set<String> collectPersonIds(List<ParsedPerson> persons) {
        Set<String> ids = new HashSet<>();
        for (ParsedPerson person : persons) ids.add(person.id());
        return ids;
    }

    private ValidationError validateKnownIds(ParsedInput input, Set<String> personIds) {
        for (ParsedParentChildEdge edge : input.graph().edges()) {
            if (!personIds.contains(edge.parentId())) {
                return error(ValidationErrorCode.UNKNOWN_ID, edge.parentId());
            }
            if (!personIds.contains(edge.childId())) {
                return error(ValidationErrorCode.UNKNOWN_ID, edge.childId());
            }
        }
        ParsedQuery query = input.query();
        if (query.type().equals("FAMILY_TREE") && !personIds.contains(query.arguments().get(0))) {
            return error(ValidationErrorCode.UNKNOWN_ID, query.arguments().get(0));
        }
        if (query.type().equals("CHECK_RELATIONSHIP")) {
            if (!personIds.contains(query.arguments().get(0))) {
                return error(ValidationErrorCode.UNKNOWN_ID, query.arguments().get(0));
            }
            if (!personIds.contains(query.arguments().get(1))) {
                return error(ValidationErrorCode.UNKNOWN_ID, query.arguments().get(1));
            }
        }
        return null;
    }

    private ValidationError validateDuplicateEdges(List<ParsedParentChildEdge> edges) {
        Set<ParsedParentChildEdge> seen = new HashSet<>();
        for (ParsedParentChildEdge edge : edges) {
            if (!seen.add(edge)) {
                return error(ValidationErrorCode.DUPLICATE_EDGE,
                        edge.parentId() + " " + edge.childId());
            }
        }
        return null;
    }

    private ValidationError validateSelfParent(List<ParsedParentChildEdge> edges) {
        for (ParsedParentChildEdge edge : edges) {
            if (edge.parentId().equals(edge.childId())) {
                return error(ValidationErrorCode.SELF_PARENT, edge.parentId());
            }
        }
        return null;
    }

    private ValidationError validateQuery(ParsedQuery query) {
        if (!query.type().equals("FAMILY_TREE") && !query.type().equals("CHECK_RELATIONSHIP")) {
            return error(ValidationErrorCode.INVALID_QUERY_TYPE, query.type());
        }
        if (query.type().equals("FAMILY_TREE")) {
            String generationToken = query.arguments().get(1);
            Integer generations = parseIntegerOrNull(generationToken);
            if (generations == null || generations < 1) {
                return error(ValidationErrorCode.INVALID_GENERATION, generationToken);
            }
            String directionToken = query.arguments().get(2);
            if (!isEnumValue(Direction.class, directionToken)) {
                return error(ValidationErrorCode.INVALID_DIRECTION, directionToken);
            }
        } else if (query.arguments().get(0).equals(query.arguments().get(1))) {
            return error(ValidationErrorCode.SAME_PERSON_QUERY, query.arguments().get(0));
        }
        return null;
    }

    private ValidationError error(ValidationErrorCode code, String detail) {
        return error(code, detail, List.of());
    }

    private ValidationError error(ValidationErrorCode code, String detail, List<String> cycle) {
        return new ValidationError(code, detail, cycle);
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
}
