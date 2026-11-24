# Bàn giao UTH Todo

Cập nhật 24/11/2026. Phạm vi bản này: ứng dụng quản lý công việc lưu trên thiết bị, có tài khoản/hồ sơ Firebase, nhắc việc cục bộ và cài đặt giao diện. Chuẩn bị xong mã và tài liệu không thay cho xác nhận build/Run trên máy người dùng.

## Các chức năng đã có mã

| Nhóm | Chức năng |
|---|---|
| Khởi động | Splash, onboarding ba trang, ghi nhận đã xem, điều hướng/Back |
| Tài khoản | Email, Google, Khách, đăng xuất; xử lý lỗi cấu hình/đăng nhập |
| Hồ sơ | Đọc và sửa tên trên Firebase Realtime Database |
| Firebase | FCM, Analytics, Remote Config, Crashlytics đã tích hợp theo BTVN 03 |
| Công việc | Home theo ngày/tiến độ, danh sách/tìm không dấu/lọc nhóm, chi tiết, tạo/sửa/xóa, hoàn thành, việc nhỏ |
| Lưu trữ | DataStore, giữ dữ liệu sau khi đóng app, tách Guest/UID; chọn riêng dữ liệu mẫu API |
| Nhắc việc | Xin quyền, gửi thử, đặt/hủy/khôi phục lịch, mở đúng task khi bấm thông báo |
| Giao diện | Theo hệ thống/Sáng/Tối, lưu lựa chọn, áp dụng toàn app và màu thanh hệ thống |

## Build và tạo APK để chạy thử/nộp demo

Trong Terminal tại `D:\UTH-TODO_APP\UTH-TODO_Mobile`:

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

Chỉ dùng APK sau khi lệnh trên kết thúc bằng **BUILD SUCCESSFUL**. Nếu lệnh lỗi, APK cũ còn trong thư mục không chứng minh mã mới đã build được.

APK debug sau build nằm tại:

```text
app\build\outputs\apk\debug\app-debug.apk
```

Đây là bản debug cho kiểm thử/demo, chưa phải bản release có chữ ký phát hành. Dùng nút Run của Android Studio để cập nhật lên thiết bị đang dùng. Không cần reset app, gỡ cài đặt hoặc xóa cache; các thao tác xóa dữ liệu có thể làm mất task cục bộ.

Chạy bộ kiểm thử UI khi đã có emulator/thiết bị kết nối:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

Bộ kiểm thử UI dùng dữ liệu giả riêng; việc chạy test không thay thế kiểm tra Google/Firebase, quyền Android và lịch nhắc thực tế.

## Kiểm thử chốt bản

Chưa tự đánh dấu các mục dưới đây là đạt; người dùng kiểm tra trên bản APK vừa build.

- [ ] Build, unit test và lint cùng thành công.
- [ ] Chọn Sáng/Tối/Theo hệ thống, Back, đóng/mở app, đổi cài đặt Android; màu và lựa chọn vẫn đúng.
- [ ] Đăng nhập email/Google hoặc dùng Khách; đăng xuất không hiện dữ liệu phiên trước. Google cần thiết bị có tài khoản Google và cấu hình Firebase đúng.
- [ ] Tạo "Lau nha" trong **Công việc trên thiết bị**; danh sách và chi tiết phải đúng nội dung, không sinh bài ôn tập mẫu.
- [ ] Sửa tên/ghi chú/ngày/giờ/nhóm/ưu tiên/việc nhỏ; tải lại và đóng/mở app vẫn giữ. Đánh dấu hoàn thành rồi mở lại vẫn đúng.
- [ ] Xóa có xác nhận; hủy xác nhận không xóa. Task đã xóa không xuất hiện lại khi mở app.
- [ ] Tìm không dấu/lọc nhóm; mở Chi tiết/Hồ sơ/Giao diện rồi Back không mất bộ lọc; Home phản ánh đúng số việc theo ngày.
- [ ] Nhắc việc: cấp quyền, gửi thử; tạo hạn khoảng 3 phút tới theo đồng hồ thiết bị, bật nhắc và đưa app ra nền. Bấm thông báo mở đúng task.
- [ ] Sửa hạn/tắt nhắc/hoàn thành/xóa/đăng xuất hoặc đổi nguồn trước giờ nhắc; lịch cũ bị hủy. Thử thiếu quyền thông báo/exact và cấp lại.
- [ ] Tắt mạng: CRUD thiết bị và đổi giao diện vẫn dùng được; các thao tác cần Firebase/API hiển thị lỗi phù hợp.
- [ ] Font lớn, màn hình hẹp, bàn phím, nút Back và hai theme: không che nút lưu/xóa hoặc làm chữ không đọc được.
- [ ] Hồ sơ Firebase lưu tên và Home cập nhật; giữ cách thử FCM/Remote Config/Analytics/Crashlytics theo báo cáo BTVN 03.

