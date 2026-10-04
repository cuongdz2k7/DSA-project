# Luồng xử lý Input: Parser → Validator → Normalizer

Tài liệu này giải thích module `com.familygraph.input`: dữ liệu đi qua những object nào, mỗi hàm nhận gì, trả gì và chịu trách nhiệm kiểm tra phần nào.

API mà các module khác sử dụng vẫn là:

```java
ValidationResult parseAndValidate(String rawInput)
```

Bên trong API này, công việc được chia thành ba tầng độc lập:

```text
String rawInput
    ↓ RawTextParser.parse(...)
ParseResult
    ↓ lấy ParsedInput nếu parse thành công
ParsedInputValidator.validate(...)
    ↓ lấy ValidatedInput nếu dữ liệu hợp lệ
InputNormalizer.normalize(...)
    ↓
NormalizedInput
    ↓
ValidationResult.success(...)
```

Nếu một tầng thất bại, pipeline dừng ngay và trả `ValidationResult.failure(...)`. Tầng sau không được chạy.

---

## 1. Ví dụ xuyên suốt

Raw input hợp lệ:

```text
PERSON_COUNT 3
PERSON P01 MALE "Nam"
PERSON P02 FEMALE "Hoa Nguyễn"
PERSON P03 UNKNOWN

EDGE_COUNT 2
EDGE P01 P03
EDGE P02 P03

QUERY FAMILY_TREE P03 2 ANCESTORS
```

Ý nghĩa:

- Có ba người: `P01`, `P02`, `P03`.
- `P01` và `P02` là phụ huynh của `P03`.
- Truy vấn yêu cầu tìm gia phả tổ tiên của `P03` trong hai đời, tính cả `P03`.

Kết quả cuối cùng là:

```text
ValidationResult
├── valid = true
├── data = NormalizedInput
│   ├── graph = FamilyGraph
│   └── query = FamilyTreeQuery
└── error = null
```

---

## 2. Tầng facade: `RawInputParser`

### 2.1. Vai trò

`RawInputParser` là điểm vào duy nhất dành cho module bên ngoài. Class này không tự chứa chi tiết parse hoặc validation; nó chỉ điều phối ba tầng.

```java
public final class RawInputParser {
    private final RawTextParser parser;
    private final ParsedInputValidator validator;
    private final InputNormalizer normalizer;
}
```

| Field | Kiểu | Ý nghĩa |
|---|---|---|
| `parser` | `RawTextParser` | Đọc raw text thành object trung gian. |
| `validator` | `ParsedInputValidator` | Kiểm tra quy tắc dữ liệu và đồ thị. |
| `normalizer` | `InputNormalizer` | Chuyển dữ liệu hợp lệ thành domain model chính thức. |

### 2.2. Constructor mặc định

```java
public RawInputParser()
```

Tạo đầy đủ ba thành phần mặc định:

```java
new RawTextParser();
new ParsedInputValidator();
new InputNormalizer();
```

Ví dụ sử dụng:

```java
RawInputParser inputParser = new RawInputParser();
ValidationResult result = inputParser.parseAndValidate(rawInput);
```

### 2.3. Constructor nhận dependency

```java
public RawInputParser(
    RawTextParser parser,
    ParsedInputValidator validator,
    InputNormalizer normalizer
)
```

Constructor này cho phép truyền các thành phần từ bên ngoài, hữu ích khi test hoặc khi sau này project dùng dependency injection.

### 2.4. Hàm `parseAndValidate`

```java
public ValidationResult parseAndValidate(String rawInput)
```

Nhận vào:

| Tham số | Kiểu | Ý nghĩa |
|---|---|---|
| `rawInput` | `String` | Toàn bộ nội dung người, cạnh và một query. |

Luồng xử lý:

```java
ParseResult parsed = parser.parse(rawInput);
```

- Nếu `parsed.successful() == false`, trả ngay `ValidationResult.failure(parsed.error())`.
- Nếu thành công, chuyển `parsed.data()` sang validator.

```java
GraphValidationResult validated = validator.validate(parsed.data());
```

