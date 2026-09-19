# Quy trình làm việc, Commit và Pull Request

Tài liệu này là quy ước làm việc chung của project đồ thị gia phả. Mục tiêu là để mọi thay đổi đều có người chịu trách nhiệm, có test đi kèm và được Tiến Cường xem trước khi đi vào `main`.

## 1. Quy tắc bắt buộc

- Không ai `push` hoặc commit trực tiếp vào branch `main`.
- Mỗi đầu việc được làm trên **một branch riêng** và kết thúc bằng **một Pull Request (PR)** vào `main`.
- Không merge PR khi chưa có review `Approve` của Tiến Cường, với vai trò Code Owner.
- Nếu muốn sửa `model.*`, thì báo cho Cường nhé.

## Quy trình làm việc

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

Nhóm 1 và nhóm 2 chỉ dùng `NormalizedInput` hợp lệ.

## 4. Trong khi làm việc

### 4.1. Không làm hỏng phần của người khác

- Chỉ sửa file thuộc vùng công việc của mình, trừ khi PR đã thống nhất cần sửa chỗ dùng chung.
- Không đổi tên, format toàn bộ project hoặc xóa file không liên quan chỉ để "dọn code".
- Không đổi tên folder, di chuyển folder

### 4.2. Test sau khi code xong

- Test ví dụ chung sẽ để ở `examples/valid/` và `examples/invalid/`. Mọi người có thể vào lấy test ví dụ và sau đó nhờ AI viết Unit test Java dựa theo để check code có đúng hay không.
- Mọi ngươi viết xong cần trace bằng tay được thuật toán theo một test ví dụ nào đấy. 

### 4.3. Kiểm tra trước khi commit

```powershell
git status
git diff
mvn test
```

`git diff` giúp xem chính xác mình đã sửa gì trước khi đưa vào commit. Nếu `mvn test` lỗi, không tạo PR với trạng thái lỗi trừ khi PR chỉ để nhờ hỗ trợ và phải ghi rõ lý do trong mô tả.

## 5. Đồng bộ branch với `main` và xử lý conflict

### 5.1. Kiểm tra trước khi đồng bộ

Nếu GitHub báo branch bị tụt sau `main`, trước hết xem thay đổi từ `main`:

```powershell
git fetch origin
git log --oneline HEAD..origin/main
git diff HEAD..origin/main
```

### 5.2. Cẩn thận đưa `main` vào branch đang làm

Chỉ làm khi cần cập nhật branch trước merge hoặc khi Tiến Cường yêu cầu. Lệnh dưới đây đưa thay đổi vào vùng kiểm tra trước, chưa tạo commit ngay:

```powershell
git merge --no-commit origin/main
git diff --cached
```

- Nếu thay đổi sau merge phù hợp: chạy `mvn test`, sau đó commit merge và push.
- Nếu không muốn tiếp tục: chạy `git merge --abort` để quay về đúng trạng thái trước lệnh merge.
- Nếu có conflict: sửa các đoạn có `<<<<<<<`, `=======`, `>>>>>>>`; chạy test; `git add` các file đã sửa; rồi `git commit` và `git push`.

`--no-commit` cho phép xem toàn bộ kết quả merge trước khi chốt. Git có thể tự ghép những dòng không đè nhau, nhưng `git diff --cached` vẫn hiển thị toàn bộ phần thay đổi để người làm và Tiến Cường kiểm tra.