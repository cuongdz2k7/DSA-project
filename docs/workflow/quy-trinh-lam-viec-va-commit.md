# Quy trình làm việc, Commit và Pull Request

Tài liệu này là quy ước làm việc chung của project đồ thị gia phả. Mục tiêu là để mọi thay đổi đều có người chịu trách nhiệm, có test đi kèm và được Tiến Cường xem trước khi đi vào `main`.

## 1. Quy tắc bắt buộc

- Không ai `push` hoặc commit trực tiếp vào branch `main`.
- Mỗi đầu việc được làm trên **một branch riêng** và kết thúc bằng **một Pull Request (PR)** vào `main`.
- Không merge PR khi chưa có review `Approve` của Tiến Cường, với vai trò Code Owner.
- Comment như `LGTM` không thay cho thao tác `Approve` trong mục **Files changed → Review changes** trên GitHub.
- Không sửa package `model.*`, chữ ký hàm chung, enum hay output contract trong một PR thuật toán thông thường. Thay đổi này cần PR hợp đồng riêng và phải mô tả module bị ảnh hưởng.
- Trước khi tạo PR, người viết code phải chạy test liên quan và `mvn test`.
- Một PR chỉ nên xử lý một mục tiêu rõ ràng. Không gộp sửa lỗi, đổi model, sửa web và dọn code không liên quan trong cùng PR.

## 2. Phân quyền trên GitHub

Branch rule của `main` cần giữ các thiết lập sau:

- **Require a pull request before merging**: bật.
- **Required approvals**: ít nhất `1`.
- **Require review from Code Owners**: bật; `CODEOWNERS` chỉ định Tiến Cường.
- **Dismiss stale pull request approvals when new commits are pushed**: bật. Có commit mới thì Tiến Cường phải review lại phiên bản mới nhất.
- **Require conversation resolution before merging**: bật.

Với cấu hình này, mọi thành viên có thể tạo PR. Chỉ sau khi Tiến Cường `Approve` đúng phiên bản code hiện tại, PR mới đủ điều kiện merge. Người bấm nút merge có thể là tác giả PR hoặc người được nhóm phân công, nhưng không được merge trước khi đủ các điều kiện trên.

## 3. Đặt tên branch

Mỗi branch bắt đầu bằng loại công việc, sau đó là vùng phụ trách và mô tả ngắn bằng chữ thường, nối bằng dấu gạch ngang.

```text
feature/familytree-labeling
feature/topological-sort
feature/relationship-bfs
feature/input-validation
test/relationship-edge-cases
fix/familytree-great-label
docs/presentation-trace
refactor/model-contract
```

Không dùng tên chung chung như `test`, `new-branch`, `cuong`, `mai-branch` hoặc `final-final`.

## 4. Bắt đầu một đầu việc mới

Mỗi người làm theo thứ tự sau trong thư mục `DSA-project`.

### Bước 1 — cập nhật `main`

```powershell
git switch main
git pull origin main
```

Chỉ tạo branch mới khi `main` trên máy đã là bản mới nhất.

### Bước 2 — tạo branch cho đầu việc

```powershell
git switch -c feature/familytree-labeling
```

Mỗi branch chỉ làm một phần. Ví dụ Kim Chi làm labeling không thêm thuật toán BFS vào cùng branch.

### Bước 3 — đọc hợp đồng trước khi code

Phải đọc hai tài liệu sau trước khi viết code có giao tiếp với module khác:

- [Hướng dẫn model và hàm](../contracts/huong-dan-model-va-ham.md)
- [Hợp đồng model ngắn gọn](../contracts/model-contract.md)

Nhóm 1 và nhóm 2 chỉ dùng `NormalizedInput` hợp lệ. Nhóm 3 mới đọc `String rawInput` và tạo `ValidationResult`.

## 5. Trong khi làm việc

### 5.1. Không làm hỏng phần của người khác

- Chỉ sửa file thuộc vùng công việc của mình, trừ khi PR đã thống nhất cần sửa chỗ dùng chung.
- Không đổi tên, format toàn bộ project hoặc xóa file không liên quan chỉ để "dọn code".
- Không commit thư mục `target/`, `.idea/`, `.vscode/`, file `.iml` hoặc cấu hình cá nhân.
- Không tự ý sửa `docs/contracts/` khi đang làm thuật toán. Nếu phát hiện hợp đồng thiếu, báo Tiến Cường trước.

### 5.2. Viết test cùng với code

Mỗi hàm thuật toán phải có test tương ứng trong `src/test/java`.

