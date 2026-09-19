# Hợp đồng Model và Result dùng chung

Tài liệu này là nguồn sự thật cho các Java object trao đổi giữa ba nhóm. Mọi thay đổi phải đi qua pull request riêng và được Tiến Cường duyệt.

## 1. Object đầu vào

### Đồ thị

```java
Person(String id, Gender gender, String name)
ParentChildEdge(String parentId, String childId)
FamilyGraph(List<Person> persons, List<ParentChildEdge> edges)
```

- `id` là ID duy nhất. `name` thiếu được chuẩn hóa thành `""`.
- `Gender` gồm `MALE`, `FEMALE`, `UNKNOWN`.
- Cạnh luôn có chiều `parentId -> childId`.
- Danh sách người và cạnh giữ thứ tự input và không thể sửa sau khi tạo.

### Truy vấn

```java
FamilyTreeQuery(String targetId, int numberOfGenerations, Direction direction)
CheckRelationshipQuery(String firstPersonId, String secondPersonId)
NormalizedInput(FamilyGraph graph, ProjectQuery query)
```

- `Direction` gồm `ANCESTORS`, `DESCENDANTS`, `BOTH`.
- `numberOfGenerations` tính cả target và phải từ 1 trở lên.
- `CheckRelationshipQuery.MAX_GENERATIONS` cố định bằng 3.
- Nhóm 1 và nhóm 2 chỉ nhận object đã chuẩn hóa, không đọc raw input.

## 2. Object kết quả kiểm tra input

```java
ValidationError(
    ValidationErrorCode code,
    String detail,
    List<String> cycle
)

ValidationResult(
    boolean valid,
    NormalizedInput data,
    ValidationError error
)
```

- Thành công dùng `ValidationResult.success(data)`.
- Thất bại dùng `ValidationResult.failure(error)`.
- `data` chỉ tồn tại khi `valid=true`; `error` chỉ tồn tại khi `valid=false`.
- `cycle` chỉ có dữ liệu với `DIRECTED_CYCLE` và phải là đường khép kín.

## 3. Object kết quả gia phả

### Kết quả topo

```java
TopoResult(List<String> topoOrder)
```

`topoOrder` chứa toàn bộ ID trong graph đúng một lần và cha/mẹ đứng trước con.

### Đồ thị con trước khi gắn nhãn

```java
FamilyNode(
    Person person,
    List<Integer> relationLevels
)

OrderedFamilyGraph(
    FamilyTreeQuery query,
    List<String> topoOrder,
    List<FamilyNode> nodes,
    List<ParentChildEdge> edges
)
```

`relationLevels` chứa tất cả độ lệch thế hệ có dấu so với target:

```text
0   target
1   cha/mẹ
2   ông/bà
3   cụ bậc 1
-1  con
-2  cháu
-3  chắt bậc 1
```

Danh sách này không trùng, có cùng hướng và được sắp theo trị tuyệt đối tăng dần. Không tồn tại một trường `level` duy nhất.

### Kết quả đã gắn nhãn

```java
RelationLabel(RelationType type, int greatDegree)
LabeledPerson(Person person, List<RelationLabel> relations)

FamilyTreeResult(
    FamilyTreeQuery query,
    List<String> topoOrder,
    List<LabeledPerson> persons,
    List<ParentChildEdge> edges
)
```

- `RelationLabel.code()` tạo mã như `FATHER` hoặc `GREAT_GRANDFATHER_2`.
- `RelationLabel.generationOffset()` trả lại độ lệch thế hệ có dấu cho web.
- `LabeledPerson` không chứa `level` hoặc `relationLevels`.
- Dữ liệu không đạt đủ số đời chỉ trả phần tìm được, không tạo warning.

## 4. Object kết quả kiểm tra ba đời

```java
RelationshipPath(
    String sourceId,
    String ancestorId,
    List<String> nodeIds
)

RelationshipResult(
    CheckRelationshipQuery query,
    List<String> commonAncestorIds,
    List<RelationshipPath> paths,
    List<Person> evidencePersons,
    List<ParentChildEdge> evidenceEdges
)
```

- `nodeIds` đi từ người nguồn lên tổ tiên, ngược chiều lưu của cạnh.
- `edgeCount()` bằng `nodeIds.size() - 1`.
- Mỗi tổ tiên chung có đúng một đường từ mỗi người trong query.
- `related()` được suy ra từ việc `commonAncestorIds` có rỗng hay không.
- `evidencePersons` và `evidenceEdges` chứa dữ liệu cần để web vẽ bằng chứng.
- Result không chứa warning hoặc trường độ sâu thực tế.

## 5. Hợp đồng hàm

```java
ValidationResult parseAndValidate(String rawInput)

TopoResult topologicalSort(FamilyGraph graph)

OrderedFamilyGraph buildFamilyView(
    FamilyGraph graph,
    FamilyTreeQuery query,
    TopoResult topoResult
)

FamilyTreeResult labelFamilyGraph(OrderedFamilyGraph orderedGraph)

RelationshipResult checkRelationship(
    FamilyGraph graph,
    CheckRelationshipQuery query
)
```

Luồng gia phả:

```text
NormalizedInput
  -> topologicalSort
  -> buildFamilyView
  -> labelFamilyGraph
  -> FamilyTreeResult
```

Luồng kiểm tra ba đời:

```text
NormalizedInput
  -> checkRelationship
  -> RelationshipResult
```

## 6. Quy tắc bất biến

- Không truyền `null` cho thuộc tính bắt buộc.
- ID không được rỗng.
- Mọi collection được sao chép bằng `List.copyOf`.
- Không module nào được sửa object do module trước trả về.
- Không dùng chuỗi tùy ý thay cho enum về giới tính, hướng, quan hệ hoặc mã lỗi.
- Câu thông báo tiếng Việt thuộc output layer, không nằm trong result thuật toán.
