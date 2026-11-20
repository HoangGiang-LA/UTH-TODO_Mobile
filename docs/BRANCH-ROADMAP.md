# Lộ trình nhánh UTH Todo

Cập nhật 20/11/2026. Mỗi mốc có code, nghiệm thu và commit riêng. Căn cứ tài liệu `Khởi tạo App Mobile(Task)(1).docx`; phần BTVN 05 chưa đủ mô tả để tự gán số bài cho các flow sau.

| Mốc | Nhánh | Phạm vi và trạng thái |
|---|---|---|
| BTVN 01 | `feature/btvn01-navigation-oop` | Splash/onboarding; đã commit/push `c4a0623`, người dùng đã thử app |
| BTVN 02 | `feature/btvn02-firebase-auth` | Google/email/Guest/Profile; người dùng đã thử thành công và commit `4f3002e` |
| BTVN 03 | `feature/btvn03-firebase-services` | Người dùng đã build và xác nhận tự kiểm thử thành công, có ảnh profile trên RTDB; 35/35 unit test, lint 0 lỗi/24 warning; sẵn sàng commit/push |
| BTVN 04 | `feature/btvn04-home-api` | Home UI/API: công việc và thống kê; chưa triển khai |
| Flow danh sách | `feature/task-list-api` | Chờ đối chiếu đầy đủ đề/API |
| Flow chi tiết | `feature/task-detail-api` | Chi tiết/hoàn tất/xóa theo hợp đồng API; chưa triển khai |

Đề BTVN 03 chỉ ghi “tích hợp toàn bộ các dịch vụ Firebase”, chưa liệt kê dịch vụ. Phạm vi hiện chọn dịch vụ phù hợp app và dùng được trên Spark; không coi là đã triển khai toàn bộ danh mục Firebase. Storage/Functions không được bật để tránh yêu cầu nâng gói. Chi tiết trong [BTVN03-REPORT.md](BTVN03-REPORT.md).

## Nhánh BTVN 03

Khi bắt đầu mốc này, base là BTVN 02 ở `4f3002e`. Người dùng đã tạo/chọn `feature/btvn03-firebase-services`; thay đổi BTVN 03 vẫn ở working tree, chưa stage. Theo yêu cầu người dùng, agent chỉ đọc Git, không tự tạo/chuyển nhánh, stage, commit, push hoặc merge.

Người dùng chạy:

```powershell
git status
git branch --show-current
```

Nhánh hiện tại kế thừa BTVN 02; không cần tạo lại. Đọc status/lịch sử trước khi thực hiện thao tác Git khác; không reset/clean để loại bỏ thay đổi.

## Nghiệm thu và commit

Cấu hình Console và chạy checklist trong [báo cáo BTVN 03](BTVN03-REPORT.md), sau đó:

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

Agent đã chạy riêng 26 unit test. Sau đó người dùng chạy build BTVN 03 thành công trong 5 phút 50 giây; báo cáo Gradle có 35/35 unit test đạt, lint 0 lỗi/24 warning. Người dùng sau đó xác nhận tự kiểm thử thành công và gửi ảnh profile trên RTDB thật. Database emulator trong môi trường agent vẫn bị chặn loopback; 16 test rules chưa thực thi.

Sau khi nghiệm thu, người dùng tự kiểm tra, stage và commit:

```powershell
git diff --stat
git diff
git ls-files --others --exclude-standard
git add README.md build.gradle.kts gradle/libs.versions.toml app/build.gradle.kts
git add app/src/main app/src/test/java/com/example/uth_todo/FirebaseServicesViewModelTest.kt
git add firebase/database.rules.json firebase/test_database_rules.py docs/BTVN03-REPORT.md docs/BRANCH-ROADMAP.md
git diff --cached --stat
git diff --cached
git commit -m "BTVN 03: Firebase services"
git push -u origin feature/btvn03-firebase-services
```

Đây là hướng dẫn, chưa được agent thực thi. Không stage cấu hình IDE/công cụ, `google-services.json`, `local.properties` hoặc cache/build output.

## Mốc sau

- Sau khi BTVN 03 được nghiệm thu và commit, tiếp tục BTVN 04 Home UI/API.
- Nếu main đã được người dùng merge mốc trước, tạo nhánh mới từ main đã cập nhật.
- Nếu chưa merge main, tạo nhánh kế tiếp từ commit mốc trước đã nghiệm thu; chọn base PR phù hợp để diff chỉ chứa bài mới.
- Không tạo hàng loạt nhánh rỗng hoặc gộp mọi tính năng thành một lần push vào main.
- Token mock API để ở cấu hình cục bộ khi đến mốc API, không đưa vào source/tài liệu Git.
