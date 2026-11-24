# Lộ trình UTH Todo

Cập nhật 24/11/2026. Người dùng thực hiện Git ghi; mỗi mốc có kiểm tra và commit riêng.

| Mốc | Nhánh | Trạng thái |
|---|---|---|
| BTVN 01 | `feature/btvn01-navigation-oop` | `c4a0623` |
| BTVN 02 | `feature/btvn02-firebase-auth` | `4f3002e`, người dùng xác nhận chạy được |
| BTVN 03 | `feature/btvn03-firebase-services` | `028f378`, người dùng đã build/tự kiểm thử |
| BTVN 04 | `feature/btvn04-home-api` | `1fbfbf0`, người dùng đã build/chạy thử |
| Danh sách | `feature/task-list-api` | `4d0d14e` |
| Chi tiết | `feature/task-detail-api` | `b27f9d3` |
| Tạo mới/API | `feature/task-create-api` | `f099f0c`, chặn phản hồi khác nội dung |
| Lưu thiết bị | `feature/task-create-api` | `59457c8`, lưu bền/tách mẫu/sửa ghi đồng thời |
| Nhắc công việc | `feature/task-reminders` | `c8eece6`; quyền/chuông/khôi phục cần kiểm tra thiết bị |
| Cài đặt giao diện | `feature/appearance-finalization` gợi ý | Đã triển khai; 148 unit test riêng đạt, cần build/Run nghiệm thu |
| Rà soát và bàn giao | Cùng mốc giao diện | Đã chuẩn bị tài liệu, checklist và hướng dẫn APK; chưa đánh dấu các ca thử thiết bị là đạt |
| BTVN 05 | Chưa xác định | Chưa có đề chi tiết để đối chiếu |

## Mốc hiện tại

Base `c8eece6`. Đang ở `feature/task-reminders`; thay đổi mới chưa stage/commit. Agent không tạo/chuyển nhánh, stage, commit, push hoặc reset dữ liệu app/emulator.

Hồ sơ → Giao diện có ba lựa chọn Theo hệ thống/Sáng/Tối, lưu trên thiết bị và áp dụng toàn app. Các thẻ/nội dung/thanh hệ thống được cập nhật cho theme tối. Xem [APPEARANCE-REPORT.md](APPEARANCE-REPORT.md).

Đã rà mã các luồng khởi động/phiên, CRUD, chọn nguồn, nhắc việc và theme. Nguồn app không có TODO/NotImplementedError. Điều này không thay cho so sánh giao diện và chạy thực tế.

## Các bước để chốt bản demo

1. Chạy `.\gradlew.bat assembleDebug testDebugUnitTest lintDebug` và xác nhận BUILD SUCCESSFUL.
2. Run bản vừa build, thử theme, dữ liệu công việc và nhắc việc theo [PROJECT-HANDOFF.md](PROJECT-HANDOFF.md).
3. Chạy UI tests nếu có thiết bị; ghi lại kết quả các ca thủ công. APK debug ở `app/build/outputs/apk/debug/app-debug.apk` sau build thành công.
4. Stage/commit/push thay đổi theo lệnh bên dưới; nếu bài nộp cần `main`, review và merge Pull Request sau khi kiểm thử đạt.

## Kiểm chứng trong môi trường agent

- JUnit chạy riêng: 148/148 đạt (137 hồi quy + 11 giao diện).
- Biên dịch 75 file Kotlin/Compose và 52 Android test với compile dependencies đúng từ Gradle model: đạt; chưa tạo APK hoặc chạy UI tests.
- AAPT2 compile tài nguyên và kiểm tra diff: đạt.
- Lệnh Gradle đầy đủ bị chặn trước task bởi quyền mở file khóa wrapper. Chưa xác nhận build/lint sau thay đổi trên máy người dùng.

Không thay phiên bản, đường dẫn máy hoặc xóa cache/khóa để xử lý giới hạn quyền.

## Stage, commit, push sau khi kiểm thử đạt

```powershell
git switch -c feature/appearance-finalization
git add app/src/main app/src/test app/src/androidTest
git add README.md docs/BRANCH-ROADMAP.md docs/APPEARANCE-REPORT.md docs/PROJECT-HANDOFF.md
git diff --cached --stat
git diff --cached
git commit -m "feat: persist appearance settings and prepare project handoff"
git push -u origin feature/appearance-finalization
```

Không tạo lại nhánh nếu đã tạo. Không stage token, Firebase config, local.properties, cấu hình IDE/công cụ hoặc cache. Các lệnh trên là hướng dẫn, chưa được agent thực thi.

## Giới hạn của bản chốt

Task vẫn lưu trên từng thiết bị; chưa đồng bộ cloud giữa máy A và B. API bài tập là nguồn mẫu riêng, chưa chứng minh lưu lâu dài. Bản debug chưa phải bản release có chữ ký phát hành. Chỉ chốt bản demo sau khi checklist trong tài liệu bàn giao đã được người dùng kiểm tra; không coi các phần chưa có yêu cầu hoặc chưa chạy là hoàn tất.
