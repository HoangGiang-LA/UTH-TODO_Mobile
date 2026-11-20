# UTH Todo

Ứng dụng Android Kotlin + Jetpack Compose, phát triển theo từng bài tập môn Lập trình di động.

| Mốc | Kết quả |
|---|---|
| BTVN 01 | Splash/onboarding/Navigation + OOP; commit `c4a0623`, người dùng đã thử |
| BTVN 02 | Google/email/Guest/Profile; commit `4f3002e`, người dùng xác nhận chạy được |
| BTVN 03 | RTDB hồ sơ, FCM, Analytics, Crashlytics, Remote Config; commit `028f378`, người dùng đã build/tự kiểm thử |
| BTVN 04 | Home UI/API, tiến độ/ngày; commit `1fbfbf0`, người dùng đã build/chạy thử |
| Danh sách | Commit `4d0d14e`, người dùng xác nhận đã test/push |
| Chi tiết | Commit `b27f9d3`, người dùng xác nhận đã test/push |
| Tạo mới/API | Commit nguồn `f099f0c`; người dùng phát hiện tạo “Lau nha” nhận công việc khác, chưa nghiệm thu lưu qua API |
| Lưu công việc trên thiết bị | Đã triển khai CRUD lưu bền và lựa chọn dữ liệu mẫu riêng; test file thật đạt, chờ build/chạy trên thiết bị |

Xem [báo cáo lưu công việc](docs/TASK-PERSISTENCE-REPORT.md), [lộ trình nhánh](docs/BRANCH-ROADMAP.md), báo cáo [Tạo mới](docs/TASK-CREATE-REPORT.md), [Chi tiết](docs/TASK-DETAIL-REPORT.md), [Danh sách](docs/TASK-LIST-REPORT.md), [BTVN 04](docs/BTVN04-REPORT.md), [01](docs/BTVN01-REPORT.md), [02](docs/BTVN02-REPORT.md), [03](docs/BTVN03-REPORT.md).

## Chạy bản hiện tại

Tiếp tục trên `feature/task-create-api`, base `f099f0c`. Agent chưa stage/commit/push/chuyển nhánh.

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

Sync/Run. Mặc định Home/Danh sách dùng **Công việc trên thiết bị**, ban đầu rỗng. Tạo “Lau nha” → mở Chi tiết → tải lại → đóng/mở lại app; dữ liệu cần giữ đúng. Không cần token mock hoặc mạng để dùng CRUD trên thiết bị.

## Hai lựa chọn dữ liệu

Vào **Hồ sơ → Chọn dữ liệu công việc**. Đổi lựa chọn trở về Home; công việc đã lưu vẫn được giữ. Nếu có bản nháp chưa lưu, ứng dụng hỏi trước khi bỏ.

| Lựa chọn | Lưu và đọc | Phạm vi |
|---|---|---|
| Công việc trên thiết bị (mặc định) | DataStore giao dịch, chỉ cập nhật UI sau khi ghi thành công | Riêng Guest hoặc từng Firebase UID trên máy hiện tại; chưa đồng bộ task lên cloud |
| Dữ liệu mẫu của bài tập | Repository HTTP GET/POST/PATCH/DELETE hiện có | Mock chung theo token của đề; chưa xác minh lưu lâu dài/POST echo đúng |

Cả Home, Danh sách, Chi tiết và Tạo mới ghi rõ lựa chọn hiện tại. Không trộn mẫu API vào công việc đã lưu trên máy và không âm thầm đổi sang lưu cục bộ khi request API lỗi. Chế độ thiết bị không gửi HTTP; chế độ mẫu vẫn gọi API thật.

Task trên thiết bị dùng ID do kho lưu cấp trong cùng giao dịch với dữ liệu; counter được lưu bền và không tái dùng ID vừa xóa. ID API tiếp tục phải do server trả. Đổi nguồn không dùng ID của một nguồn để sửa task cùng số ID ở nguồn khác.

Đăng xuất/đổi tài khoản xóa state trên màn hình và hủy request cũ; dữ liệu đã ghi trên máy được giữ cho lần dùng lại đúng Guest/UID. Không tự chuyển task Guest sang tài khoản đăng nhập. Xóa dữ liệu app/gỡ cài đặt có thể làm mất dữ liệu trên thiết bị; không cần thao tác này để chạy bản cập nhật.

## Các flow

- Home dùng tên hồ sơ đã đồng bộ, chọn ngày, tiến độ theo ngày, thẻ đếm Học tập/Cuộc sống.
- Xem tất cả/thanh Danh sách/thẻ nhóm mở List, tìm kiếm không dấu, lọc nhóm, chia Cần làm/Đã xong.
- List giữ tìm kiếm/bộ lọc khi mở Chi tiết/Hồ sơ rồi Back.
- Chi tiết đọc theo ID từ kho đang chọn, sửa thông tin và việc nhỏ, hoàn thành, xóa có xác nhận.
- Nút + mở form tạo mới, nhận ngày từ Home hoặc nhóm từ List. Lỗi giữ draft; không báo thành công trước khi nguồn dữ liệu xác nhận.
- Lỗi dữ liệu/file lưu không bị biến thành danh sách rỗng và không tự xóa file để xử lý.
- remind được lưu cùng task; lập lịch thông báo theo ngày/giờ chưa triển khai. Chuông mở quyền FCM hiện có.