- Nếu `validated.valid() == false`, trả ngay `ValidationResult.failure(validated.error())`.
- Nếu thành công, chuyển `validated.data()` sang normalizer.

```java
NormalizedInput data = normalizer.normalize(validated.data());
return ValidationResult.success(data);
```

Facade không chạy topo, không dựng gia phả và không chạy BFS.

---

## 3. Tầng 1: `RawTextParser`

### 3.1. Trách nhiệm

`RawTextParser` chỉ kiểm tra xem raw text có đọc được theo cấu trúc đã quy ước hay không.

Tầng này làm:

- Bỏ dòng trống.
- Bỏ khoảng trắng đầu và cuối dòng.
- Chấp nhận nhiều khoảng trắng giữa các token.
- Tìm các marker `PERSON_COUNT`, `EDGE_COUNT`, `QUERY`.
- Đọc dòng `PERSON`, `EDGE` và `QUERY`.
- Đọc tên trống, tên một từ hoặc tên nhiều từ trong dấu `"..."`.
- Tạo các object `Parsed*`.

Tầng này không kiểm tra:

- ID trùng.
- Giới tính có thuộc enum hay không.
- ID trong cạnh có tồn tại hay không.
- Cạnh trùng, self-parent hoặc chu trình.
- Hai phụ huynh có cùng giới tính hay không.
- Số đời và hướng query có hợp lệ hay không.

### 3.2. Hàm `parse`

```java
public ParseResult parse(String rawInput)
```

Nhận `String`, trả `ParseResult`.

Ví dụ token giới tính chưa hợp lệ:

```text
PERSON_COUNT 1
PERSON P01 OTHER "Nam"
EDGE_COUNT 0
QUERY FAMILY_TREE P01 1 BOTH
```

Parser vẫn thành công và tạo:

```java
new ParsedPerson("P01", "OTHER", "Nam")
```

`OTHER` chỉ bị từ chối ở tầng validator.

Nếu sai cú pháp:

```text
PERSON P01 MALE "Nam
```

Parser trả:

```java
ParseResult.failure(
    new ValidationError(
        ValidationErrorCode.MALFORMED_INPUT,
        "line 2: invalid PERSON record",
        List.of()
    )
)
```

### 3.3. Các hàm nội bộ của parser

#### `parseDocument`

```java
private ParsedInput parseDocument(String rawInput)
```

Điều phối toàn bộ việc đọc văn bản:

1. Từ chối `null` hoặc chuỗi rỗng.
2. Chuẩn hóa danh sách dòng.
3. Đọc `PERSON_COUNT`.
4. Tìm `EDGE_COUNT` và đọc các dòng người ở giữa.
5. Đọc `EDGE_COUNT`.
6. Tìm `QUERY` và đọc các dòng cạnh ở giữa.
7. Bảo đảm `QUERY` là dòng nội dung cuối cùng.
8. Tạo `ParsedFamilyGraph`, `ParsedQuery` và `ParsedInput`.

#### `parseCountLine`

```java
private int parseCountLine(
    String line,
    String expectedKeyword,
    int lineNumber
)
```

Kiểm tra một dòng count có đúng hai token và đúng marker hay không.

Hợp lệ:

```text
PERSON_COUNT 3
EDGE_COUNT 2
```

Không hợp lệ:

```text
PERSON_COUNT three
PERSON_COUNT 3 EXTRA
```

Hàm chỉ kiểm tra token count có đọc thành số nguyên được không. Giá trị âm hoặc không khớp số bản ghi được validator xử lý sau.

#### `parsePersonLine`

```java
private ParsedPerson parsePersonLine(String line, int lineNumber)
```

Các dạng tên được hỗ trợ:

```text
PERSON P01 MALE
PERSON P01 MALE Nam
PERSON P01 MALE "Nguyễn Văn Nam"
```

Kết quả tương ứng:

```java
new ParsedPerson("P01", "MALE", "")
new ParsedPerson("P01", "MALE", "Nam")
new ParsedPerson("P01", "MALE", "Nguyễn Văn Nam")
```

Tên có khoảng trắng bắt buộc đặt trong dấu ngoặc kép.

