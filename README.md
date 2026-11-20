# UTH Todo

Ứng dụng Android Kotlin + Jetpack Compose, phát triển theo từng bài tập môn Lập trình di động.

| Mốc | Kết quả |
|---|---|
| BTVN 01 | Splash/onboarding/Navigation + OOP; commit `c4a0623`, người dùng đã thử app |
| BTVN 02 | Google/email/Guest/Profile; commit `4f3002e`, người dùng xác nhận chạy được |
| BTVN 03 | RTDB, FCM, Analytics, Crashlytics, Remote Config; commit `028f378`, người dùng đã build và tự kiểm thử; Gradle 35/35 unit test, lint 0 lỗi/24 warning |
| BTVN 04 | Home UI/API, tiến độ/ngày, danh sách xem nhanh/tìm kiếm/lọc và đánh dấu hoàn thành; người dùng đã xác nhận build/chạy thử thành công, sẵn sàng commit/push |

Xem [báo cáo BTVN 04](docs/BTVN04-REPORT.md), [lộ trình nhánh](docs/BRANCH-ROADMAP.md), và báo cáo [01](docs/BTVN01-REPORT.md), [02](docs/BTVN02-REPORT.md), [03](docs/BTVN03-REPORT.md).

## Chạy BTVN 04

Người dùng đã ở `feature/btvn04-home-api` và xác nhận build/chạy thử thành công. Các thay đổi chưa stage/commit; khi cần tái kiểm tra, chạy:

```powershell
git branch --show-current
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
- Home hiển thị tối đa 8 việc trong ngày theo giờ; Xem tất cả hoặc thẻ nhóm mở danh sách đầy đủ dạng bottom sheet.
- Danh sách có tìm kiếm tiếng Việt không cần dấu, lọc nhóm, chia Cần làm/Đã xong.
- Bấm một việc để xem nhanh nội dung đang có trong danh sách. Đây chưa phải flow chi tiết tải lại riêng bằng API.
- Checkbox gửi PATCH và chờ ACK 2xx rồi mới cập nhật tiến độ; lỗi giữ trạng thái cũ và cho thử lại.
- Tải ban đầu, tải lại, danh sách rỗng và lỗi được phân biệt. Không diễn giải JSON sai/401/500 thành danh sách rỗng.
- Đăng xuất/đổi tài khoản hủy request và xóa state trước; API mock dùng bộ dữ liệu chung theo token, không phải dữ liệu task riêng theo Firebase UID.

| Endpoint | Vai trò |
|---|---|
| `GET https://share.tryapps.store/mock/teach/tasks` | Mảng công việc theo cấu trúc trong ảnh đề |
| `PATCH https://share.tryapps.store/mock/teach/tasks/{id}` | Gửi `{"done":true}` hoặc `{"done":false}`; xử lý ACK 2xx |

Có thể chọn ngày bằng cách bấm dòng ngày ở header. Nếu server trả dữ liệu mẫu thuộc một ngày cố định, xem ngày đó hoặc chọn Xem tất cả; không tự đổi ngày hệ thống hoặc gán ngày dữ liệu mẫu thành “hôm nay”. API mock có thể trả lại mẫu khi tải lại; chưa xác minh khả năng lưu thay đổi lâu dài.

Chuông/thanh Thông báo mở phần bật FCM đã có. Nút thêm việc hiện thông báo chưa sẵn sàng; chưa có POST, xóa, sửa nội dung hoặc flow chi tiết CRUD.

## Firebase

Dùng `app/google-services.json` của đúng Android app `com.example.uth_todo`. Google Services/Crashlytics plugin chỉ áp dụng khi config tồn tại; Guest vẫn dùng được khi chưa cấu hình Auth.

- Google/email, đăng ký, đặt lại mật khẩu và đăng xuất theo BTVN 02.
- RTDB lưu tên ở `users/{uid}/profile`; email/UID/xác minh lấy từ Auth.
- FCM nhận thông báo chung qua topic `uth_todo_updates`.
- Remote Config điều khiển banner Home; Analytics ghi route/profile_saved; Crashlytics ghi lỗi bất thường đã loại thông tin tài khoản.
- Giữ Spark theo phạm vi hiện tại. Cấu hình Console/rules và kiểm thử dịch vụ theo [BTVN03-REPORT.md](docs/BTVN03-REPORT.md).

Không stage `google-services.json`, `api.local.properties`, `local.properties`, cấu hình IDE/công cụ hoặc cache.

## Kiểm tra

| BTVN 04 | Kết quả trong môi trường agent |
|---|---|
| Unit test chạy riêng bằng Kotlin 2.2.10/JUnit | 51/51 đạt: 25 test API/Home mới + 26 Auth/Firebase hồi quy |
| Biên dịch riêng 46 source Kotlin/Compose với thư viện có sẵn, header R/BuildConfig phục vụ kiểm tra nguồn | Đạt; chưa phải APK/build Gradle |
| AAPT2 compile resources | Đạt |
| assembleDebug/testDebugUnitTest/lintDebug qua wrapper | Bị chặn file khóa wrapper, Access is denied |
| Gọi API thật/Figma/prototype | Không truy cập được; host bị chặn DNS, trang thiết kế không đọc được qua công cụ |
| UI/API trên thiết bị theo xác nhận người dùng | Người dùng báo đã build và chạy thử thành công; agent không tự vận hành thiết bị |

Bố cục/JSON dựa trên ảnh nhúng trong file đề cục bộ; chưa khẳng định khớp từng pixel Figma hoặc đã gọi API thật thành công. Kết quả Gradle 35 test/lint ở BTVN 03 là lịch sử. BTVN 04 đã được người dùng cung cấp ảnh BUILD SUCCESSFUL và sau đó xác nhận chạy thử thành công; số test/lint mới không được suy diễn từ kết quả cũ.

## Cấu trúc

Các đường dẫn Kotlin tính từ `app/src/main/java/com/example/uth_todo/`.

| Thành phần | Trách nhiệm |
|---|---|
| `MainActivity`, `navigation/` | Factory ViewModel, một Activity/NavHost, bind phiên và điều hướng |
| `model/` | Phiên, hồ sơ, task/category/priority, nhóm lỗi và thống kê |
| `data/auth/`, `data/preferences/` | Firebase Auth/Credential Manager và onboarding DataStore |
| `data/firebase/`, `notifications/` | Hồ sơ RTDB, Remote Config/telemetry, FCM |
| `data/tasks/` | HTTP/JSON API, ngày, lọc/tìm kiếm và thống kê |
| `ui/home/` | HomeViewModel, tiến độ, thẻ công việc, danh sách/xem nhanh |
| `ui/profile/` | Hồ sơ, lưu tên, quyền thông báo |
| `ui/onboarding/`, `ui/splash/`, `ui/welcome/`, `ui/auth/` | Khởi động và đăng nhập |
| `ui/components/`, `ui/theme/` | Thành phần, typography và minh họa dùng chung |

Giữ SDK 37/minSdk 24, AGP 9.4.1, Gradle 9.6.0, Kotlin 2.2.10, Java source/target 11 và cấu hình máy đang hoạt động. Không đổi versionName hoặc nâng Firebase BoM.
