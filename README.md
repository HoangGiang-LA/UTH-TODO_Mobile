# UTH Todo

Ứng dụng Android Kotlin + Jetpack Compose, phát triển theo từng bài tập môn Lập trình di động.

| Mốc | Kết quả |
|---|---|
| BTVN 01 | Splash/onboarding/Navigation + OOP; commit `c4a0623`, người dùng đã thử app |
| BTVN 02 | Google/email/Guest/Profile; commit `4f3002e`, người dùng xác nhận chạy được |
| BTVN 03 | RTDB, FCM, Analytics, Crashlytics, Remote Config; commit `028f378`, người dùng đã build và tự kiểm thử; Gradle 35/35 unit test, lint 0 lỗi/24 warning |
| BTVN 04 | Home UI/API, tiến độ/ngày và đánh dấu hoàn thành; commit `1fbfbf0`, người dùng đã build/chạy thử thành công |
| Flow danh sách | Commit `4d0d14e`; người dùng xác nhận đã test và push thành công |
| Flow chi tiết | GET theo ID, sửa thông tin/việc nhỏ, hoàn thành và xóa có xác nhận; đã triển khai, chờ build/chạy thử trên thiết bị |

Xem [báo cáo flow chi tiết](docs/TASK-DETAIL-REPORT.md), [báo cáo flow danh sách](docs/TASK-LIST-REPORT.md), [báo cáo BTVN 04](docs/BTVN04-REPORT.md), [lộ trình nhánh](docs/BRANCH-ROADMAP.md), và báo cáo [01](docs/BTVN01-REPORT.md), [02](docs/BTVN02-REPORT.md), [03](docs/BTVN03-REPORT.md).

## Chạy flow chi tiết

Base Danh sách đã được commit ở `4d0d14e`. Mã Chi tiết hiện nằm trong working tree trên `feature/task-list-api`, chưa stage/commit. Người dùng tự tạo nhánh cho mốc này trước khi chốt:

```powershell
git switch -c feature/task-detail-api
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

Sau đó Run trên thiết bị có mạng. Home lấy công việc thật từ endpoint mock trong đề; không dùng fixture test để giả lập dữ liệu thành công khi mất mạng.

Token mock từ đề đã được đặt trong `api.local.properties` trên máy hiện tại và file đã được Git bỏ qua. Khi clone sang máy khác:

1. Sao chép `api.local.properties.example` thành `api.local.properties`.
2. Điền `MOCK_API_TOKEN` bằng token được cấp trong đề, rồi Sync/build lại.
3. Có thể dùng biến môi trường `UTH_MOCK_API_TOKEN` thay file; biến môi trường được ưu tiên.

Token này là thông tin truy cập API mock của môn học, độc lập với phiên Firebase. Không dùng Firebase ID token hoặc service account. Giá trị được biên dịch vào APK để client gửi request, nên đây không phải cách giữ bí mật một khóa máy chủ trong ứng dụng production. Token thật không nằm trong source, template hoặc tài liệu Git.

## Trang chủ và API

- Header dùng tên hồ sơ đã đồng bộ; avatar/Cài đặt mở Hồ sơ hiện có.
- Tiến độ là số việc đã xong/tổng số việc trong ngày đang chọn.
- Hai thẻ Học tập/Cuộc sống đếm mọi việc chưa xong của nhóm, kể cả ngày khác.
- Home hiển thị tối đa 8 việc trong ngày theo giờ. Xem tất cả/thanh Danh sách mở màn hình danh sách riêng với bộ lọc Tất cả; thẻ Học tập/Cuộc sống truyền đúng nhóm ban đầu.
- Danh sách có tìm kiếm tiếng Việt không cần dấu, nút xóa tìm kiếm, lọc nhóm, chia Cần làm/Đã xong và ngày/giờ của từng việc. Số việc cần làm phản ánh kết quả lọc/tìm kiếm đang hiển thị.
- Tìm kiếm/bộ lọc lưu bằng SavedStateHandle theo lần mở màn hình; vị trí cuộn dùng LazyListState. Đi Hồ sơ rồi Back giữ trạng thái danh sách. Back/Home trở về Home; mở danh sách lần mới áp dụng nhóm được chọn từ Home.
- Home, Danh sách và Chi tiết dùng chung HomeViewModel. Mở Chi tiết sẽ GET theo ID; không dùng dữ liệu cache để giả thành công khi GET lỗi. Quay lại sau khi tải/sửa/xóa thành công giữ kết quả vừa nhận, không tự GET list ngay; vẫn có nút tải lại.
- Bấm công việc mở màn hình Chi tiết. Có sửa tên, nhóm, ngày/giờ, ghi chú, ưu tiên và tùy chọn nhắc; thêm/sửa/bỏ việc nhỏ rồi Lưu.
- Checkbox việc nhỏ gửi PATCH mảng subtasks. Hoàn thành việc nhỏ không tự đánh dấu công việc cha.
- Xóa yêu cầu xác nhận; chỉ trở về màn hình trước và bỏ công việc khỏi danh sách sau ACK 2xx. Lưu lỗi giữ bản nháp; rời phần sửa có thay đổi cần xác nhận bỏ.
- Checkbox gửi PATCH và chờ ACK 2xx rồi mới cập nhật tiến độ; lỗi giữ trạng thái cũ và cho thử lại.
- Tải ban đầu, tải lại, danh sách rỗng và lỗi được phân biệt. Không diễn giải JSON sai/401/500 thành danh sách rỗng.
- Đăng xuất/đổi tài khoản hủy request và xóa state trước; API mock dùng bộ dữ liệu chung theo token, không phải dữ liệu task riêng theo Firebase UID.

| Endpoint | Vai trò |
|---|---|
| `GET https://share.tryapps.store/mock/teach/tasks` | Mảng công việc theo cấu trúc trong ảnh đề |
| `GET https://share.tryapps.store/mock/teach/tasks/{id}` | Một object công việc; kiểm tra ID trả về khớp ID yêu cầu |
| `PATCH https://share.tryapps.store/mock/teach/tasks/{id}` | Cập nhật done, thông tin hoặc subtasks; chờ ACK 2xx |
| `DELETE https://share.tryapps.store/mock/teach/tasks/{id}` | Xóa sau xác nhận, chờ ACK 2xx |