#### `parseEdgeLine`

```java
private ParsedParentChildEdge parseEdgeLine(String line, int lineNumber)
```

Dòng hợp lệ phải có đúng ba token:

```text
EDGE P01 P03
```

Kết quả:

```java
new ParsedParentChildEdge("P01", "P03")
```

Parser chưa kiểm tra `P01` và `P03` có tồn tại hay không.

#### `parseQueryLine`

```java
private ParsedQuery parseQueryLine(String line, int lineNumber)
```

Hai format đã biết:

```text
QUERY FAMILY_TREE P03 2 ANCESTORS
QUERY CHECK_RELATIONSHIP P03 P04
```

Kết quả:

```java
new ParsedQuery(
    "FAMILY_TREE",
    List.of("P03", "2", "ANCESTORS")
)
```

hoặc:

```java
new ParsedQuery(
    "CHECK_RELATIONSHIP",
    List.of("P03", "P04")
)
```

Parser kiểm tra số tham số của hai query đã biết. Việc kiểm tra `2`, `ANCESTORS` hoặc ID thuộc về validator.

#### `findMarker`

```java
private int findMarker(
    List<String> lines,
    String keyword,
    int startIndex
)
```

Tìm vị trí đầu tiên của marker như `EDGE_COUNT` hoặc `QUERY`, bắt đầu từ `startIndex`. Nếu không tìm thấy, trả `-1` và parser tạo lỗi `MALFORMED_INPUT`.

#### `splitTokens`

```java
private String[] splitTokens(String line)
```

Tách dòng theo một hoặc nhiều khoảng trắng. Vì vậy hai dòng sau được đọc giống nhau:

```text
EDGE P01 P03
EDGE     P01     P03
```

---

## 4. Các object trung gian của parser

Các object này nằm trong package:

```text
com.familygraph.input.model
```

Chúng không phải domain model chính thức dùng bởi nhóm 1 và nhóm 2.

### 4.1. `ParsedPerson`

```java
public record ParsedPerson(
    String id,
    String genderToken,
    String name
) {}
```

| Field | Kiểu | Ý nghĩa |
|---|---|---|
| `id` | `String` | ID vừa đọc từ dòng `PERSON`; chưa kiểm tra trùng. |
| `genderToken` | `String` | Token giới tính chưa chuyển thành `Gender`. |
| `name` | `String` | Tên hiển thị; dùng `""` nếu input không có tên. |

Ví dụ:

```java
new ParsedPerson("P02", "FEMALE", "Hoa Nguyễn")
```

### 4.2. `ParsedParentChildEdge`

```java
public record ParsedParentChildEdge(
    String parentId,
    String childId
) {}
```

| Field | Kiểu | Ý nghĩa |
|---|---|---|
| `parentId` | `String` | ID phía cha/mẹ của cạnh. |
| `childId` | `String` | ID phía người con của cạnh. |

Chiều cạnh luôn được hiểu là:

```text
parentId → childId
```

### 4.3. `ParsedFamilyGraph`

```java
public record ParsedFamilyGraph(
    int declaredPersonCount,
    List<ParsedPerson> persons,
    int declaredEdgeCount,
    List<ParsedParentChildEdge> edges
) {}
```

| Field | Kiểu | Ý nghĩa |
|---|---|---|
| `declaredPersonCount` | `int` | Giá trị ghi sau `PERSON_COUNT`. |
| `persons` | `List<ParsedPerson>` | Các dòng `PERSON` thực tế, giữ thứ tự input. |
| `declaredEdgeCount` | `int` | Giá trị ghi sau `EDGE_COUNT`. |
| `edges` | `List<ParsedParentChildEdge>` | Các dòng `EDGE` thực tế, giữ thứ tự input. |

Ví dụ input khai báo sai:

```text
PERSON_COUNT 3
PERSON P01 MALE
PERSON P02 FEMALE
```

Object vẫn có thể chứa:

```text
declaredPersonCount = 3
persons.size()       = 2
```

Sự khác nhau này được `validateCounts` phát hiện.

