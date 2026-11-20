# Lộ trình nhánh UTH Todo

Cập nhật 20/11/2026. Người dùng thực hiện mọi thao tác Git ghi; mỗi mốc được kiểm tra và commit riêng.

| Mốc | Nhánh | Trạng thái |
|---|---|---|
| BTVN 01 | `feature/btvn01-navigation-oop` | `c4a0623`, đã thử |
| BTVN 02 | `feature/btvn02-firebase-auth` | `4f3002e`, người dùng xác nhận chạy được |
| BTVN 03 | `feature/btvn03-firebase-services` | `028f378`, đã build/tự kiểm thử |
| BTVN 04 | `feature/btvn04-home-api` | `1fbfbf0`, đã build/chạy thử |
| Danh sách | `feature/task-list-api` | `4d0d14e`, đã test/push theo người dùng |
| Chi tiết | `feature/task-detail-api` | `b27f9d3`, đã test/push theo người dùng |
| Tạo mới/API | `feature/task-create-api` | `f099f0c`; source đã commit, tạo “Lau nha” nhận task khác nên chưa nghiệm thu lưu API |
| Lưu công việc thật trên thiết bị | Cùng `feature/task-create-api` | Đã triển khai; 114 unit test riêng đạt, có file thật; chờ build/chạy UI |
| Thông báo/Cài đặt | Chưa tạo | Tiếp tục sau nghiệm thu lưu công việc |
| BTVN 05 | Chưa đặt | Không tự gán khi đề chưa mô tả |

## Mốc đang làm

Base hiện tại `f099f0c`, thay đổi mới trong working tree, chưa stage/commit. Chế độ mặc định lưu CRUD trên thiết bị, theo Guest/UID; API mẫu giữ nguyên ở lựa chọn riêng trong Hồ sơ. Không coi lưu cục bộ là đã sửa hoặc xác minh khả năng lưu server mock.

Agent không tạo/chuyển nhánh, stage, commit, push, merge hoặc reset app/emulator.

## Build và nghiệm thu

```powershell
git branch --show-current
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

Run → chọn Công việc trên thiết bị nếu cần → tạo “Lau nha” → tải lại/mở Chi tiết → đóng/mở app → sửa/tick/xóa và tải lại. Chuyển sang dữ liệu mẫu rồi trở lại phải còn dữ liệu đã lưu.

Xem [TASK-PERSISTENCE-REPORT.md](TASK-PERSISTENCE-REPORT.md). Kết quả hiện tại: 114 JUnit test riêng, 61 file Kotlin/Compose, mã 36 Android test và resources đạt kiểm tra; wrapper bị chặn file khóa, UI Android chưa chạy.

Đã sửa lỗi Windows trong test tạo đồng thời: serialize đọc/ghi bằng Mutex, không bỏ test. Nhóm 13 test lưu bền đạt 25 lượt liên tiếp; toàn bộ 114 test chạy riêng đạt. Gradle vẫn cần chạy lại trên máy người dùng.

## Stage, commit và push sau khi chạy đạt

```powershell
git add README.md docs/BRANCH-ROADMAP.md docs/TASK-CREATE-REPORT.md docs/TASK-PERSISTENCE-REPORT.md
git add app/src/main app/src/androidTest
git add app/src/test/java/com/example/uth_todo/TaskPersistenceTest.kt
git diff --cached --stat
git diff --cached
git commit -m "fix: persist tasks and serialize device storage access"
git push -u origin feature/task-create-api
```

Không stage token, Firebase config, local.properties, cấu hình IDE/công cụ hoặc cache. Các lệnh trên là hướng dẫn; agent chưa thực thi.

## Tiếp theo

Sau khi lưu thiết bị được người dùng nghiệm thu, tiếp tục Thông báo/nhắc theo giờ và Cài đặt còn lại. Đồng bộ task lên RTDB/giữa thiết bị là phạm vi chưa triển khai; không tự khẳng định đã có vì app đã dùng Firebase Auth/hồ sơ.

Hợp đồng POST và tính lưu lâu dài của mock vẫn chưa xác minh. Giữ API mẫu để kiểm tra bài tập, không trộn kết quả mẫu vào dữ liệu cá nhân.
