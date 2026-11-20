# Lộ trình nhánh UTH Todo

Cập nhật 20/11/2026. Mỗi mốc được kiểm thử và commit riêng. Người dùng thực hiện mọi thao tác Git làm thay đổi trạng thái.

| Mốc | Nhánh | Trạng thái |
|---|---|---|
| BTVN 01 | `feature/btvn01-navigation-oop` | Commit `c4a0623`, người dùng đã thử |
| BTVN 02 | `feature/btvn02-firebase-auth` | Commit `4f3002e`, người dùng xác nhận Google/email chạy được |
| BTVN 03 | `feature/btvn03-firebase-services` | Commit `028f378`, người dùng đã build/tự kiểm thử thành công |
| BTVN 04 | `feature/btvn04-home-api` | Commit `1fbfbf0`, người dùng đã build/chạy thử thành công |
| Flow danh sách | `feature/task-list-api` dự kiến | Màn hình riêng và điều hướng đã triển khai; 57 unit test riêng đạt, chờ build Gradle và thử thiết bị |
| Flow chi tiết | `feature/task-detail-api` dự kiến | Tiếp theo: GET theo ID, sửa/xóa theo hợp đồng API; hiện chỉ có hộp xem nhanh dữ liệu list |
| BTVN 05 | Chưa đặt | Đề chưa đủ mô tả, không tự gán tính năng |

Flow danh sách đối chiếu ảnh màn hình “Danh sách” trong file đề cục bộ. Gọi GET/PATCH từ repository BTVN 04; tìm kiếm/lọc phía ứng dụng. Không thêm endpoint chưa có trong đề.

## Nhánh hiện tại

Base là BTVN 04 ở `1fbfbf0`. Khi triển khai, nhánh vẫn là `feature/btvn04-home-api`; thay đổi danh sách chưa stage/commit. Agent không tạo/chuyển nhánh, stage, commit, push hoặc merge.

Người dùng tự chạy trước khi chốt mốc:

```powershell
git status
git branch --show-current
git switch -c feature/task-list-api
```

Nhánh mới kế thừa Home đã nghiệm thu và mang theo thay đổi working tree. Không reset/clean hoặc tạo lại nếu nhánh này đã được tạo sau khi tài liệu được viết.

## Build và nghiệm thu

Token mock nằm trong `api.local.properties` bị Git bỏ qua. Máy khác cần tạo file từ template hoặc cung cấp biến `UTH_MOCK_API_TOKEN`. Giữ cấu hình Firebase của máy đang hoạt động.

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

Sau đó Run và thực hiện checklist trong [TASK-LIST-REPORT.md](TASK-LIST-REPORT.md): mở từ mọi điểm vào Home, tìm kiếm/lọc, hoàn thành công việc, kiểm tra Home đồng bộ, Back/Hồ sơ và phiên đăng nhập.

Trong môi trường agent, wrapper bị chặn quyền mở file khóa trước khi chạy task. 57 test JUnit riêng, 49 file Kotlin/Compose, mã Android test và resources đã kiểm tra đạt; không thay thế kết quả Gradle hoặc thử trên thiết bị.

## Commit/push sau khi chạy đạt

Các lệnh dưới đây để người dùng thực hiện sau khi xem diff và nghiệm thu:

```powershell
git branch --show-current
git diff --stat
git diff
git ls-files --others --exclude-standard
git add README.md docs/BRANCH-ROADMAP.md docs/TASK-LIST-REPORT.md
git add app/src/main app/src/androidTest app/src/test/java/com/example/uth_todo/TaskListViewModelTest.kt
git diff --cached --stat
git diff --cached
git commit -m "feat: implement task list screen and navigation"
git push -u origin feature/task-list-api
```

Không stage cấu hình IDE/công cụ, config Firebase, token cục bộ hoặc cache. Đây là hướng dẫn, agent chưa thực thi các thao tác ghi Git.

## Mốc sau

- Hoàn thiện flow chi tiết sau khi danh sách được người dùng build/chạy đạt và chốt commit.
- Dùng GET theo ID và xác nhận API khi sửa/xóa; không báo lưu/xóa thành công trước ACK.
- Nếu main đã merge mốc trước, người dùng tạo nhánh mới từ main đã cập nhật. Nếu chưa merge, nhánh tiếp theo kế thừa commit mốc trước đã nghiệm thu và chọn base PR phù hợp.
- Phần thêm mới và CRUD đầy đủ chỉ triển khai theo đề/hợp đồng API được xác minh.