### 4.4. `ParsedQuery`

```java
public record ParsedQuery(
    String type,
    List<String> arguments
) {}
```

| Field | Kiểu | Ý nghĩa |
|---|---|---|
| `type` | `String` | Token loại query, ví dụ `FAMILY_TREE`. |
| `arguments` | `List<String>` | Các tham số vẫn ở dạng chuỗi. |

Ví dụ:

```java
new ParsedQuery(
    "FAMILY_TREE",
    List.of("P03", "2", "ANCESTORS")
)
```

### 4.5. `ParsedInput`

```java
public record ParsedInput(
    ParsedFamilyGraph graph,
    ParsedQuery query
) {}
```

| Field | Kiểu | Ý nghĩa |
|---|---|---|
| `graph` | `ParsedFamilyGraph` | Toàn bộ người, cạnh và count vừa đọc. |
| `query` | `ParsedQuery` | Query vừa đọc. |

`ParsedInput` có nghĩa là “đọc được cú pháp”, không có nghĩa là “dữ liệu hợp lệ”.

### 4.6. `ParseResult`

```java
public record ParseResult(
    boolean successful,
    ParsedInput data,
    ValidationError error
) {}
```

| Field | Ý nghĩa khi thành công | Ý nghĩa khi thất bại |
|---|---|---|
| `successful` | `true` | `false` |
| `data` | Có `ParsedInput` | `null` |
| `error` | `null` | Có lỗi `MALFORMED_INPUT` |

Phải tạo qua:

```java
ParseResult.success(parsedInput);
ParseResult.failure(validationError);
```

Record bảo vệ trạng thái, không cho phép vừa có `data` vừa có `error`.

---

## 5. Tầng 2: `ParsedInputValidator`

### 5.1. Trách nhiệm

Validator nhận object đã đúng cú pháp và kiểm tra logic dữ liệu theo thứ tự ưu tiên cố định.

```java
public GraphValidationResult validate(ParsedInput input)
```

Validator trả lỗi đầu tiên tìm thấy. Điều này giúp output ổn định khi một input chứa nhiều lỗi.

### 5.2. Thứ tự kiểm tra

| Thứ tự | Mã lỗi | Hàm/phần kiểm tra |
|---:|---|---|
| 1 | `INVALID_COUNT` | `validateCounts` |
| 2 | `DUPLICATE_ID` | `validateDuplicateIds` |
| 3 | `INVALID_GENDER` | `validateGenders` |
| 4 | `UNKNOWN_ID` | `validateKnownIds` |
| 5 | `DUPLICATE_EDGE` | `validateDuplicateEdges` |
| 6 | `SELF_PARENT` | `validateSelfParent` |
| 7 | `DIRECTED_CYCLE` | `findCycle` |
| 8 | `SAME_GENDER_PARENTS` | `validateParentGenders` |
| 9 | `INVALID_QUERY_TYPE` | `validateQuery` |
| 10 | `INVALID_GENERATION` | `validateQuery` |
| 11 | `INVALID_DIRECTION` | `validateQuery` |
| 12 | `SAME_PERSON_QUERY` | `validateQuery` |

Ví dụ: nếu cùng một input vừa có duplicate edge vừa có hai phụ huynh nam, kết quả là `DUPLICATE_EDGE` vì lỗi này được kiểm tra trước.

### 5.3. `validateCounts`

Kiểm tra:

- `PERSON_COUNT >= 1`.
- `PERSON_COUNT` bằng số dòng người thực tế.
- `EDGE_COUNT >= 0`.
- `EDGE_COUNT` bằng số dòng cạnh thực tế.

Ví dụ lỗi:

```text
PERSON_COUNT 3
PERSON P01 MALE
PERSON P02 FEMALE
```

Kết quả:

```text
code   = INVALID_COUNT
detail = PERSON_COUNT expected 3 but found 2
```

### 5.4. `validateDuplicateIds`

Kiểm tra mỗi ID chỉ xuất hiện một lần trong danh sách người.

```text
PERSON P01 MALE
PERSON P01 FEMALE
```

Kết quả:

