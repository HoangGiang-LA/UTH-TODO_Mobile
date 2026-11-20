# Nhắc công việc theo giờ và màn hình Thông báo

Ngày: 20/11/2026. Base `59457c8`. Nhánh dự kiến `feature/task-reminders`; agent giữ nguyên nhánh hiện tại khi triển khai.

## Đã triển khai

Màn hình Thông báo riêng mở từ chuông Home, thanh Thông báo ở Home/List hoặc Nhắc công việc trong Hồ sơ. Back quay về nơi mở; từ List giữ từ khóa/bộ lọc/vị trí cuộn.

Màn hình có trạng thái quyền, số lịch đang đặt, nút bật thông báo, quyền nhắc đúng giờ, gửi thông báo thử, tải lại và các việc chưa xong đã bật remind. Quyền thông báo chung/FCM hiện có được giữ ở phần riêng; nhắc công việc chạy cục bộ, không cần Firebase Messaging hoặc mạng.

| Trường hợp | Hành vi |
|---|---|
| Task thiết bị chưa xong, remind=true, ngày/giờ hợp lệ và tương lai | Đặt lịch |
| Còn hơn 15 phút khi tạo lịch | Nhắc trước hạn 15 phút |
| Còn không quá 15 phút khi tạo lịch | Nhắc vào hạn chót |
| Task mới đã quá hạn/thiếu lịch/done/remind=false | Không tạo lịch mới |
| Sửa hạn chót | Hủy lịch cũ và đặt lịch mới |
| Hoàn thành/xóa/tắt remind | Hủy lịch và thông báo cũ liên quan |
| Đăng xuất/đổi Guest/UID hoặc chọn dữ liệu mẫu | Hủy lịch/thông báo của nguồn hoặc phiên trước |
| Dữ liệu mẫu API | Không đặt lịch giờ |
| Cùng task và cùng hạn chót đã gửi | Không gửi thêm khi refresh hoặc khôi phục |

Ngày/giờ được hiểu theo múi giờ thiết bị. Thời điểm không tồn tại do đổi giờ mùa hè không được tự dời sang giờ khác. Bật remind trong form yêu cầu đủ ngày và giờ. Task cũ đã lưu remind=true nhưng thiếu lịch vẫn hoàn thành/đổi việc nhỏ được; không bị khóa thao tác bởi quy tắc form mới.

## Quyền và độ chính xác

POST_NOTIFICATIONS và kênh Nhắc công việc được kiểm tra trước khi gửi. Quyền được hỏi khi người dùng bấm nút; nếu đã từ chối hoặc tắt kênh, có thể mở cài đặt Android. Tính năng không yêu cầu cấu hình Firebase để gửi thông báo cục bộ.

Dùng SCHEDULE_EXACT_ALARM cho lựa chọn nhắc đúng giờ; UI giải thích và mở trang cài đặt Android khi người dùng chọn. Không dùng USE_EXACT_ALARM. Kiểm tra canScheduleExactAlarms trước setExactAndAllowWhileIdle, đồng thời xử lý SecurityException nếu quyền thay đổi.

Khi chưa có quyền exact, dùng setAndAllowWhileIdle và hiển thị trạng thái gần đúng; Android có thể giao muộn. Không hứa chính xác từng giây hoặc coi test gửi thử là đã chứng minh alarm hoạt động. Không chạy timer/foreground service liên tục hoặc polling mạng để canh giờ.

Registry giữ lịch đang chờ và dấu đã gửi cho từng scope/ID/hạn chót. Lịch đã có mà OS giao muộn được giữ để có thể nhắc một lần; body hiển thị hạn thực tế, không khẳng định vẫn còn 15 phút. Task quá hạn chưa từng có lịch không tự sinh nhắc mới.

## Gửi, hủy và khôi phục

Alarm dùng PendingIntent bất biến với data URI riêng theo scope/ID; cập nhật cùng key thay lịch cũ. Receiver không export ra ứng dụng khác. Intent chỉ chứa scope hash/Guest, ID và thời điểm, không chứa tên/ghi chú task.

Trước khi hiển thị, engine đọc lại task đã lưu, mode, trạng thái done/remind, hạn chót, scope đang dùng và quyền. Không tin dữ liệu nội dung từ Intent. Thông báo dùng kênh riêng, tự đóng khi bấm và giữ nội dung riêng tư trên màn hình khóa.

Receiver khôi phục khi BOOT_COMPLETED, MY_PACKAGE_REPLACED, đổi giờ/múi giờ và cấp quyền exact. RECEIVE_BOOT_COMPLETED được khai báo. Việc lưu task/mode phát tín hiệu sau commit; runtime đọc lại kho và cập nhật alarm. Bản thân registry không phát tín hiệu ghi task nên không tạo vòng lặp.