Có thể chọn ngày bằng cách bấm dòng ngày ở header. Nếu server trả dữ liệu mẫu thuộc một ngày cố định, xem ngày đó hoặc chọn Xem tất cả; không tự đổi ngày hệ thống hoặc gán ngày dữ liệu mẫu thành “hôm nay”. API mock có thể trả lại mẫu khi tải lại; chưa xác minh khả năng lưu thay đổi lâu dài.

Chuông/thanh Thông báo mở phần bật FCM đã có. Nút thêm công việc mới vẫn chưa có POST. Tùy chọn remind được lưu qua API; lập lịch nhắc theo ngày/giờ chưa triển khai. Hợp đồng GET/PATCH/DELETE dựa trên ảnh đề; payload PATCH dùng các trường/kiểu của GET, còn cần đối chiếu dịch vụ thật.

## Firebase

Dùng `app/google-services.json` của đúng Android app `com.example.uth_todo`. Google Services/Crashlytics plugin chỉ áp dụng khi config tồn tại; Guest vẫn dùng được khi chưa cấu hình Auth.

- Google/email, đăng ký, đặt lại mật khẩu và đăng xuất theo BTVN 02.
- RTDB lưu tên ở `users/{uid}/profile`; email/UID/xác minh lấy từ Auth.
- FCM nhận thông báo chung qua topic `uth_todo_updates`.
- Remote Config điều khiển banner Home; Analytics ghi route/profile_saved; Crashlytics ghi lỗi bất thường đã loại thông tin tài khoản.
- Giữ Spark theo phạm vi hiện tại. Cấu hình Console/rules và kiểm thử dịch vụ theo [BTVN03-REPORT.md](docs/BTVN03-REPORT.md).

Không stage `google-services.json`, `api.local.properties`, `local.properties`, cấu hình IDE/công cụ hoặc cache.

## Kiểm tra

| Flow chi tiết | Kết quả trong môi trường agent |
|---|---|
| Unit test chạy riêng bằng Kotlin 2.2.10/JUnit | 74/74 đạt: 57 test hồi quy + 17 test API/Chi tiết mới |
| Biên dịch riêng 50 source Kotlin/Compose với thư viện có sẵn, header R/BuildConfig phục vụ kiểm tra nguồn | Đạt; chưa phải APK/build Gradle |
| Biên dịch mã Android test với thư viện có sẵn | Đạt; 23 test, gồm 8 test Chi tiết mới; chưa chạy trên thiết bị |
| AAPT2 compile resources | Đạt |
| assembleDebug/testDebugUnitTest/lintDebug qua wrapper | Bị chặn mở file khóa wrapper, Access is denied; chưa xác nhận APK/lint của mốc này |
| UI/API thật của màn hình Chi tiết mới | Chờ người dùng chạy; agent chưa vận hành thiết bị hoặc xác minh live API |

Bố cục/JSON dựa trên ảnh nhúng trong file đề cục bộ; Figma/prototype và live API chưa truy cập được qua công cụ. Người dùng đã xác nhận build/chạy đạt Home và Danh sách, đồng thời đã push commit `4d0d14e`. Màn hình Chi tiết mới cần nghiệm thu riêng. Mã Android test còn một cảnh báo deprecation của createComposeRule hiện có.

## Cấu trúc

Các đường dẫn Kotlin tính từ `app/src/main/java/com/example/uth_todo/`.

| Thành phần | Trách nhiệm |
|---|---|
| `MainActivity`, `navigation/` | Factory ViewModel, một Activity/NavHost, bind phiên và điều hướng |
| `model/` | Phiên, hồ sơ, task/category/priority, nhóm lỗi và thống kê |
| `data/auth/`, `data/preferences/` | Firebase Auth/Credential Manager và onboarding DataStore |
| `data/firebase/`, `notifications/` | Hồ sơ RTDB, Remote Config/telemetry, FCM |
| `data/tasks/` | HTTP/JSON API, ngày, lọc/tìm kiếm và thống kê |
| `ui/home/` | HomeViewModel dùng chung dữ liệu/điều phối API, tiến độ, thẻ công việc |
| `ui/tasklist/` | Màn hình danh sách, state tìm kiếm/bộ lọc theo NavBackStackEntry |
| `ui/taskdetail/` | Màn hình Chi tiết/editor, bản nháp bất biến và kiểm tra dữ liệu nhập |
| `ui/profile/` | Hồ sơ, lưu tên, quyền thông báo |
| `ui/onboarding/`, `ui/splash/`, `ui/welcome/`, `ui/auth/` | Khởi động và đăng nhập |
| `ui/components/`, `ui/theme/` | Thanh điều hướng Home/Danh sách, thành phần, typography và minh họa dùng chung |

Giữ SDK 37/minSdk 24, AGP 9.4.1, Gradle 9.6.0, Kotlin 2.2.10, Java source/target 11 và cấu hình máy đang hoạt động. Không đổi versionName hoặc nâng Firebase BoM.
