# BTVN 03 — Tích hợp dịch vụ Firebase

Cập nhật 20/11/2026, tiếp nối BTVN 02 đã được người dùng thử thành công và commit `4f3002e`.

**Trạng thái cập nhật:** người dùng đã chạy `assembleDebug testDebugUnitTest lintDebug` thành công trong 5 phút 50 giây (56 task). Báo cáo Gradle xác nhận **35/35 unit test đạt**, lint **0 lỗi/24 warning**. Nhánh hiện tại là `feature/btvn03-firebase-services`; config Android đã có URL Realtime Database. Người dùng sau đó xác nhận tự kiểm thử thành công và gửi ảnh node profile với displayName/updatedAt trên RTDB thật. Sẵn sàng commit/push theo nghiệm thu người dùng. Agent không tự chạy UI/Firebase thật; 16 test rules emulator vẫn chưa thực thi. Agent chưa tự tạo/chuyển nhánh, stage, commit hoặc push.

## Phạm vi

Đề gốc chỉ ghi “tích hợp toàn bộ các dịch vụ Firebase”, không có danh sách dịch vụ/API cụ thể. Đợt này chọn dịch vụ phù hợp UTH Todo và dùng được trên Spark, tiếp tục Auth đã có. Không khẳng định đã bao phủ toàn bộ sản phẩm Firebase.

| Dịch vụ | Tích hợp thực tế | Điểm nghiệm thu trên app/Console |
|---|---|---|
| Authentication | Giữ Google/email/Guest và đăng xuất BTVN 02 | Hồi quy sau Sync |
| Realtime Database | Hồ sơ theo UID, lưu tên và timestamp máy chủ, listener realtime | Config URL và lưu/đọc hồ sơ thật theo UID |
| Cloud Messaging | Foreground/data service, background notification, channel, quyền API 33+, topic chung | Cấp quyền và gửi từ Console |
| Analytics | `screen_view` cho route, `profile_saved` sau ACK | Analytics DebugView |
| Crashlytics | Plugin, SDK, crash tự động và nonfatal lỗi bất thường đã loại thông tin tài khoản | Khởi tạo/report trên Console |
| Remote Config | Bật/tắt và nội dung banner Trang chủ, defaults/timeout/fallback | Publish và mở lại app |

Không bật Cloud Storage hoặc deploy Cloud Functions vì yêu cầu Blaze. Chưa làm Firestore song song RTDB, avatar, nhắc hạn cá nhân, danh sách/API công việc hay thống kê. Topic chỉ chứa thông báo chung, không chứa dữ liệu tài khoản/công việc.

## Cấu hình Firebase Console

### 1. Realtime Database

1. Mở đúng dự án UTH Todo đang dùng cho đăng nhập, vào **Build → Realtime Database → Create database**.
2. Chọn vị trí phù hợp, có thể Singapore nếu được cung cấp; dùng chế độ khóa ban đầu.
3. Trong **Rules**, dùng nội dung `firebase/database.rules.json` và **Publish**. Đây là schema UTH Todo của bài này, không phải database English Battle.
4. Trong **Project settings → General → Your apps → Android**, tải lại `google-services.json` sau khi tạo DB và thay file cục bộ `app/google-services.json`.
5. File cần có `project_info.firebase_url` để plugin sinh `firebase_database_url`. Không ghép URL từ project ID vì khác vùng/tên database.
6. Sync Gradle, build và Run lại.

Kiểm tra sau khi người dùng cập nhật cấu hình và build: file Android **đã có firebase_url**. SDK đã qua bước biên dịch; người dùng sau đó xác nhận chạy thử thành công và cung cấp ảnh profile được lưu trong database. Agent không tự thay đổi hoặc xác minh toàn bộ rules trên Console. Agent không sửa config, đường dẫn SDK hoặc cấu hình máy.

Đăng nhập, mở **Thông tin cá nhân → Tên của bạn**, nhập tên rồi **Lưu tên hiển thị**. Node được tạo ở lần lưu đầu tiên: `users/{uid}/profile`.