| Người phụ trách | Vùng test chính |
|---|---|
| Phương Mai | Thứ tự topo, nhiều cha/mẹ, đồ thị rời rạc, thứ tự ổn định. |
| Kim Chi | Nhãn theo giới tính, `UNKNOWN`, quan hệ nhiều đường, các bậc cụ/chắt. |
| Tiến Cường | Lấy đồ thị con theo hướng và số đời, tích hợp topo → view → labeling. |
| Đăng Doanh | BFS ba đời, giao tổ tiên, không có giao, đường bằng chứng. |
| Đức Tiến | Input đúng/sai, ID trùng, cạnh sai, query sai, chu trình. |
| Đắc Thịnh | Edge case BFS, nhiều nhánh, giới hạn độ sâu, review chéo test. |

Test ví dụ chung có thể để ở `examples/valid/` và `examples/invalid/`. Unit test Java không đọc ví dụ chung một cách ngầm định; nếu dùng một file example trong test thì test phải ghi rõ mục đích và kết quả mong đợi.

### 5.3. Kiểm tra trước khi commit

```powershell
git status
git diff
mvn test
```

`git diff` giúp xem chính xác mình đã sửa gì trước khi đưa vào commit. Nếu `mvn test` lỗi, không tạo PR với trạng thái lỗi trừ khi PR chỉ để nhờ hỗ trợ và phải ghi rõ lý do trong mô tả.

## 6. Quy ước commit

### 6.1. Cấu trúc message

```text
<type>(<scope>): <mô tả ngắn ở dạng mệnh lệnh>
```

`scope` có thể bỏ nếu không rõ, nhưng nên dùng khi thay đổi thuộc một module cụ thể.

| Type | Khi dùng | Ví dụ |
|---|---|---|
| `feat` | Thêm chức năng mới | `feat(familytree): add relation labeling` |
| `fix` | Sửa lỗi | `fix(relationship): keep shortest ancestor path` |
| `test` | Thêm hoặc sửa test | `test(input): cover directed cycle cases` |
| `docs` | Chỉ sửa tài liệu | `docs: add commit workflow` |
| `refactor` | Đổi cấu trúc nhưng không đổi hành vi | `refactor(model): move validation out of records` |
| `chore` | Việc nền như Maven, gitignore | `chore: update Maven test configuration` |

Message nên ngắn, mô tả việc đã làm; không dùng:

```text
update
fix bug
done
code cua Mai
final final
```

### 6.2. Một commit = một thay đổi hợp lý

Ví dụ tốt cho nhánh Kim Chi:

```text
feat(familytree): map parent labels by gender
feat(familytree): support great ancestor labels
test(familytree): cover unknown gender labels
```

Ví dụ không tốt: một commit vừa đổi model chung, thêm BFS, sửa CSS web và đổi toàn bộ README.

### 6.3. Lệnh commit an toàn

Không dùng ngay `git add .` hoặc `git add -A` nếu chưa xem file thay đổi. Thay vào đó:

```powershell
git status
git add src/main/java/com/familygraph/familytree/RelationLabeler.java
git add src/test/java/com/familygraph/familytree/RelationLabelerTest.java
git diff --cached
git commit -m "feat(familytree): add relation labeling"
```

`git diff --cached` là lần kiểm tra cuối: chỉ những file ở đó mới đi vào commit.

## 7. Push branch và tạo Pull Request

### Bước 1 — push branch lần đầu

```powershell
git push -u origin feature/familytree-labeling
```

Các lần push sau:

```powershell
git push
```

### Bước 2 — tạo PR trên GitHub

Chọn:

```text
base: main
compare: feature/familytree-labeling
```

Tiêu đề PR dùng cùng phong cách commit, ví dụ:

```text
feat(familytree): add relation labeling
```

Mô tả PR cần có đủ mẫu sau:

```markdown
## Mục tiêu
- [Việc PR này giải quyết]

## Thay đổi chính
- [File hoặc vùng logic 1]
- [File hoặc vùng logic 2]

## Hợp đồng nhận/trả
- Nhận: [object / hàm]
- Trả: [object / hàm]

## Kiểm thử
- [ ] mvn test đã pass
- [ ] Đã thêm/sửa test: [tên test hoặc trường hợp]
- [ ] Đã kiểm tra output ví dụ: [mô tả ngắn]

## Ngoài phạm vi
- [Điều PR này cố ý chưa làm]
```

Sau khi mở PR, gắn Tiến Cường vào mục **Reviewers**. Không gộp nhiều commit mới vào PR mà không cập nhật phần mô tả.

