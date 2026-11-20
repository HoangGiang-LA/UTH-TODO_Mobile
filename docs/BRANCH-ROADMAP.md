# Lộ trình nhánh UTH Todo

Cập nhật 20/11/2026. Người dùng thực hiện mọi thao tác Git làm thay đổi trạng thái; mỗi mốc có kiểm tra và commit riêng.

| Mốc | Nhánh | Trạng thái |
|---|---|---|
| BTVN 01 | `feature/btvn01-navigation-oop` | Commit `c4a0623`, người dùng đã thử |
| BTVN 02 | `feature/btvn02-firebase-auth` | Commit `4f3002e`, người dùng xác nhận chạy được |
| BTVN 03 | `feature/btvn03-firebase-services` | Commit `028f378`, người dùng đã build/tự kiểm thử thành công |
| BTVN 04 | `feature/btvn04-home-api` | Commit `1fbfbf0`, người dùng đã build/chạy thử thành công |
| Flow danh sách | `feature/task-list-api` | Commit `4d0d14e`, đã test/push theo xác nhận người dùng |
| Flow chi tiết | `feature/task-detail-api` | Commit `b27f9d3`, đã test/push theo xác nhận người dùng |
| Thêm công việc | `feature/task-create-api` | Chưa đạt nghiệm thu: tạo “Lau nha” nhận task khác; đã sửa đối chiếu response, còn cần xác minh POST lưu thật |
| Thông báo/Cài đặt | Chưa tạo | Tiếp tục hoàn thiện nhắc theo giờ và đối chiếu các màn hình còn lại |
| BTVN 05 | Chưa đặt | Đề chưa đủ mô tả, không tự gán tính năng |

## Nhánh hiện tại và build

Base là Chi tiết `b27f9d3`. Mã tạo mới hiện nằm trong working tree trên `feature/task-create-api`, chưa stage/commit. Agent không tạo/chuyển nhánh, stage, commit, push hoặc merge.

Nhánh tạo mới đã được người dùng tạo. Chạy tại gốc dự án:

```powershell
git status
git branch --show-current
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

Run rồi kiểm tra theo [TASK-CREATE-REPORT.md](TASK-CREATE-REPORT.md). Token/config đang dùng được giữ nguyên. Máy khác tạo `api.local.properties` từ template hoặc dùng `UTH_MOCK_API_TOKEN`.

Trong môi trường agent: 92 JUnit test riêng đạt, 54 source Kotlin/Compose và mã 32 Android test biên dịch đạt, resources đạt. Wrapper bị chặn mở file khóa; APK/lint, Android test trên thiết bị và live POST chưa được xác nhận.

POST hiện dùng đường dẫn collection/schema công việc đang có. Prototype bị DNS nên chưa xác minh được hợp đồng POST trực tiếp; cần kiểm tra response thực, không coi fixture/test là live API.

## Stage, commit và push sau khi chạy đạt

```powershell
git branch --show-current
git add README.md docs/BRANCH-ROADMAP.md docs/TASK-DETAIL-REPORT.md docs/TASK-CREATE-REPORT.md
git add app/src/main app/src/androidTest
git add app/src/test/java/com/example/uth_todo/HomeViewModelTest.kt app/src/test/java/com/example/uth_todo/TaskDetailTest.kt app/src/test/java/com/example/uth_todo/TaskCreateTest.kt
git diff --cached --stat
git diff --cached
git commit -m "feat: add task creation flow"
git push -u origin feature/task-create-api
```

Đây là hướng dẫn cho người dùng, agent chưa thực thi thao tác Git ghi. Không stage token, Firebase config, local.properties, cấu hình IDE/công cụ hoặc cache.

## Phần tiếp theo

Chưa chuyển mốc khi việc tạo “Lau nha” chưa được lưu/đọc lại đúng. Sau khi chốt Thêm công việc, đối chiếu Thông báo/nhắc việc theo giờ và Cài đặt để tiếp tục hoàn thiện app. remind hiện là tùy chọn được lưu, chưa lập lịch thông báo theo giờ. Chưa coi toàn bộ app đã hoàn tất.

Nếu main đã merge mốc trước, người dùng tạo nhánh mới từ main đã cập nhật; nếu chưa merge, kế thừa commit mốc trước đã nghiệm thu và chọn base PR tương ứng.
