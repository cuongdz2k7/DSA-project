# Hướng dẫn Model và Hợp đồng Hàm

Tài liệu này mô tả các Java object dùng chung giữa ba nhóm: object nào là **đầu vào**, object nào là **đầu ra**, và mỗi hàm phải nhận/trả dữ liệu gì. Đây là hợp đồng tích hợp; tên class, package, field và kiểu dữ liệu không được tự ý đổi nếu chưa có pull request hợp đồng.

## 1. Nguyên tắc sử dụng chung

- Nhóm 3 là nhóm duy nhất đọc `String rawInput` và quyết định input có hợp lệ hay không.
- Nhóm 1 và nhóm 2 chỉ làm việc trên `NormalizedInput` hoặc các object được tạo từ dữ liệu đã hợp lệ.
- Các `record` là object truyền dữ liệu, không tự chạy lại kiểm tra nghiệp vụ. Collection trong record được sao chép bằng `List.copyOf` để không ai sửa được kết quả của module khác.
- Lỗi input phải đi qua `ValidationResult` và `ValidationError`; không ném lỗi để thay cho output nghiệp vụ.
- Thiếu cha, mẹ hoặc thiếu số đời không phải lỗi. Thuật toán chỉ trả những người và quan hệ thực sự có trong dữ liệu.

## 2. Nhóm object hàm nhận vào

### 2.1. Đồ thị gốc — package `com.familygraph.model.graph`

#### `Gender`

```java
public enum Gender {
    MALE,
    FEMALE,
    UNKNOWN
}
```

| Giá trị | Ý nghĩa |
|---|---|
| `MALE` | Nam; dùng để gắn nhãn như `FATHER`, `SON`, `GRANDFATHER`. |
| `FEMALE` | Nữ; dùng để gắn nhãn như `MOTHER`, `DAUGHTER`, `GRANDMOTHER`. |
| `UNKNOWN` | Chưa xác định; dùng nhãn trung tính như `PARENT`, `CHILD`, `GRANDPARENT`. |

#### `Person`

```java
public record Person(
    String id,
    Gender gender,
    String name
) {}
```

| Field | Kiểu | Ý nghĩa |
|---|---|---|
| `id` | `String` | Mã định danh duy nhất của một người, ví dụ `P03`. |
| `gender` | `Gender` | Giới tính để thuật toán labeling chọn nhãn phù hợp. |
| `name` | `String` | Tên hiển thị trên web; nhóm 3 chuẩn hóa tên thiếu thành chuỗi rỗng `""`. |

#### `ParentChildEdge`

```java
public record ParentChildEdge(
    String parentId,
    String childId
) {}
```

| Field | Kiểu | Ý nghĩa |
|---|---|---|
| `parentId` | `String` | ID cha hoặc mẹ. |
| `childId` | `String` | ID người con. |

Chiều cạnh luôn là:

```text
parentId -> childId
```

Ví dụ `new ParentChildEdge("P01", "P03")` nghĩa là `P01` là cha hoặc mẹ của `P03`.

#### `FamilyGraph`

```java
public record FamilyGraph(
    List<Person> persons,
    List<ParentChildEdge> edges
) {}
```

| Field | Kiểu | Ý nghĩa |
|---|---|---|
| `persons` | `List<Person>` | Toàn bộ người trong input. Thứ tự được giữ theo input để kết quả ổn định khi cần. |
| `edges` | `List<ParentChildEdge>` | Toàn bộ quan hệ cha/mẹ → con trong input. |

`FamilyGraph` là đồ thị đầy đủ, chưa lọc theo người cần tìm hoặc số đời.

### 2.2. Truy vấn — package `com.familygraph.model.query`

#### `Direction`

```java
public enum Direction {
    ANCESTORS,
    DESCENDANTS,
    BOTH
}
```

| Giá trị | Ý nghĩa |
|---|---|
| `ANCESTORS` | Chỉ lấy tổ tiên của target. |
| `DESCENDANTS` | Chỉ lấy hậu duệ của target. |
| `BOTH` | Lấy cả tổ tiên và hậu duệ. |

