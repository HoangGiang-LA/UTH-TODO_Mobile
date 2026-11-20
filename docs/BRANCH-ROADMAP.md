# Lộ trình nhánh UTH Todo

Cập nhật 20/11/2026. Mỗi mốc được kiểm thử và commit riêng. Người dùng thực hiện mọi thao tác Git làm thay đổi trạng thái.

| Mốc | Nhánh | Trạng thái |
|---|---|---|
| BTVN 01 | `feature/btvn01-navigation-oop` | Commit `c4a0623`, người dùng đã thử |
| BTVN 02 | `feature/btvn02-firebase-auth` | Commit `4f3002e`, người dùng xác nhận chạy được |
| BTVN 03 | `feature/btvn03-firebase-services` | Commit `028f378`, người dùng đã build/tự kiểm thử thành công |
| BTVN 04 | `feature/btvn04-home-api` | Commit `1fbfbf0`, người dùng đã build/chạy thử thành công |
| Flow danh sách | `feature/task-list-api` | Commit `4d0d14e`, người dùng xác nhận đã test và push hoàn tất |
| Flow chi tiết | `feature/task-detail-api` dự kiến | Đã triển khai GET theo ID, sửa, việc nhỏ, hoàn thành và xóa; chờ build/chạy thử thiết bị |
| Thêm công việc | Chưa tạo | Mốc tiếp theo; cần đối chiếu POST và màn hình theo đề |
| BTVN 05 | Chưa đặt | Đề chưa đủ mô tả, không tự gán tính năng |

## Nhánh hiện tại

Base là Danh sách ở `4d0d14e`. Khi triển khai Chi tiết, nhánh vẫn là `feature/task-list-api`; thay đổi mới chưa stage/commit. Agent không tạo/chuyển nhánh, stage, commit, push hoặc merge.

Người dùng tự chạy:

```powershell
git status
git switch -c feature/task-detail-api
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

Nhánh mới kế thừa Danh sách đã nghiệm thu và mang theo thay đổi working tree. Không tạo lại nếu đã tạo nhánh này sau khi tài liệu được viết.

## Nghiệm thu Chi tiết

Xem [TASK-DETAIL-REPORT.md](TASK-DETAIL-REPORT.md). Run rồi thử mở từ Home/Danh sách, sửa/lưu, thêm/sửa/bỏ việc nhỏ, hoàn thành, xác nhận xóa, Back giữ từ khóa/bộ lọc, tải lỗi và phiên không còn hợp lệ.

Token/config cục bộ đang hoạt động được giữ nguyên. Máy khác tạo `api.local.properties` từ template hoặc dùng biến `UTH_MOCK_API_TOKEN`.

Trong môi trường agent: 74 JUnit test riêng đạt, 50 source Kotlin/Compose và mã 23 Android test biên dịch đạt, resources đạt. Wrapper bị chặn mở file khóa, chưa xác nhận APK/lint. Android test và live API chưa chạy; cần kiểm tra payload PATCH/DELETE với mock thật.

## Stage, commit và push sau khi chạy đạt

```powershell
git branch --show-current
git add README.md docs/BRANCH-ROADMAP.md docs/TASK-LIST-REPORT.md docs/TASK-DETAIL-REPORT.md
git add app/src/main app/src/androidTest
git add app/src/test/java/com/example/uth_todo/HomeViewModelTest.kt app/src/test/java/com/example/uth_todo/TaskDetailTest.kt
git diff --cached --stat
git diff --cached
git commit -m "feat: implement task detail editing and deletion"
git push -u origin feature/task-detail-api
```

Các lệnh trên để người dùng thực hiện; agent chưa chạy thao tác Git ghi. Không stage token, config Firebase, local.properties, cấu hình IDE/công cụ hoặc cache.

## Sau mốc này

Hoàn thiện Thêm công việc mới theo API POST sau khi Chi tiết được nghiệm thu và chốt commit. Sau đó đối chiếu các màn hình còn lại của đề, gồm nhắc theo giờ và Cài đặt; chưa tự coi toàn bộ app đã hoàn tất.

Nếu main đã merge mốc trước, người dùng tạo nhánh mới từ main đã cập nhật. Nếu chưa merge, nhánh tiếp theo kế thừa commit mốc trước đã nghiệm thu và chọn base PR phù hợp.
