# Lộ trình nhánh UTH Todo

Cập nhật 20/11/2026. Người dùng thực hiện Git ghi; mỗi mốc có kiểm tra và commit riêng.

| Mốc | Nhánh | Trạng thái |
|---|---|---|
| BTVN 01 | `feature/btvn01-navigation-oop` | `c4a0623` |
| BTVN 02 | `feature/btvn02-firebase-auth` | `4f3002e`, người dùng xác nhận chạy được |
| BTVN 03 | `feature/btvn03-firebase-services` | `028f378`, đã build/tự kiểm thử theo người dùng |
| BTVN 04 | `feature/btvn04-home-api` | `1fbfbf0`, đã build/chạy thử theo người dùng |
| Danh sách | `feature/task-list-api` | `4d0d14e` |
| Chi tiết | `feature/task-detail-api` | `b27f9d3` |
| Tạo mới/API | `feature/task-create-api` | `f099f0c`, đã sửa chặn response khác nội dung |
| Lưu thiết bị | `feature/task-create-api` | `59457c8`, lưu bền/tách dữ liệu mẫu/sửa lỗi ghi đồng thời |
| Nhắc công việc | `feature/task-reminders` dự kiến | Đã triển khai; 137 unit test riêng đạt; quyền/chuông/reboot cần thử thiết bị |
| Cài đặt giao diện | Chưa tạo | Mốc tiếp theo sau nghiệm thu nhắc giờ |
| BTVN 05 | Chưa đặt | Không tự gán khi đề chưa mô tả |

## Mốc đang làm

Base `59457c8`. Nhánh vẫn là `feature/task-create-api`, thay đổi nhắc giờ chưa stage/commit. Agent không tạo/chuyển nhánh, stage, commit, push hoặc reset dữ liệu app/emulator.

Người dùng tạo nhánh và build:

```powershell
git switch -c feature/task-reminders
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

Nếu nhánh này đã được tạo sau khi tài liệu được viết, không tạo lại.

## Nghiệm thu

Xem [TASK-REMINDERS-REPORT.md](TASK-REMINDERS-REPORT.md). Run → Thông báo → bật quyền/gửi thử → cho phép nhắc đúng giờ → tạo task thiết bị có hạn vài phút tới và remind → đợi → mở thông báo vào đúng Chi tiết → thử hoàn thành/xóa/tắt nhắc/đổi phiên/nguồn.

137 test JUnit chạy riêng đạt, 71 file Kotlin/Compose và mã 45 Android test biên dịch đạt, resources đạt. Wrapper vẫn bị chặn file khóa; không coi typecheck/test engine là đã thử AlarmManager, quyền hoặc delivery trên Android.

## Stage, commit, push sau khi chạy đạt

```powershell
git add README.md docs/BRANCH-ROADMAP.md docs/TASK-PERSISTENCE-REPORT.md docs/TASK-REMINDERS-REPORT.md
git add app/src/main app/src/androidTest
git add app/src/test/java/com/example/uth_todo/TaskReminderTest.kt app/src/test/java/com/example/uth_todo/TaskCreateTest.kt app/src/test/java/com/example/uth_todo/TaskPersistenceTest.kt
git diff --cached --stat
git diff --cached
git commit -m "feat: add scheduled task reminders and notification screen"
git push -u origin feature/task-reminders
```

Không stage token, Firebase config, local.properties, cấu hình IDE/công cụ hoặc cache. Các lệnh trên là hướng dẫn, chưa được agent thực thi.

## Tiếp theo

Cài đặt giao diện sáng/tối và đối chiếu những màn hình còn lại. Task cloud sync và tính lưu lâu dài của server mock vẫn chưa được triển khai/xác minh; không tự coi toàn bộ app đã hoàn tất.