#### `ProjectQuery`

```java
public interface ProjectQuery {}
```

Đây là interface đánh dấu. `NormalizedInput.query()` có thể là một trong hai loại query bên dưới.

#### `FamilyTreeQuery`

```java
public record FamilyTreeQuery(
    String targetId,
    int numberOfGenerations,
    Direction direction
) implements ProjectQuery {}
```

| Field | Kiểu | Ý nghĩa |
|---|---|---|
| `targetId` | `String` | ID người trung tâm của truy vấn gia phả. |
| `numberOfGenerations` | `int` | Số đời cần lấy, **tính cả target**. Ví dụ `3` là target, cha/mẹ, ông/bà theo hướng tổ tiên. |
| `direction` | `Direction` | Phạm vi lấy `ANCESTORS`, `DESCENDANTS` hoặc `BOTH`. |

#### `CheckRelationshipQuery`

```java
public record CheckRelationshipQuery(
    String firstPersonId,
    String secondPersonId
) implements ProjectQuery {
    public static final int MAX_GENERATIONS = 3;
}
```

| Field / hằng số | Kiểu | Ý nghĩa |
|---|---|---|
| `firstPersonId` | `String` | Người thứ nhất cần kiểm tra. |
| `secondPersonId` | `String` | Người thứ hai cần kiểm tra. |
| `MAX_GENERATIONS` | `int` | Hằng số phạm vi kiểm tra: 3 đời. |

### 2.3. Input đã chuẩn hóa — package `com.familygraph.model.validation`

#### `NormalizedInput`

```java
public record NormalizedInput(
    FamilyGraph graph,
    ProjectQuery query
) {}
```

| Field | Kiểu | Ý nghĩa |
|---|---|---|
| `graph` | `FamilyGraph` | Đồ thị đã được nhóm 3 kiểm tra và chuẩn hóa. |
| `query` | `ProjectQuery` | Một `FamilyTreeQuery` hoặc `CheckRelationshipQuery` đã hợp lệ. |

Nhóm 1 và 2 không nhận raw input; các bạn nhận `NormalizedInput`, sau đó ép kiểu `query` đúng với luồng đang xử lý.

## 3. Nhóm object hàm trả ra

### 3.1. Kết quả kiểm tra input — package `com.familygraph.model.validation`

#### `ValidationErrorCode`

```java
public enum ValidationErrorCode {
    MALFORMED_INPUT,
    INVALID_COUNT,
    DUPLICATE_ID,
    INVALID_GENDER,
    UNKNOWN_ID,
    DUPLICATE_EDGE,
    SELF_PARENT,
    DIRECTED_CYCLE,
    INVALID_QUERY_TYPE,
    INVALID_GENERATION,
    INVALID_DIRECTION,
    SAME_PERSON_QUERY
}
```

Đây là mã lỗi ổn định cho output layer.

| Mã | Ý nghĩa |
|---|---|
| `MALFORMED_INPUT` | Không đọc được cấu trúc text theo format đã quy ước. |
| `INVALID_COUNT` | Số bản ghi thực tế không khớp `PERSON_COUNT` hoặc `EDGE_COUNT`. |
| `DUPLICATE_ID` | Một ID người xuất hiện nhiều hơn một lần. |
| `INVALID_GENDER` | Giới tính không phải `MALE`, `FEMALE` hoặc `UNKNOWN`. |
| `UNKNOWN_ID` | ID trong cạnh hoặc query không có trong danh sách người. |
| `DUPLICATE_EDGE` | Một cạnh cha/mẹ → con xuất hiện lặp lại. |
| `SELF_PARENT` | Một người là cha/mẹ trực tiếp của chính mình. |
| `DIRECTED_CYCLE` | Đồ thị huyết thống tạo chu trình có hướng. |
| `INVALID_QUERY_TYPE` | Lệnh query không phải `FAMILY_TREE` hoặc `CHECK_RELATIONSHIP`. |
| `INVALID_GENERATION` | Số đời không hợp lệ. |
| `INVALID_DIRECTION` | Hướng tìm không phải `ANCESTORS`, `DESCENDANTS` hoặc `BOTH`. |
| `SAME_PERSON_QUERY` | Hai ID trong query kiểm tra quan hệ là cùng một người. |