| Trường | Nội dung |
|---|---|
| `displayName` | Chuỗi 1–60 ký tự; client trim và từ chối ký tự điều khiển |
| `updatedAt` | Timestamp máy chủ RTDB |

Home/Profile ưu tiên tên RTDB. Khi chưa có node, dùng tên Auth làm gợi ý để lưu. Không ghi đè tên trong Firebase Auth; email/UID/xác minh vẫn lấy từ Auth. Không sao chép mật khẩu, token hoặc quyền vào DB.

Rules chỉ cho UID tương ứng đọc/ghi chính profile; khóa root/users, UID khác, trường lạ, xóa profile và timestamp không khớp thời điểm máy chủ. Đường dẫn ngoài phạm vi mặc định bị khóa. Rules chưa được publish tự động.

### 2. Thông báo FCM

1. Run trên thiết bị có Google Play services và mạng.
2. Vào **Thông tin cá nhân → Thông báo → Bật thông báo**, chấp nhận quyền trên Android 13+.
3. Đợi “Đã bật thông báo chung của UTH Todo”. Lỗi mạng có thể thử lại; từ chối quyền có thể vào **Cài đặt thông báo** để cấp lại.
4. Trong Firebase Console **Messaging**, soạn notification, chọn topic `uth_todo_updates` và gửi nội dung thử không có dữ liệu cá nhân.
5. Thử khi app foreground và background; bấm notification mở app qua luồng đăng nhập hiện có.

Background notification payload do FCM SDK hiển thị; service hiển thị callback foreground/data. Không tự mở link/route từ payload. Topic được SDK duy trì theo installation; chưa có server gửi riêng theo UID, không lưu/in token ra log, không cần service account hoặc Functions. Tắt channel/quyền trong Android Settings vẫn dùng app bình thường. Đây chưa phải nhắc hạn từng công việc.

### 3. Remote Config

Trong **Remote Config**, tạo và **Publish changes**:

| Parameter | Kiểu | Ví dụ |
|---|---|---|
| `home_announcement_enabled` | Boolean | `true` |
| `home_announcement_text` | String | `Chúc bạn một ngày học tập hiệu quả!` |

Mở lại tiến trình app để fetch; debug có khoảng cách fetch tối thiểu 60 giây, release 12 giờ. Không chờ Remote Config để vào Home. Offline/chưa publish dùng cấu hình đã activate hoặc mặc định ẩn banner. Nội dung trim, tối đa 300 ký tự; chuỗi rỗng không tạo thẻ rỗng.

### 4. Analytics và Crashlytics

- **Project settings → Integrations**: bật/liên kết Analytics nếu chưa có. Mở màn hình và lưu tên để phát sinh sự kiện.
- DebugView: `adb shell setprop debug.firebase.analytics.app com.example.uth_todo`, rồi mở lại app. Kết thúc: `adb shell setprop debug.firebase.analytics.app .none.`. Dùng Platform Tools sẵn có nếu adb chưa trong PATH; không đổi SDK path.
- Mở **Crashlytics** và hoàn tất khởi tạo SDK. Plugin chỉ áp dụng khi có Android config, như Google Services.
- Crashlytics tự nhận crash; code mới báo nonfatal với tên thao tác/nhóm lỗi cố định. Offline, quyền bị từ chối, validation và thiếu config không bị coi là lỗi ứng dụng. Không gửi SDK exception message, UID, email, tên hoặc token vào báo cáo tùy biến.
- Agent chưa gây crash hoặc gửi report thử. Nếu cần test nonfatal có chủ đích, trong phiên debug do người dùng điều khiển có thể Evaluate Expression: `com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(java.lang.IllegalStateException("UTH_BTVN03_TEST"))`, rồi kiểm tra Console. Không có nút gây crash/debug trong UI sản phẩm.

## Kiến trúc và trạng thái