```text
code   = DUPLICATE_ID
detail = P01
```

### 5.5. `validateGenders`

Chỉ chấp nhận:

```text
MALE
FEMALE
UNKNOWN
```

Ví dụ:

```text
PERSON P01 OTHER
```

Kết quả:

```text
code   = INVALID_GENDER
detail = OTHER
```

### 5.6. `collectPersonIds`

Tạo `Set<String>` chứa toàn bộ ID người để các bước sau tra cứu nhanh.

Ví dụ:

```text
P01, P02, P03
```

được chuyển thành tập:

```java
Set.of("P01", "P02", "P03")
```

Đây là helper chuẩn bị dữ liệu, không tự trả lỗi.

### 5.7. `validateKnownIds`

Kiểm tra theo thứ tự:

1. `parentId` và `childId` trong từng cạnh.
2. ID trong query.

Ví dụ:

```text
EDGE P99 P03
```

nếu `P99` không có trong danh sách người:

```text
code   = UNKNOWN_ID
detail = P99
```

### 5.8. `validateDuplicateEdges`

Hai cạnh có cùng `parentId` và `childId` không được xuất hiện lặp lại.

```text
EDGE P01 P03
EDGE P01 P03
```

Kết quả:

```text
code   = DUPLICATE_EDGE
detail = P01 P03
```

### 5.9. `validateSelfParent`

Một người không được là phụ huynh trực tiếp của chính mình.

```text
EDGE P01 P01
```

Kết quả:

```text
code   = SELF_PARENT
detail = P01
```

### 5.10. `findCycle`

Chữ ký được validator cung cấp:

```java
public List<String> findCycle(ParsedFamilyGraph graph)
```

Việc tìm chu trình được tách sang `DirectedCycleDetector`. Thuật toán dùng DFS và duyệt người/cạnh theo thứ tự input để kết quả ổn định.

Ví dụ:

```text
EDGE P01 P02
EDGE P02 P03
EDGE P03 P01
```

Kết quả chu trình khép kín:

```java
List.of("P01", "P02", "P03", "P01")
```

Validator tạo lỗi:

```text
code   = DIRECTED_CYCLE
detail = P01 -> P02 -> P03 -> P01
cycle  = [P01, P02, P03, P01]
```

Nếu không có chu trình, `findCycle` trả `List.of()`.

### 5.11. DFS nội bộ trong `DirectedCycleDetector`

Mỗi người có một trong ba trạng thái:

```text
UNVISITED: chưa duyệt
VISITING:  đang nằm trên đường DFS hiện tại
VISITED:   đã duyệt xong
```

Khi từ một đỉnh gặp lại đỉnh đang `VISITING`, thuật toán đã tìm thấy chu trình. `activePath` và `activeIndexes` được dùng để lấy đúng đoạn tạo thành vòng.

### 5.12. `validateParentGenders`

Chữ ký:

```java
public ValidationError validateParentGenders(
    ParsedFamilyGraph graph
)
```

Logic được tách sang `ParentGenderValidator`.

Quy tắc:

- Hai phụ huynh khác ID cùng `MALE` của một người → lỗi.
- Hai phụ huynh khác ID cùng `FEMALE` của một người → lỗi.
- `MALE + FEMALE` → hợp lệ.
- `MALE + UNKNOWN` → hợp lệ.
- `FEMALE + UNKNOWN` → hợp lệ.
- `UNKNOWN + UNKNOWN` → hợp lệ.
- `MALE + FEMALE + UNKNOWN` → hợp lệ.
- Một người có nhiều hơn hai phụ huynh vẫn được phép nếu không vi phạm quy tắc trên.

Ví dụ lỗi:

```text
PERSON P01 MALE
PERSON P02 MALE
PERSON P03 FEMALE
EDGE P01 P03
EDGE P02 P03
```

Kết quả:

```text
code   = SAME_GENDER_PARENTS
detail = child=P03 parents=P01,P02 gender=MALE
cycle  = []
```

`UNKNOWN` bị bỏ qua vì không đủ dữ liệu để kết luận giới tính phụ huynh bị trùng.

