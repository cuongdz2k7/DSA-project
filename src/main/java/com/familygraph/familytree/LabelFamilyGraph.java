package com.familygraph.familytree;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.familygraph.model.familytree.FamilyNode;
import com.familygraph.model.familytree.FamilyTreeResult;
import com.familygraph.model.familytree.LabeledPerson;
import com.familygraph.model.familytree.OrderedFamilyGraph;
import com.familygraph.model.familytree.RelationLabel;
import com.familygraph.model.familytree.RelationType;
import com.familygraph.model.graph.Gender;

public class LabelFamilyGraph {
    public FamilyTreeResult labelFamilyGraph(OrderedFamilyGraph orderedFamilyGraph) { 
        List<LabeledPerson> labeledPersonList = new ArrayList<>();

        for (FamilyNode node : orderedFamilyGraph.nodes()) {
            List<RelationLabel> relations = new ArrayList<>();

            //duyet tung khoang cach the he
            for(int level : node.relationLevels()) {
                relations.add(mapLevelToRelationLabel(level, node.person().gender()));
            }
            // sx nhan
            relations.sort(Comparator.comparingInt((RelationLabel r) -> Math.abs(r.generationOffset()))
                    .thenComparing(RelationLabel::code));

            labeledPersonList.add(new LabeledPerson(node.person(), relations));
        }

        return new FamilyTreeResult(
                orderedFamilyGraph.query(),
                orderedFamilyGraph.topoOrder(),
                labeledPersonList,
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