#### `ValidationError`

```java
public record ValidationError(
    ValidationErrorCode code,
    String detail,
    List<String> cycle
) {}
```

| Field | Kiểu | Ý nghĩa |
|---|---|---|
| `code` | `ValidationErrorCode` | Loại lỗi. |
| `detail` | `String` | Chi tiết ngắn, thường là ID, token hoặc giá trị gây lỗi. |
| `cycle` | `List<String>` | Chuỗi ID của chu trình, ví dụ `[P01, P02, P03, P01]`; rỗng nếu lỗi không phải chu trình. |

#### `ValidationResult`

```java
public record ValidationResult(
    boolean valid,
    NormalizedInput data,
    ValidationError error
) {
    public static ValidationResult success(NormalizedInput data);
    public static ValidationResult failure(ValidationError error);
}
```

| Field | Kiểu | Ý nghĩa |
|---|---|---|
| `valid` | `boolean` | `true` nếu input hợp lệ. |
| `data` | `NormalizedInput` | Chỉ có khi `valid == true`; là dữ liệu chuyển cho nhóm 1 hoặc 2. |
| `error` | `ValidationError` | Chỉ có khi `valid == false`; dùng để in output lỗi. |

### 3.2. Kết quả topo — package `com.familygraph.model.familytree`

#### `TopoResult`

```java
public record TopoResult(
    List<String> topoOrder
) {}
```

| Field | Kiểu | Ý nghĩa |
|---|---|---|
| `topoOrder` | `List<String>` | ID của **toàn bộ** người trong graph theo thứ tự topo: cha/mẹ đứng trước con. |

`TopoResult` chưa liên quan đến target, số đời hoặc nhãn quan hệ.

### 3.3. Đồ thị con gia phả trước labeling — package `com.familygraph.model.familytree`

#### `FamilyNode`

```java
public record FamilyNode(
    Person person,
    List<Integer> relationLevels
) {}
```

| Field | Kiểu | Ý nghĩa |
|---|---|---|
| `person` | `Person` | Người thuộc phần gia phả đã lọc. |
| `relationLevels` | `List<Integer>` | Toàn bộ khoảng cách thế hệ có dấu của người này so với target. |

Quy ước `relationLevels`:

| Giá trị | Ý nghĩa |
|---:|---|
| `0` | Chính target. |
| `1`, `2`, `3`, ... | Cha/mẹ, ông/bà, cụ bậc 1, ... |
| `-1`, `-2`, `-3`, ... | Con, cháu, chắt bậc 1, ... |

Ví dụ `List.of(1, 3)` nghĩa là người đó vừa có quan hệ cha/mẹ, vừa có quan hệ cụ bậc 1 với target qua hai đường khác nhau. `buildFamilyView` chịu trách nhiệm tạo list đúng quy ước; `FamilyNode` chỉ lưu list đó.

#### `OrderedFamilyGraph`

```java
public record OrderedFamilyGraph(
    FamilyTreeQuery query,
    List<String> topoOrder,
    List<FamilyNode> nodes,
    List<ParentChildEdge> edges
) {}
```

| Field | Kiểu | Ý nghĩa |
|---|---|---|
| `query` | `FamilyTreeQuery` | Truy vấn gốc để Kim Chi biết target, số đời và hướng. |
| `topoOrder` | `List<String>` | Thứ tự topo sau khi đã lọc xuống phần gia phả cần trả. |
| `nodes` | `List<FamilyNode>` | Người thuộc phần gia phả cùng tất cả `relationLevels`. |
| `edges` | `List<ParentChildEdge>` | Chỉ các cạnh nằm trong đồ thị con được trả về. |