- `ProfileRepository` tách SDK khỏi UI. `FirebaseProfileRepository` dùng callbackFlow/awaitClose để gỡ listener và kiểm tra UID Auth trước khi tạo reference.
- `FirebaseServicesViewModel` hủy job/listener, xóa tên cũ khi đổi tài khoản/đăng xuất; kết quả muộn của tài khoản cũ không áp vào tài khoản mới. Guest không đọc/ghi profile cloud.
- Chỉ completion callback máy chủ mới báo “Đã lưu”. Snapshot local có thể hiển thị tên đang chờ gửi nhưng không xác nhận write. Timeout nói chưa xác nhận; SDK có thể tiếp tục gửi write đã xếp hàng khi mạng trở lại.
- Editor giữ bản nháp khi snapshot mới đến, tách lỗi đọc/lưu, chặn bấm lặp và giới hạn thời gian chờ. Home dùng tên đã đồng bộ.
- Remote Config có defaults XML/fallback. AppTelemetry cho test fake, chỉ nhận event/nhóm lỗi cố định.
- UthTodoApplication tạo channel; UthMessagingService nhận FCM; card Hồ sơ xin quyền khi bấm và hỗ trợ quay lại từ Settings.
- Giữ nguyên Splash/onboarding, SystemBarStyle, Google/email, Back stack, SDK, AGP, Kotlin, Gradle, JDK, versionName và đường dẫn máy. BoM vẫn 34.7.0 cho Kotlin 2.2.10.

## File thay đổi

| Nhóm | Mục đích |
|---|---|
| Version catalog, root/app Gradle | Năm SDK theo BoM cũ, Crashlytics plugin 3.0.8 |
| `model/CloudProfile.kt`, `data/firebase/` | Model, hợp đồng, RTDB, Remote Config, telemetry |
| `ui/profile/FirebaseServicesViewModel.kt` | Phiên hồ sơ, tải/lưu/lỗi/timeout |
| Profile editor/notification card, Home/Profile | Chỉnh tên, nhận thông báo, banner |
| MainActivity/NavHost | Factory, bind phiên và callback |
| Application, notifications, manifest, resources | Channel, FCM service, quyền và chuỗi |
| `FirebaseServicesViewModelTest.kt` | 14 test hành vi |
| `firebase/database.rules.json`, `test_database_rules.py` | Rules và 16 test trên DB emulator |
| README, roadmap, báo cáo | Cấu hình và nghiệm thu |

## Kiểm tra thực tế

| Kiểm tra | Kết quả |
|---|---|
| Kotlin 2.2.10 biên dịch riêng model/interfaces/ViewModels/tests | Đạt |
| JUnit Auth + FirebaseServices ViewModel | **26/26 đạt**, gồm 12 Auth cũ + 14 mới |
| testDebugUnitTest trong lần build người dùng xác nhận | **35/35 đạt**, 0 failure/error/skipped: Auth 12, Firebase Services 14, Onboarding ViewModel 7, Preferences 1, Example 1 |
| AAPT2 36.0.0 compile resources app | Đạt; không đổi build-tools của Gradle |
| Parse XML/TOML/JSON, trùng resource name, Kotlin/BoM giữ nguyên | Đạt |
| git diff --check | Đạt; chỉ cảnh báo LF/CRLF |
| assembleDebug/testDebugUnitTest/lintDebug trên máy người dùng | **BUILD SUCCESSFUL**, 5 phút 50 giây, 56 task; lint 0 lỗi/24 warning |
| Wrapper/tải dependency trong môi trường agent, lần trước | Bị chặn file khóa Access is denied/DNS getaddrinfo failed; kết quả build mới của người dùng đã xác minh được compile Android |
| python firebase/test_database_rules.py | Emulator bị chặn loopback Java, Permission denied: connect; **16 test chưa thực thi** |
| UI/Firebase thật theo nghiệm thu người dùng | Người dùng xác nhận đã tự kiểm thử thành công; ảnh RTDB cho thấy profile đã có displayName và updatedAt. Agent không tự chạy UI hoặc thu log riêng cho từng dịch vụ |