## API bài tập

Token mock nằm trong file bị Git bỏ qua `api.local.properties`. Máy khác sao chép template `api.local.properties.example`, điền `MOCK_API_TOKEN`, Sync/build; biến `UTH_MOCK_API_TOKEN` được ưu tiên. Token mock độc lập với Firebase ID token/service account và được biên dịch vào APK.

| Endpoint | Vai trò |
|---|---|
| GET `https://share.tryapps.store/mock/teach/tasks` | Mảng công việc |
| GET `https://share.tryapps.store/mock/teach/tasks/{id}` | Object công việc, kiểm tra ID |
| POST `https://share.tryapps.store/mock/teach/tasks` | Tạo mới, không gửi ID; kiểm tra response khớp draft |
| PATCH `https://share.tryapps.store/mock/teach/tasks/{id}` | done, thông tin hoặc subtasks; chờ ACK 2xx |
| DELETE `https://share.tryapps.store/mock/teach/tasks/{id}` | Xóa sau xác nhận/ACK |

Prototype/live API không truy cập được trong môi trường agent. Không coi nguồn lưu trên thiết bị là đã sửa hợp đồng POST hoặc đã chứng minh server lưu dữ liệu. Mẫu ngày cố định vẫn giữ nguyên ngày; không tự gán thành hôm nay.

## Firebase

Dùng `app/google-services.json` của Android app `com.example.uth_todo`. Auth/Profile vẫn theo BTVN 02/03; RTDB lưu tên ở `users/{uid}/profile`. FCM topic `uth_todo_updates`, Remote Config banner, Analytics route/profile_saved và Crashlytics giữ nguyên.

Không đổi Firebase rules hoặc tạo kho task RTDB trong mốc này. Chưa có đồng bộ task giữa thiết bị; Spark và cấu hình đang hoạt động được giữ nguyên.

## Kiểm tra

| Kiểm tra trong môi trường agent | Kết quả |
|---|---|
| Kotlin 2.2.10/JUnit chạy riêng với thư viện thực có sẵn | 114/114 đạt: toàn bộ test JUnit hiện có, gồm 13 test lưu bền/nguồn dữ liệu |
| File DataStore thật | Đạt tạo/đóng/mở lại, nhiều lần sửa/xóa, 20 tạo đồng thời; nhóm 13 test đạt 25 lượt liên tiếp sau sửa lỗi Windows |
| Biên dịch 61 source Kotlin/Compose với header R/BuildConfig kiểm tra nguồn | Đạt; chưa phải APK/Gradle |
| Mã Android test | 36 test biên dịch đạt, gồm 4 test mới cho lưu thiết bị/chuyển nguồn; chưa chạy trên thiết bị |
| AAPT2 compile resources và git diff --check | Đạt |
| assembleDebug/testDebugUnitTest/lintDebug qua wrapper | Bị chặn quyền mở file khóa wrapper, chưa chạy task |
| UI thật/connectedDebugAndroidTest/live API | Chưa chạy trong môi trường agent |

Đã tái hiện lỗi AccessDeniedException khi Windows thay file preferences trong test đồng thời; DeviceTaskStorage dùng Mutex để xếp hàng cả đọc/mode/ghi, giữ nguyên test 20 create. TaskApiFailure giữ cause để báo cáo lỗi có nguyên nhân gốc. Cảnh báo Unsafe không phải nguyên nhân dừng build trong báo cáo này.

Còn cảnh báo deprecation createComposeRule có sẵn. Kết quả test nguồn/lưu file không thay cho nghiệm thu UI trên thiết bị.

## Cấu trúc

Đường dẫn Kotlin tính từ `app/src/main/java/com/example/uth_todo/`.

| Thành phần | Trách nhiệm |
|---|---|
| `MainActivity`, `navigation/` | Khởi tạo repository, bind phiên, điều hướng và đổi nguồn |
| `model/` | Session, task/draft/validation, lựa chọn dữ liệu |
| `data/tasks/` | DataStore, factory một instance/file, phân vùng phiên, repository API, tìm kiếm/ngày/thống kê |
| `data/auth/`, `data/preferences/` | Firebase Auth/Credentials và onboarding |
| `data/firebase/`, `notifications/` | Hồ sơ, Remote Config/telemetry và FCM |
| `ui/home/` | State dùng chung task, điều phối đọc/ghi và đổi nguồn |
| `ui/tasklist/`, `ui/taskdetail/`, `ui/taskcreate/` | Các flow công việc |
| `ui/profile/`, `ui/components/` | Hồ sơ, chọn dữ liệu, thông báo quyền, form và thành phần chung |
| `ui/onboarding/`, `ui/splash/`, `ui/welcome/`, `ui/auth/`, `ui/theme/` | Khởi động/đăng nhập/theme |

Giữ SDK 37/minSdk 24, AGP 9.4.1, Gradle 9.6.0, Kotlin 2.2.10, Java source/target 11, Firebase BoM và cấu hình máy. Không thêm dependency. Không stage token/config Firebase/local.properties/cấu hình IDE/công cụ/cache.