### 3.4. Kết quả gắn nhãn — package `com.familygraph.model.familytree`

#### `RelationType`

`RelationType` là enum để Kim Chi chọn đúng loại nhãn. Không tự tạo chuỗi nhãn ngoài enum này.

| Nhóm | Giá trị |
|---|---|
| Bản thân | `SELF` |
| Cha/mẹ | `FATHER`, `MOTHER`, `PARENT` |
| Ông/bà | `GRANDFATHER`, `GRANDMOTHER`, `GRANDPARENT` |
| Con | `SON`, `DAUGHTER`, `CHILD` |
| Cháu | `GRANDSON`, `GRANDDAUGHTER`, `GRANDCHILD` |
| Cụ | `GREAT_GRANDFATHER`, `GREAT_GRANDMOTHER`, `GREAT_GRANDPARENT` |
| Chắt | `GREAT_GRANDSON`, `GREAT_GRANDDAUGHTER`, `GREAT_GRANDCHILD` |

#### `RelationLabel`

```java
public record RelationLabel(
    RelationType type,
    int greatDegree
) {
    public String code();
    public int generationOffset();
}
```

| Field / method | Kiểu | Ý nghĩa |
|---|---|---|
| `type` | `RelationType` | Quan hệ sau khi đã xét giới tính và hướng. |
| `greatDegree` | `int` | Bậc cụ/chắt; dùng cho `GREAT_*`. Với nhãn thường, nhóm labeling truyền `0`. |
| `code()` | `String` | Chuỗi output, ví dụ `FATHER` hoặc `GREAT_GRANDFATHER_2`. |
| `generationOffset()` | `int` | Khôi phục khoảng cách thế hệ có dấu để web đặt vị trí. |

Ví dụ: `new RelationLabel(GREAT_GRANDMOTHER, 1)` có `code()` là `GREAT_GRANDMOTHER_1` và `generationOffset()` là `3`.

#### `LabeledPerson`

```java
public record LabeledPerson(
    Person person,
    List<RelationLabel> relations
) {}
```

| Field | Kiểu | Ý nghĩa |
|---|---|---|
| `person` | `Person` | Thông tin người cần hiển thị. |
| `relations` | `List<RelationLabel>` | Toàn bộ nhãn quan hệ hợp lệ của người đó với target. |

Không có field `level` hoặc `relationLevels`. Web lấy độ lệch thế hệ qua `RelationLabel.generationOffset()`.

#### `ProjectResult`

```java
public interface ProjectResult {}
```

Interface đánh dấu cho kết quả cuối. `FamilyTreeResult` và `RelationshipResult` đều implements interface này.

#### `FamilyTreeResult`

```java
public record FamilyTreeResult(
    FamilyTreeQuery query,
    List<String> topoOrder,
    List<LabeledPerson> persons,
    List<ParentChildEdge> edges
) implements ProjectResult {}
```

| Field | Kiểu | Ý nghĩa |
|---|---|---|
| `query` | `FamilyTreeQuery` | Truy vấn gia phả ban đầu. |
| `topoOrder` | `List<String>` | Thứ tự tổ tiên trước, hậu duệ sau trong phần đồ thị trả về. |
| `persons` | `List<LabeledPerson>` | Người cùng các nhãn quan hệ cuối cùng. |
| `edges` | `List<ParentChildEdge>` | Cạnh dùng để output và web nối các node. |

### 3.5. Kết quả kiểm tra quan hệ ba đời — package `com.familygraph.model.relationship`

#### `RelationshipPath`

```java
public record RelationshipPath(
    String sourceId,
    String ancestorId,
    List<String> nodeIds
) {
    public int edgeCount();
}
```