Khởi động lại thiết bị, thay múi giờ và cập nhật app cần thử thêm nếu nghiệm thu đầy đủ khả năng khôi phục nhắc giờ. Android có thể trì hoãn lịch gần đúng hoặc dừng lịch sau force-stop/hạn chế pin; mở lại app để đối chiếu lịch. Không dùng Clear data làm bước sửa lỗi.

## Máy khác lấy mã nguồn

1. Mở đúng thư mục chứa `settings.gradle.kts`, `gradlew.bat`, `app/`, không mở thư mục cha `UTH-TODO_APP`.
2. Dùng Android SDK/JDK phù hợp cấu hình hiện có; để Android Studio thiết lập `local.properties` riêng trên máy đó. Không sao chép đường dẫn SDK của máy cũ và không commit file này.
3. Để dùng Firebase, thêm `app/google-services.json` đúng project/package `com.example.uth_todo`. Google Sign-In cần fingerprint của khóa ký tương ứng đã đăng ký trong Firebase. Không có cấu hình Firebase vẫn có thể dùng Khách và task thiết bị.
4. Chỉ khi cần thử dữ liệu mẫu API: tạo `api.local.properties` từ template `api.local.properties.example` nếu chưa có, điền token được cấp riêng. Không ghi đè file đang có hoặc commit token.
5. Sync Gradle, chạy build/check ở trên rồi Run. Firebase config/token cần chia sẻ riêng cho người được phép; không có service-account key trong app.

## Trạng thái kiểm chứng của agent

| Kiểm tra | Kết quả |
|---|---|
| JUnit chạy riêng với thư viện thực có sẵn | 148/148 đạt |
| Biên dịch Kotlin/Compose với compile dependencies từ Gradle model | 75 file đạt; không phải build APK |
| AAPT2 compile tài nguyên | Đạt |
| Biên dịch Android test với compile dependencies từ Gradle model | 52 test đạt biên dịch, gồm 6 test mới; chưa thực thi |
| Gradle assembleDebug/testDebugUnitTest/lintDebug | Bị chặn trước task do quyền mở file khóa wrapper |
| UI thực tế/connected tests/Google/Firebase/nhắc thực tế | Cần người dùng chạy và xác nhận |

Các báo cáo nguồn không thay thế một lần Gradle build thành công. Trong môi trường agent, không xóa cache/khóa, thay phiên bản hoặc sửa đường dẫn để vượt giới hạn quyền.

## Phần ngoài bản chốt này

- **Chưa có đồng bộ task giữa thiết bị.** Firebase hiện lưu tài khoản/hồ sơ; task trên máy A không tự xuất hiện trên máy B.
- Server API của bài tập là nguồn mẫu riêng. Phản hồi POST khác nội dung bị từ chối; việc app dùng được task thiết bị không chứng minh server mẫu đã lưu dữ liệu.
- Chưa xác nhận yêu cầu BTVN 05 vì chưa có đề chi tiết. Chưa thực hiện phát hành cửa hàng hoặc tạo bản release có chữ ký.

Khi checklist trên đạt, có thể chốt **bản demo quản lý công việc trên thiết bị**. Không gắn nhãn đã hoàn thành đồng bộ cloud hay toàn bộ bài tập chưa được cung cấp.

## Stage, commit, push

Sau khi build và chạy thử đạt, tạo nhánh mới từ nhánh hiện tại nếu chưa tạo:

```powershell
git switch -c feature/appearance-finalization
git add app/src/main app/src/test app/src/androidTest
git add README.md docs/BRANCH-ROADMAP.md docs/APPEARANCE-REPORT.md docs/PROJECT-HANDOFF.md
git diff --cached --stat
git diff --cached
git commit -m "feat: persist appearance settings and prepare project handoff"
git push -u origin feature/appearance-finalization
```

Không tạo lại nhánh nếu đã tồn tại/đang đứng trên nhánh đó. Không dùng `git add .` để tránh kéo theo cấu hình máy, IDE/công cụ hoặc dữ liệu nhạy cảm. Agent chưa thực hiện các lệnh Git ghi này.

Nếu bài nộp cần mã trên `main`, sau khi nhánh mới đã push và kiểm thử đạt, mở Pull Request vào `main` để review rồi merge. Đây là bước Git của người dùng, chưa được agent thực hiện.