Lần build BTVN 03 của người dùng đã xác minh SDK resolve/biên dịch, APK debug, unit test và lint. Unit test dùng fake. Bằng chứng runtime là xác nhận tự kiểm thử của người dùng và ảnh hồ sơ đã ghi lên RTDB; không coi đó là kết quả tự động của 16 test rules emulator hoặc log riêng cho từng dịch vụ.

Lệnh chuẩn:

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

Nếu đã có Python/Java/RTDB emulator JAR, có thể chạy:

```powershell
python firebase/test_database_rules.py
```

Script dùng loopback/cổng trống và namespace demo, nạp rules, chạy 16 test rồi dừng đúng tiến trình do nó tạo. Không kết nối DB thật hoặc điều khiển/xóa dữ liệu AVD. Có thể truyền `--emulator-jar PATH`. Python không bắt buộc để build Android.

## Nghiệm thu người dùng

Ngày 20/11, người dùng thông báo: “tôi đã test và thấy thành công hết rồi”. Ảnh kèm theo xác nhận profile trong RTDB thật có tên hiển thị và timestamp. Ghi nhận các luồng người dùng đã tự thử là đạt; không lưu tên, UID, URL dự án hoặc dữ liệu tài khoản trong báo cáo. Không suy diễn thêm rằng agent đã chạy UI, test rules emulator hay thu báo cáo riêng trên từng dashboard Firebase.

## Checklist tham khảo khi kiểm thử lại

- Build, unit test, lint phiên bản mới thành công.
- Config Android có URL Realtime Database.
- Google/email/Guest, onboarding và Back vẫn hoạt động.
- Tạo DB, tải config có URL, publish rules.
- Tài khoản A lưu tên có dấu: Console có đúng UID/name/server timestamp, Home đổi tên.
- Mở lại app/thiết bị khác cùng tài khoản: đọc lại tên.
- Đổi sang B/Guest: không còn tên cloud của A; Guest không có form lưu.
- Mất mạng/thử lại: không treo hoặc báo lưu khi chưa ACK.
- Chạy test rules, xác minh truy cập chéo UID bị từ chối.
- Từ chối/cấp lại quyền thông báo đều xử lý đúng.
- Nhận topic FCM foreground/background, bấm mở app.
- Publish Remote Config: banner hiện/ẩn sau fetch.
- Analytics DebugView và Crashlytics report được kiểm tra.
- Form tên dùng được với font lớn, màn hình nhỏ/landscape/bàn phím.

## Git và bước sau

Người dùng đã tạo/chọn `feature/btvn03-firebase-services`; các thay đổi vẫn chưa stage/commit. Không cần tạo lại nhánh. Đã sẵn sàng commit/push theo xác nhận nghiệm thu của người dùng; lệnh chọn file nằm trong [BRANCH-ROADMAP.md](BRANCH-ROADMAP.md).

Mốc kế tiếp là BTVN 04 Home UI/API danh sách và thống kê. Chưa triển khai lấn sang API hoặc đưa token vào source.

## Nguồn

- [Pricing](https://firebase.google.com/pricing), [Spark/Blaze](https://firebase.google.com/docs/projects/billing/firebase-pricing-plans), [Storage yêu cầu Blaze](https://firebase.google.com/docs/storage/faqs-storage-changes-announced-sept-2024).
- [Realtime Database Android](https://firebase.google.com/docs/database/android/read-and-write), [Rules](https://firebase.google.com/docs/database/security).
- [FCM Android](https://firebase.google.com/docs/cloud-messaging/android/get-started), [nhận thông báo](https://firebase.google.com/docs/cloud-messaging/android/receive-messages).
- [Remote Config](https://firebase.google.com/docs/remote-config/android/get-started), [hạn mức](https://firebase.google.com/docs/remote-config/pricing).
- [Crashlytics/plugin 3.0.8](https://firebase.google.com/docs/crashlytics/android/get-started), [Analytics](https://firebase.google.com/docs/analytics/get-started?platform=android).