## 8. Quy trình review và sửa theo review

### 8.1. Việc của người tạo PR

- Tự xem tab **Files changed** trước khi yêu cầu review.
- Trả lời từng comment bằng lý do hoặc commit sửa tương ứng.
- Nếu nhận `Request changes`, sửa trên **chính branch đó**, chạy test lại rồi `git push`.
- Không tạo PR thứ hai để sửa một PR đang mở, trừ khi Tiến Cường yêu cầu tách nhỏ.

### 8.2. Việc của Tiến Cường khi review

- Xem mục tiêu PR, diff, test và mức ảnh hưởng đến hợp đồng chung.
- Comment trực tiếp vào dòng code khi cần sửa; dùng `Request changes` nếu chưa đạt.
- Chỉ bấm `Approve` trong **Review changes** khi code, test và mô tả đều ổn.
- Vì bật **Dismiss stale approvals**, nếu tác giả push commit mới sau approval thì approval cũ mất hiệu lực. Tiến Cường phải review và approve lại.

### 8.3. Khi nào được merge

PR chỉ được merge khi:

- Có review `Approve` của Tiến Cường/Code Owner cho commit mới nhất.
- Không còn conversation chưa resolve.
- `mvn test` và các check bắt buộc đều pass.
- Không có conflict với `main`.

Không tick **Merge without waiting for requirements to be met (bypass rules)** cho PR thông thường.

## 9. Đồng bộ branch với `main` và xử lý conflict

### 9.1. Kiểm tra trước khi đồng bộ

Nếu GitHub báo branch bị tụt sau `main`, trước hết xem thay đổi từ `main`:

```powershell
git fetch origin
git log --oneline HEAD..origin/main
git diff HEAD..origin/main
```

### 9.2. Cẩn thận đưa `main` vào branch đang làm

Chỉ làm khi cần cập nhật branch trước merge hoặc khi Tiến Cường yêu cầu. Lệnh dưới đây đưa thay đổi vào vùng kiểm tra trước, chưa tạo commit ngay:

```powershell
git merge --no-commit --no-ff origin/main
git diff --cached
```

- Nếu thay đổi sau merge phù hợp: chạy `mvn test`, sau đó commit merge và push.
- Nếu không muốn tiếp tục: chạy `git merge --abort` để quay về đúng trạng thái trước lệnh merge.
- Nếu có conflict: sửa các đoạn có `<<<<<<<`, `=======`, `>>>>>>>`; chạy test; `git add` các file đã sửa; rồi `git commit` và `git push`.

`--no-commit` cho phép xem toàn bộ kết quả merge trước khi chốt. Git có thể tự ghép những dòng không đè nhau, nhưng `git diff --cached` vẫn hiển thị toàn bộ phần thay đổi để người làm và Tiến Cường kiểm tra.

## 10. Sau khi PR được merge

Người làm cập nhật máy và xóa branch local đã xong:

```powershell
git switch main
git pull origin main
git branch -d feature/familytree-labeling
```

Trên GitHub có thể chọn **Delete branch** sau khi merge. Không dùng `git branch -D` trừ khi Tiến Cường xác nhận branch không còn thay đổi cần giữ.

## 11. Khi cần thay đổi model hoặc hợp đồng chung

Ví dụ cần thêm field vào `FamilyTreeResult`, đổi `RelationLabel`, đổi package hoặc đổi chữ ký hàm.

1. Báo trong Discord: vấn đề, lý do và những nhóm bị ảnh hưởng.
2. Tạo branch riêng, ví dụ `refactor/model-contract-result`.
3. Cập nhật tài liệu trong `docs/contracts/`, model Java và test hợp đồng.
4. Không đồng thời nhét thuật toán mới vào PR hợp đồng.
5. Tiến Cường review; sau khi merge, các branch thuật toán đang mở phải cập nhật `main` trước khi tiếp tục.

## 12. Checklist ngắn trước khi gửi review

```text
[ ] Tôi đang ở branch riêng, không phải main.
[ ] Tôi chỉ thay đổi đúng vùng công việc.
[ ] Tôi đã đọc hợp đồng input/output liên quan.
[ ] Tôi đã thêm hoặc cập nhật test.
[ ] mvn test pass.
[ ] git diff và git diff --cached không có file lạ.
[ ] Commit message đúng quy ước.
[ ] PR mô tả rõ nhận gì, trả gì và đã test gì.
[ ] Tôi đã yêu cầu Tiến Cường review.
```
