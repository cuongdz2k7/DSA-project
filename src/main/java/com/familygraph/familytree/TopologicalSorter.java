package com.familygraph.familytree;

import com.familygraph.model.familytree.TopoResult;
import com.familygraph.model.graph.FamilyGraph;
import com.familygraph.model.graph.ParentChildEdge;
import com.familygraph.model.graph.Person;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Sắp xếp toàn bộ đồ thị gia phả theo thứ tự cha/mẹ đứng trước con.
 *
 * <p>Thứ tự giữa những người cùng bậc vào được giữ theo thứ tự xuất hiện
 * trong {@link FamilyGraph#persons()}.</p>
 */
public class TopologicalSorter {
    /**
     * Thực hiện sắp xếp topo theo thuật toán Kahn.
     *
     * @param graph đồ thị gia phả đã được nhóm kiểm tra input xác nhận hợp lệ
     * @return danh sách ID theo thứ tự topo; đồ thị null trả về danh sách rỗng
     */
    public TopoResult topologicalSort(FamilyGraph graph) {
        if (graph == null || graph.persons() == null) {
            return new TopoResult(Collections.emptyList());
        }

        // LinkedHashMap giữ thứ tự person trong input khi có nhiều đỉnh bậc vào bằng 0.
        Map<String, Integer> inDegree = new LinkedHashMap<>();
        Map<String, List<String>> adjacencyList = new HashMap<>();

        for (Person person : graph.persons()) {
            inDegree.put(person.id(), 0);
            adjacencyList.put(person.id(), new ArrayList<>());
        }

        if (graph.edges() != null) {
            for (ParentChildEdge edge : graph.edges()) {
                String parentId = edge.parentId();
                String childId = edge.childId();

                // Input hợp lệ luôn có hai ID này; kiểm tra giúp hàm an toàn khi dùng độc lập.
                if (adjacencyList.containsKey(parentId) && inDegree.containsKey(childId)) {
                    adjacencyList.get(parentId).add(childId);
                    inDegree.put(childId, inDegree.get(childId) + 1);
                }
            }
        }

        Queue<String> queue = new ArrayDeque<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.offer(entry.getKey());
            }
        }

        List<String> topoOrder = new ArrayList<>();
        while (!queue.isEmpty()) {
            String currentId = queue.poll();
            topoOrder.add(currentId);

            for (String childId : adjacencyList.get(currentId)) {
                int updatedInDegree = inDegree.get(childId) - 1;
                inDegree.put(childId, updatedInDegree);
                if (updatedInDegree == 0) {
                    queue.offer(childId);
                }
            }
        }

        return new TopoResult(topoOrder);
    }
}