| Field / method | Kiểu | Ý nghĩa |
|---|---|---|
| `sourceId` | `String` | Một trong hai người của query. |
| `ancestorId` | `String` | Tổ tiên chung ở cuối đường. |
| `nodeIds` | `List<String>` | Đường từ `sourceId` đi ngược lên tổ tiên, ví dụ `[P03, P01]`. |
| `edgeCount()` | `int` | Số cạnh trên đường, bằng `nodeIds.size() - 1`. |

`nodeIds` đi từ con lên tổ tiên nên ngược chiều lưu của `ParentChildEdge`.

#### `RelationshipResult`

```java
public record RelationshipResult(
    CheckRelationshipQuery query,
    List<String> commonAncestorIds,
    List<RelationshipPath> paths,
    List<Person> evidencePersons,
    List<ParentChildEdge> evidenceEdges
) implements ProjectResult {
    public boolean related();
}
```

| Field / method | Kiểu | Ý nghĩa |
|---|---|---|
| `query` | `CheckRelationshipQuery` | Hai người đang được kiểm tra. |
| `commonAncestorIds` | `List<String>` | Các tổ tiên chung tìm được trong phạm vi ba đời. |
| `paths` | `List<RelationshipPath>` | Đường bằng chứng từ mỗi người tới từng tổ tiên chung. |
| `evidencePersons` | `List<Person>` | Các người xuất hiện trên những đường bằng chứng. |
| `evidenceEdges` | `List<ParentChildEdge>` | Các cạnh xuất hiện trên những đường bằng chứng. |
| `related()` | `boolean` | `true` nếu `commonAncestorIds` không rỗng; không lưu thành field riêng. |

Khi không tìm được tổ tiên chung, ba list bằng chứng có thể rỗng và `related()` trả `false`.

## 4. Hợp đồng hàm theo từng thành viên

### 4.1. Đức Tiến — kiểm tra và chuẩn hóa raw input

```java
ValidationResult parseAndValidate(String rawInput)
```

| Nội dung | Object |
|---|---|
| Nhận vào | `String rawInput` — toàn bộ dữ liệu text người dùng nhập. |
| Trả về khi hợp lệ | `ValidationResult.success(NormalizedInput)` chứa `FamilyGraph` và đúng loại `ProjectQuery`. |
| Trả về khi lỗi | `ValidationResult.failure(ValidationError)` chứa mã lỗi, chi tiết và chu trình nếu có. |

Đức Tiến kiểm tra tất cả quy tắc input: số lượng bản ghi, ID, giới tính, cạnh, self-parent, chu trình, loại query, số đời, hướng và hai người trùng nhau. Hàm này không chạy topo, không dựng gia phả và không chạy BFS.

### 4.2. Phương Mai — sắp xếp topo

```java
TopoResult topologicalSort(FamilyGraph graph)
```

| Nội dung | Object |
|---|---|
| Nhận vào | `FamilyGraph graph` từ `NormalizedInput.graph()`. |
| Trả về | `TopoResult` chứa `topoOrder` của toàn bộ graph. |

Phương Mai chỉ xử lý thứ tự topo. Hàm không biết target, không lọc số đời, không tạo `relationLevels` và không gắn nhãn quan hệ.

### 4.3. Tiến Cường — dựng đồ thị con gia phả

```java
OrderedFamilyGraph buildFamilyView(
    FamilyGraph graph,
    FamilyTreeQuery query,
    TopoResult topoResult
)
```

| Nội dung | Object |
|---|---|
| Nhận vào 1 | `FamilyGraph graph` hợp lệ, đầy đủ. |
| Nhận vào 2 | `FamilyTreeQuery query` chứa target, số đời và hướng. |
| Nhận vào 3 | `TopoResult topoResult` do Phương Mai trả về. |
| Trả về | `OrderedFamilyGraph` gồm query, topo đã lọc, `FamilyNode` có `relationLevels`, và cạnh của đồ thị con. |

Tiến Cường lấy đúng vùng tổ tiên/hậu duệ theo query, giữ mọi đường quan hệ hợp lệ và tính đủ `relationLevels`. Hàm không quyết định `FATHER`, `MOTHER`, `GRANDPARENT`… vì đó là trách nhiệm của Kim Chi.

