package com.familygraph.input;

import com.familygraph.input.model.ParsedFamilyGraph;
import com.familygraph.input.model.ParsedParentChildEdge;
import com.familygraph.input.model.ParsedPerson;
import com.familygraph.model.validation.ValidationError;
import com.familygraph.model.validation.ValidationErrorCode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Applies the project's known-parent gender consistency rule. */
public final class ParentGenderValidator {
    public ValidationError validateParentGenders(ParsedFamilyGraph graph) {
        Map<String, String> genderById = new LinkedHashMap<>();
        for (ParsedPerson person : graph.persons()) {
            genderById.put(person.id(), person.genderToken());
        }

        Map<String, Map<String, String>> firstParentByChildAndGender = new LinkedHashMap<>();
        for (ParsedParentChildEdge edge : graph.edges()) {
            String gender = genderById.get(edge.parentId());
            if (gender.equals("UNKNOWN")) {
                continue;
            }
            Map<String, String> firstParentByGender = firstParentByChildAndGender
                    .computeIfAbsent(edge.childId(), ignored -> new LinkedHashMap<>());
            String firstParent = firstParentByGender.putIfAbsent(gender, edge.parentId());
            if (firstParent != null && !firstParent.equals(edge.parentId())) {
                return new ValidationError(
                        ValidationErrorCode.SAME_GENDER_PARENTS,
                        "child=" + edge.childId()
                                + " parents=" + firstParent + "," + edge.parentId()
                                + " gender=" + gender,
                        List.of()
                );
            }
        }
        return null;
    }
}