### 5.13. `validateQuery`

Với mọi query, kiểm tra loại query trước:

```text
FAMILY_TREE
CHECK_RELATIONSHIP
```

Loại khác trả `INVALID_QUERY_TYPE`.

Với `FAMILY_TREE`, kiểm tra:

- Số đời đọc được thành số nguyên.
- Số đời từ 1 trở lên.
- Hướng thuộc `ANCESTORS`, `DESCENDANTS`, `BOTH`.

Ví dụ:

```text
QUERY FAMILY_TREE P03 0 BOTH
```

trả:

```text
INVALID_GENERATION
```

```text
QUERY FAMILY_TREE P03 2 SIDEWAYS
```

trả:

```text
INVALID_DIRECTION
```

Với `CHECK_RELATIONSHIP`, hai ID phải khác nhau:

```text
QUERY CHECK_RELATIONSHIP P03 P03
```

trả:

```text
SAME_PERSON_QUERY
```

### 5.14. `GraphValidationResult`

```java
public record GraphValidationResult(
    boolean valid,
    ValidatedInput data,
    ValidationError error
) {}
```

| Field | Khi hợp lệ | Khi không hợp lệ |
|---|---|---|
| `valid` | `true` | `false` |
| `data` | Có `ValidatedInput` | `null` |
| `error` | `null` | Có `ValidationError` |

Tạo bằng:

```java
GraphValidationResult.success(validatedInput);
GraphValidationResult.failure(validationError);
```

### 5.15. `ValidatedInput`

```java
public record ValidatedInput(
    ParsedInput parsedInput
) {}
```

Object này là dấu xác nhận rằng `ParsedInput` đã vượt qua validator. `InputNormalizer` chỉ nhận `ValidatedInput`, tránh việc vô tình normalize dữ liệu chưa kiểm tra.

---

## 6. Tầng 3: `InputNormalizer`

### 6.1. Trách nhiệm

```java
public NormalizedInput normalize(ValidatedInput input)
```

Normalizer chỉ chuyển kiểu dữ liệu:

```text
ParsedPerson          → Person
ParsedParentChildEdge → ParentChildEdge
ParsedFamilyGraph     → FamilyGraph
ParsedQuery           → FamilyTreeQuery hoặc CheckRelationshipQuery
```

Normalizer không kiểm tra lại duplicate, cycle, ID hoặc query. Những quy tắc đó đã được validator xác nhận.

### 6.2. Chuyển người

Trước:

```java
new ParsedPerson("P01", "MALE", "Nam")
```

Sau:

```java
new Person("P01", Gender.MALE, "Nam")
```

Điểm quan trọng là `genderToken` dạng chuỗi được chuyển thành enum `Gender`.

### 6.3. Chuyển cạnh

Trước:

```java
new ParsedParentChildEdge("P01", "P03")
```

Sau:

```java
new ParentChildEdge("P01", "P03")
```

Thứ tự cạnh và người vẫn giữ nguyên theo input.

### 6.4. Chuyển query gia phả

Trước:

```java
new ParsedQuery(
    "FAMILY_TREE",
    List.of("P03", "2", "ANCESTORS")
)
```

Sau:

```java
new FamilyTreeQuery(
    "P03",
    2,
    Direction.ANCESTORS
)
```

### 6.5. Chuyển query kiểm tra quan hệ

Trước:

```java
new ParsedQuery(
    "CHECK_RELATIONSHIP",
    List.of("P03", "P04")
)
```

Sau:

```java
new CheckRelationshipQuery("P03", "P04")
```

### 6.6. `NormalizedInput`

```java
public record NormalizedInput(
    FamilyGraph graph,
    ProjectQuery query
) {}
```

| Field | Kiểu | Ý nghĩa |
|---|---|---|
| `graph` | `FamilyGraph` | Đồ thị chính thức, hợp lệ, giữ thứ tự người và cạnh. |
| `query` | `ProjectQuery` | `FamilyTreeQuery` hoặc `CheckRelationshipQuery` hợp lệ. |

Đây là object duy nhất được chuyển cho nhóm 1 và nhóm 2.