Engine dùng Mutex để serialize reconcile/delivery. Runtime dùng application scope và singleton kho thiết bị, không bị tạo lại theo recomposition/Activity. Registry phụ trợ chỉ lưu scope/ID/thời điểm/dấu đã gửi bằng SharedPreferences riêng, không ghi nội dung task hay token.

Bấm thông báo mở đúng task sau khi có cùng Guest/UID và nguồn thiết bị. Nếu app đang ở Welcome, cần dùng lại đúng tài khoản hoặc Khách. Khác tài khoản hoặc đang xem API thì báo hướng dẫn, không tự đổi phiên/nguồn. Chờ thao tác lưu/đổi nguồn hoàn tất trước khi điều hướng. Back từ Chi tiết vẫn theo lịch sử điều hướng.

## Cấu trúc

Đường dẫn Kotlin tính từ `app/src/main/java/com/example/uth_todo/`.

| Thành phần | Vai trò |
|---|---|
| `notifications/TaskReminderPlan.kt` | Thời điểm nhắc, key, registry/service state |
| `notifications/TaskReminderEngine.kt` | Lập/hủy lịch, đọc lại dữ liệu, dedup và kiểm tra phiên |
| `notifications/TaskReminderController.kt` | Interface bind phiên/refresh/test cho ViewModel |
| `notifications/AndroidReminderAlarms.kt` | AlarmManager, kênh và notification/PendingIntent |
| `notifications/AndroidReminderRegistry.kt` | Lưu registry phụ trợ |
| `notifications/TaskReminderRuntime.kt` | Nối thay đổi kho/quyền/phiên với engine |
| `notifications/TaskReminderReceiver.kt` | Delivery và restore receivers, goAsync có thời hạn |
| `ui/notifications/` | Màn hình và thao tác yêu cầu quyền |
| `data/tasks/TaskSessionScope.kt` | Scope đồng nhất giữa dữ liệu/nhắc/mở thông báo |
| `MainActivity`, `navigation/` | Nhận Intent, bảo vệ route, mở đúng ID |
| `DeviceTaskStorage`, `HomeViewModel`, `TaskDraft` | Phát change sau commit, bind session, validation |

App manifest thêm exact/boot permissions, hai receiver không export và singleTop cho MainActivity. Không thay phiên bản thư viện, Gradle, SDK, Kotlin, JDK, Firebase BoM, đường dẫn máy hoặc cache. Không thêm dependency và không chỉnh Firebase rules/Console.

## Kết quả kiểm tra

| Kiểm tra trong môi trường agent | Kết quả |
|---|---|
| JUnit chạy riêng với Kotlin/thư viện thực có sẵn | 137/137 đạt: 114 hồi quy + 22 nhắc giờ + 1 tương thích task cũ |
| Kotlin/Compose typecheck với header R/BuildConfig phục vụ kiểm tra nguồn | 71 file đạt; chưa phải APK |
| Biên dịch Android test | 45 test, gồm 9 test mới cho màn hình/Back/quyền/open task; chưa chạy thiết bị |
| AAPT2 compile resources | Đạt |
| XML, tên resource, diff, token trong source/docs | Đạt |
| assembleDebug/testDebugUnitTest/lintDebug | Bị chặn trước task do quyền mở file khóa wrapper |
| AlarmManager/permission UI/notification thật/reboot/connected tests | Chưa thực thi trong môi trường agent |

22 unit test mới bao phủ lead 15 phút/gần hạn, bỏ task không hợp lệ/past/done, múi giờ/DST, validation, mode API, gửi một lần, hoàn thành/xóa/tắt remind, sửa hạn, đổi phiên, quyền post/exact, giao muộn, restore, đọc tên mới nhất, post thất bại, Intent sai, đọc cũ sau đổi phiên và bind ViewModel. Một test lưu bền mới giữ tương thích task cũ thiếu ngày/giờ.

9 Android test mới kiểm tra mở Thông báo/Back, giữ List state, task nhắc → Chi tiết, nút test qua controller fake, mở đúng owner, chặn owner khác/API, mất phiên, trạng thái quyền và màn hình hẹp/font 2. Không tự mở dialog quyền Android, gửi notification hoặc sửa dữ liệu app người dùng trong các test này.

Có cảnh báo deprecation createComposeRule từ bộ test cũ. Nguồn/engine test đạt không chứng minh chuông và thời điểm thực trên Android đã được nghiệm thu.

## Người dùng build và thử

Nếu chưa tạo nhánh mới, tại gốc dự án:

```powershell
git switch -c feature/task-reminders
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

Run trên thiết bị đang dùng, không reset dữ liệu:

1. Chọn Công việc trên thiết bị. Mở chuông/thanh Thông báo → Bật thông báo → cho phép. Bấm Gửi thông báo thử và kiểm tra thanh thông báo.
2. Bấm Cho phép nhắc đúng giờ nếu có → bật quyền trong Android → quay về app, kiểm tra trạng thái. Nếu không cấp, nhắc có thể đến muộn.
3. Tạo một việc tên dễ nhận biết, ngày là hôm nay theo thiết bị, giờ cách giờ thiết bị khoảng 3 phút, bật Nhắc nhở. Với hạn gần này, nhắc vào hạn chót.
4. Kiểm tra có lịch ở Thông báo, đưa app ra nền/tắt màn hình rồi chờ. Bấm thông báo phải mở đúng Chi tiết; Guest có thể cần chọn lại Dùng thử nếu app đã khởi động process mới.
5. Thử hạn xa hơn 15 phút để kiểm tra lead. Sửa hạn, tắt remind, hoàn thành hoặc xóa trước giờ phải hủy lịch cũ.
6. Đổi tài khoản/đăng xuất/chọn API trước giờ; không được hiện nội dung task của phiên/nguồn cũ.
7. Thử từ chối quyền/tắt kênh rồi bật lại; app vẫn lưu task và cập nhật lịch khi quay về.
8. Sau khi có lịch tương lai, người dùng có thể chủ động reboot thiết bị để thử restore. Kiểm tra cả đổi múi giờ và cập nhật app nếu cần nghiệm thu đầy đủ.
9. Refresh nhiều lần không tạo nhiều notification cho cùng một hạn chót đã gửi. Khi quay từ Thông báo về List, từ khóa/nhóm vẫn giữ.

Có thể chạy Android test với thiết bị kết nối:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

Force-stop, hạn chế pin và việc thu hồi quyền exact có thể ngừng/hủy alarm; cần mở lại app để đối chiếu/cập nhật lịch. Không dùng Wipe/Clear data để xử lý quyền. Đây là giới hạn giao nhận của nền tảng cần kiểm tra, không phải bằng chứng task đã lưu bị mất.

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

Agent chưa stage/commit/push/tạo hoặc chuyển nhánh. Không stage token, Firebase config, local.properties, cấu hình IDE/công cụ hoặc cache.

Sau nghiệm thu: tiếp tục Cài đặt giao diện sáng/tối và đối chiếu các màn hình còn lại. Task cloud sync và tính lưu của API mẫu chưa thuộc mốc này.

## Nguồn kỹ thuật

- [Android: Schedule alarms](https://developer.android.com/develop/background-work/services/alarms).
- [Android: Notification runtime permission](https://developer.android.com/develop/ui/compose/notifications/notification-permission).

## Cập nhật sửa lint ngày 20/11/2026

Lần build của người dùng bị chặn bởi `NonObservableLocale` trong `NotificationsScreen.kt`: gọi `Locale.getDefault()` trong composable. Bản sửa đầu dùng `LocalLocale.current.platformLocale` nhưng API này chưa có trong Compose UI `1.10.5` trên compile classpath của dự án, gây lỗi `Unresolved reference`. Kiểm tra nguồn riêng trước đó đã lấy thư viện mới hơn trong cache nên không phát hiện lỗi tương thích này.

Bản sửa hiện tại dùng `LocalConfiguration.current.locales[0]` ở cấp màn hình, truyền locale này vào `SimpleDateFormat`. Compose theo dõi thay đổi cấu hình và cập nhật lại locale; API danh sách locale Android phù hợp với minSdk 24 hiện tại. Giữ nguyên định dạng `dd/MM/yyyy HH:mm`, giao diện và logic nhắc giờ; không tắt lint hoặc thay cấu hình build.

Kiểm tra nguồn riêng đã được điều chỉnh để dùng đúng 137 dependency trên compile classpath trong mô hình lint do Gradle tạo, gồm Compose UI `1.10.5`, thay vì tự chọn phiên bản có trong cache. Biên dịch 71 file Kotlin/Compose với bộ dependency này và header R/BuildConfig đã đạt; đây không phải build APK hoặc chạy lint. Báo cáo XML Gradle hiện có ghi nhận 137 test, không lỗi hoặc bỏ qua; không phải kết quả chạy lại sau sửa locale. Lệnh `assembleDebug testDebugUnitTest lintDebug` chạy lại trong môi trường agent vẫn bị chặn trước task bởi quyền mở file khóa Gradle wrapper (`Access is denied`). Cần chạy lại lệnh này trong Terminal của người dùng để xác nhận build và lint sau sửa. Không xóa file khóa hoặc cache để xử lý lỗi này.
