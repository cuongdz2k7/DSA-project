package com.familygraph.familytree;

import com.familygraph.model.familytree.*;
import com.familygraph.model.graph.Gender;
import com.familygraph.model.graph.Person;
import com.familygraph.model.graph.ParentChildEdge;
import com.familygraph.model.query.FamilyTreeQuery;
import com.familygraph.model.query.Direction;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LabelFamilyGraphTest {

    @Test
    public void testLabelFamilyGraphSuccess() {
        // 1. Tạo dữ liệu mẫu Person (id, gender, name)
        Person selfPerson = new Person("P1", Gender.MALE, "Nguyen Van A");
        Person fatherPerson = new Person("P2", Gender.MALE, "Nguyen Van B");
        Person sonPerson = new Person("P3", Gender.MALE, "Nguyen Van C");

        // 2. Tạo các FamilyNode kèm khoảng cách thế hệ
        FamilyNode selfNode = new FamilyNode(selfPerson, List.of(0));
        FamilyNode fatherNode = new FamilyNode(fatherPerson, List.of(1));
        FamilyNode sonNode = new FamilyNode(sonPerson, List.of(-1));

        List<FamilyNode> nodes = List.of(selfNode, fatherNode, sonNode);
        List<String> topoOrder = List.of("P2", "P1", "P3");
        List<ParentChildEdge> edges = List.of();

        // Dùng Direction enum chuẩn cho query
        FamilyTreeQuery query = new FamilyTreeQuery("P1", 2, Direction.BOTH);

        // Đóng gói vào OrderedFamilyGraph
        OrderedFamilyGraph orderedGraph = new OrderedFamilyGraph(
                query,
                topoOrder,
                nodes,
                edges
        );

        // 3. Gọi hàm cần test
        LabelFamilyGraph labeler = new LabelFamilyGraph();
        FamilyTreeResult result = labeler.labelFamilyGraph(orderedGraph);

        // 4. Kiểm tra kết quả (dùng .persons() thay vì .labeledPersons())
        assertNotNull(result, "Kết quả trả về không được null");
        assertEquals(3, result.persons().size(), "Phải có đúng 3 người được gắn nhãn");

        // Kiểm tra nhãn của từng người
        assertEquals(RelationType.SELF, result.persons().get(0).relations().get(0).type());
        assertEquals(RelationType.FATHER, result.persons().get(1).relations().get(0).type());
        assertEquals(RelationType.SON, result.persons().get(2).relations().get(0).type());
    }
}