---

## 7. Object lỗi dùng chung

### 7.1. `ValidationError`

```java
public record ValidationError(
    ValidationErrorCode code,
    String detail,
    List<String> cycle
) {}
```

| Field | Ý nghĩa |
|---|---|
| `code` | Mã lỗi ổn định để output layer xử lý. |
| `detail` | Token, ID, cạnh hoặc nội dung ngắn giúp xác định lỗi. |
| `cycle` | Chu trình khép kín; rỗng với mọi lỗi khác `DIRECTED_CYCLE`. |

Ví dụ lỗi ID:

```java
new ValidationError(
    ValidationErrorCode.UNKNOWN_ID,
    "P99",
    List.of()
)
```

Ví dụ lỗi chu trình:

```java
new ValidationError(
    ValidationErrorCode.DIRECTED_CYCLE,
    "P01 -> P02 -> P03 -> P01",
    List.of("P01", "P02", "P03", "P01")
)
```

### 7.2. `ValidationResult`

```java
public record ValidationResult(
    boolean valid,
    NormalizedInput data,
    ValidationError error
) {}
```

Đây là kết quả công khai cuối cùng của facade.

Thành công:

```text
valid = true
data  = NormalizedInput
error = null
```

Thất bại:

```text
valid = false
data  = null
error = ValidationError
```

---

## 8. Ba ví dụ luồng hoàn chỉnh

### 8.1. Thành công

```text
PERSON_COUNT 2
PERSON P01 MALE "Nam"
PERSON P02 FEMALE "Lan"
EDGE_COUNT 0
QUERY CHECK_RELATIONSHIP P01 P02
```

Luồng:

```text
RawTextParser
→ ParseResult.success(ParsedInput)

ParsedInputValidator
→ GraphValidationResult.success(ValidatedInput)

InputNormalizer
→ NormalizedInput(FamilyGraph, CheckRelationshipQuery)

RawInputParser
→ ValidationResult.success(NormalizedInput)
```

### 8.2. Sai cú pháp

```text
PERSON_COUNT 1
PERSON P01 MALE "Nam
EDGE_COUNT 0
QUERY FAMILY_TREE P01 1 BOTH
```

Luồng dừng tại parser:

```text
RawTextParser
→ ParseResult.failure(MALFORMED_INPUT)

RawInputParser
→ ValidationResult.failure(MALFORMED_INPUT)
```

Validator và normalizer không chạy.

### 8.3. Đúng cú pháp nhưng sai nghiệp vụ

```text
PERSON_COUNT 3
PERSON P01 FEMALE "A"
PERSON P02 FEMALE "B"
PERSON P03 MALE "C"
EDGE_COUNT 2
EDGE P01 P03
EDGE P02 P03
QUERY FAMILY_TREE P03 2 ANCESTORS
```

Luồng:

```text
RawTextParser
→ ParseResult.success(ParsedInput)

ParsedInputValidator
→ GraphValidationResult.failure(SAME_GENDER_PARENTS)

RawInputParser
→ ValidationResult.failure(SAME_GENDER_PARENTS)
```

Normalizer không chạy.

---

## 9. Ranh giới trách nhiệm

| Thành phần | Được làm | Không được làm |
|---|---|---|
| `RawTextParser` | Đọc và tách raw text | Không kết luận giới tính, ID, cycle hoặc query có hợp lệ |
| `ParsedInputValidator` | Kiểm tra toàn bộ quy tắc input | Không tạo domain model chính thức, không chạy thuật toán gia phả |
| `InputNormalizer` | Chuyển dữ liệu hợp lệ sang domain model | Không chạy lại validation |
| `RawInputParser` | Điều phối ba tầng và trả `ValidationResult` | Không chứa chi tiết từng quy tắc |
| Nhóm 1 và nhóm 2 | Nhận `NormalizedInput` và chạy thuật toán | Không đọc raw text và không kiểm tra lại input |

Việc tách trách nhiệm giúp thay đổi cú pháp, quy tắc validation hoặc domain model độc lập hơn và giúp test xác định chính xác lỗi thuộc tầng nào.