### 4.4. Kim Chi — gắn nhãn quan hệ

```java
FamilyTreeResult labelFamilyGraph(
    OrderedFamilyGraph orderedGraph
)
```

| Nội dung | Object |
|---|---|
| Nhận vào | `OrderedFamilyGraph` do Tiến Cường trả, đã có `FamilyNode.person()` và `FamilyNode.relationLevels()`. |
| Trả về | `FamilyTreeResult` chứa `LabeledPerson`, topo và cạnh giữ nguyên. |

Kim Chi duyệt từng `relationLevel`, kết hợp với `person.gender()` để tạo `RelationLabel`. Ví dụ người nam có `[1, 3]` nhận `FATHER` và `GREAT_GRANDFATHER_1`. Hàm không chạy topo, không tìm thêm đỉnh/cạnh và không kiểm tra raw input.

### 4.5. Đăng Doanh — BFS kiểm tra quan hệ ba đời

```java
RelationshipResult checkRelationship(
    FamilyGraph graph,
    CheckRelationshipQuery query
)
```

| Nội dung | Object |
|---|---|
| Nhận vào 1 | `FamilyGraph graph` hợp lệ từ `NormalizedInput.graph()`. |
| Nhận vào 2 | `CheckRelationshipQuery query` chứa hai người cần kiểm tra. |
| Trả về | `RelationshipResult` có tổ tiên chung, các `RelationshipPath`, người và cạnh bằng chứng. |

Đăng Doanh chạy BFS từ mỗi người ngược chiều cạnh đến tối đa `CheckRelationshipQuery.MAX_GENERATIONS`. Hàm không xử lý topo, không gắn nhãn cha/mẹ/ông/bà và không kiểm tra raw input.

### 4.6. Tiến Cường — tích hợp hai luồng

#### Luồng gia phả

```java
TopoResult topo = topologicalSort(input.graph());

OrderedFamilyGraph orderedGraph = buildFamilyView(
    input.graph(),
    (FamilyTreeQuery) input.query(),
    topo
);

FamilyTreeResult result = labelFamilyGraph(orderedGraph);
```

#### Luồng kiểm tra quan hệ

```java
RelationshipResult result = checkRelationship(
    input.graph(),
    (CheckRelationshipQuery) input.query()
);
```

Sau đó output layer hoặc web chỉ đọc `FamilyTreeResult` / `RelationshipResult`; không gọi lại thuật toán để suy đoán quan hệ.

## 5. Sơ đồ object đi qua các hàm

```text
Raw String
  |
  | Đức Tiến: parseAndValidate
  v
ValidationResult
  |-- invalid --> ValidationError --> output lỗi
  |
  '-- valid --> NormalizedInput
                   |
                   |-- FamilyTreeQuery --> Phương Mai: topologicalSort
                   |                         |
                   |                         v
                   |                       TopoResult
                   |                         |
                   |                         v
                   |                       Tiến Cường: buildFamilyView
                   |                         |
                   |                         v
                   |                       OrderedFamilyGraph
                   |                         |
                   |                         v
                   |                       Kim Chi: labelFamilyGraph
                   |                         |
                   |                         v
                   |                       FamilyTreeResult --> web/output
                   |
                   '-- CheckRelationshipQuery --> Đăng Doanh: checkRelationship
                                                     |
                                                     v
                                               RelationshipResult --> web/output
```

## 6. Quy tắc thay đổi hợp đồng

- Trước khi sửa field, enum, package hoặc chữ ký hàm, tạo pull request chỉ cho thay đổi hợp đồng và mô tả module nào bị ảnh hưởng.
- Tiến Cường review thay đổi đó trước khi merge vào `main`.
- Sau khi hợp đồng đổi, người phụ trách module cập nhật test và mọi import liên quan trong cùng pull request hoặc pull request kế tiếp đã được thống nhất.
