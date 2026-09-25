package com.familygraph.familytree;

import com.familygraph.model.familytree.*;
import com.familygraph.model.graph.Gender;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class LabelFamilyGraph {
    public FamilyTreeResult labelFamilyGraph(OrderedFamilyGraph orderedFamilyGraph) {
        List<LabeledPerson> labledPersonList = new ArrayList<>();

        for (FamilyNode node : orderedFamilyGraph.nodes()) {
            List<RelationLabel> relations = new ArrayList<>();

            //duyet tung khoang cach the he
            for(int level : node.relationLevels()) {
                relations.add(mapLevelToRelationLabel(level, node.person().gender()));
            }
            // sx nhan
            relations.sort(Comparator.comparingInt((RelationLabel r) -> Math.abs(r.generationOffset()))
                    .thenComparing(RelationLabel::code));

            labledPersonList.add(new LabeledPerson(node.person(), relations));
        }

        return new FamilyTreeResult(
                orderedFamilyGraph.query(),
                orderedFamilyGraph.topoOrder(),
                labledPersonList,
                orderedFamilyGraph.edges()
        );
    }

    private RelationLabel mapLevelToRelationLabel(int level, Gender gender) {
        RelationType type;
        int greatDegree = 0;

        if (level == 0) {
            type = RelationType.SELF;
        } else if (level == 1) {
            type = switch (gender) {
                case MALE -> RelationType.FATHER;
                case FEMALE -> RelationType.MOTHER;
                default -> RelationType.PARENT;
            };
        } else if (level == 2) {
            type = switch (gender) {
                case MALE -> RelationType.GRANDFATHER;
                case FEMALE -> RelationType.GRANDMOTHER;
                default -> RelationType.GRANDPARENT;
            };
        } else if (level >= 3) {
            greatDegree = level - 2;
            type = switch (gender) {
                case MALE -> RelationType.GREAT_GRANDFATHER;
                case FEMALE -> RelationType.GREAT_GRANDMOTHER;
                default -> RelationType.GREAT_GRANDPARENT;
            };
        } else if (level == -1) {
            type = switch (gender) {
                case MALE -> RelationType.SON;
                case FEMALE -> RelationType.DAUGHTER;
                default -> RelationType.CHILD;
            };
        } else if (level == -2) {
            type = switch (gender) {
                case MALE -> RelationType.GRANDSON;
                case FEMALE -> RelationType.GRANDDAUGHTER;
                default -> RelationType.GRANDCHILD;
            };
        } else {
            greatDegree = Math.abs(level) - 2;
            type = switch (gender) {
                case MALE -> RelationType.GREAT_GRANDSON;
                case FEMALE -> RelationType.GREAT_GRANDDAUGHTER;
                default -> RelationType.GREAT_GRANDCHILD;
            };
        }

        return new RelationLabel(type, greatDegree);
    }
}
