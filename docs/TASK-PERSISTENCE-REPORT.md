# Lưu công việc thật và khắc phục lỗi “Lau nha”

Ngày: 20/11/2026. Base `f099f0c`, tiếp tục trên `feature/task-create-api`.

**Mốc đã chốt:** commit `59457c8` trên `feature/task-create-api`. Người dùng đã yêu cầu tiếp tục mốc nhắc giờ; xem [TASK-REMINDERS-REPORT.md](TASK-REMINDERS-REPORT.md). Nội dung bên dưới lưu phạm vi và bằng chứng của mốc lưu thiết bị, chưa phải kết quả kiểm thử quyền/chuông Android ở mốc mới.

## Kết quả triển khai

Bổ sung kho lưu thật trên thiết bị cho flow tạo/đọc/sửa/hoàn thành/việc nhỏ/xóa. Mặc định app dùng kho này để công việc tự nhập không phụ thuộc response mẫu của mock. API bài tập vẫn có đầy đủ repository HTTP ở lựa chọn dữ liệu riêng.

Đã chạy test tạo “Lau nha” vào file DataStore thực, đóng instance, mở lại cùng file và đọc đúng tên/ghi chú/việc nhỏ; kiểm tra nhiều lần sửa/xóa và 20 thao tác tạo đồng thời. Đây là kiểm tra file trên môi trường agent, chưa phải nghiệm thu app Android trên máy người dùng.

## Sửa lỗi build ở test tạo đồng thời trên Windows

Báo cáo Gradle của người dùng có 114 test, 1 lỗi ở concurrentCreatesUseTransactionalIdsAndDoNotLoseTasks. Báo cáo cũ chỉ hiện LOCAL_STORAGE vì ngoại lệ gốc đã bị che. Đã giữ cause của TaskApiFailure và tái hiện cùng test bằng chạy lặp; lỗi xuất hiện ở lượt 14 với AccessDeniedException khi Okio/Windows atomicMove file .preferences_pb.tmp sang file chính.

DeviceTaskStorage nay xếp hàng toàn bộ truy cập DataStore bằng Mutex, gồm cả đọc mode/dữ liệu và mutation. Transaction của DataStore vẫn giữ nguyên; 20 lời gọi create vẫn khởi chạy đồng thời và test vẫn kiểm tra đủ task/ID duy nhất. Không thêm sleep, bỏ test, tắt lỗi hoặc đổi đường dẫn/cache/phiên bản để vượt test.

Sau sửa: nhóm 13 test TaskPersistenceTest đạt 25 lần liên tiếp (325 lượt test, gồm 500 create đồng thời theo từng nhóm 20). Sau đó chạy riêng toàn bộ 114 test JUnit hiện có: đạt 114/114. Không coi kết quả này là task Gradle đã chạy thành công trong môi trường agent; wrapper vẫn bị chặn quyền mở file khóa. Cảnh báo Unsafe ở ảnh không phải nguyên nhân test thất bại.

## Phân biệt hai lựa chọn

| | Công việc trên thiết bị | Dữ liệu mẫu của bài tập |
|---|---|---|
| Mặc định | Có | Không |
| Nguồn | DataStore trong dữ liệu riêng của app | Mock API theo token giảng viên |
| Tạo/sửa/xóa | Giao dịch lưu bền trước khi cập nhật UI | HTTP, chờ ACK và kiểm tra response |
| ID | Kho thiết bị cấp counter trong giao dịch | Server trả ID |
| Tải lại/mở app | Đọc file đã lưu | Gọi API, có thể nhận lại mẫu cố định |
| Mạng/token | Không cần để CRUD | Cần |
| Đồng bộ task giữa máy | Chưa có | Không khẳng định mock là kho đồng bộ cá nhân |

Home, List, Detail và Create hiển thị nguồn hiện tại. **Hồ sơ → Chọn dữ liệu công việc** cho phép đổi. Đổi nguồn trở về Home và bỏ Back stack/form cũ để không dùng nhầm ID giữa nguồn; nếu có bản nháp chưa lưu sẽ hỏi trước khi bỏ. Event điều hướng được tiêu thụ để không lặp khi Compose/Activity tái tạo.

Chọn dữ liệu mẫu không xóa dữ liệu trên thiết bị; chọn lại thiết bị đọc đúng dữ liệu trước đó. Không tự nhập mẫu API vào kho cá nhân, không tự đổi kho sau một request thất bại và không coi ghi thiết bị là API đã lưu thành công.

## Lưu bền và tài khoản

DeviceTaskStorage dùng Preferences DataStore, một JSON task array cho mỗi phạm vi Guest/UID, cùng counter ID và lựa chọn nguồn. UID được băm để tạo key phân vùng. Đây là cách tách dữ liệu trong app, không phải cơ chế đồng bộ hoặc mã hóa task.

- Lần đầu kho thiết bị rỗng; công việc mẫu cũ vẫn xem được bằng lựa chọn API.
- Guest có kho riêng; từng UID có kho riêng trên máy.
- Đăng xuất xóa state trên màn hình/hủy request; không xóa công việc đã lưu.
- Dùng lại đúng Guest hoặc UID trên cùng máy đọc lại dữ liệu cũ.
- Không tự chuyển dữ liệu Guest sang UID khác.
- Không tạo nhánh RTDB task, đổi rules hoặc gửi task cá nhân sang Firebase trong mốc này.
- Gỡ cài đặt/xóa dữ liệu app có thể làm mất kho thiết bị; không cần thực hiện thao tác này để cập nhật.

Mỗi mutation đọc/đổi/ghi task và counter trong cùng edit transaction. Tạo/sửa/xóa chỉ phản ánh thành công sau khi việc ghi hoàn tất. Xóa không làm counter lùi và tái dùng ID. Không phụ thuộc số ID/date/title của mẫu API.

JSON hỏng hoặc lỗi đọc/ghi trả LOCAL_STORAGE, giữ file hiện có; không chuyển thành danh sách rỗng hoặc tự xóa cache. Không tự sửa dữ liệu bằng mẫu mạng. Dataset nhỏ của bài tập phù hợp cách lưu này; khi mở rộng dữ liệu lớn/quan hệ cần đánh giá kho dữ liệu chuyên dụng.

## Factory và vòng đời

AndroidTaskRepository giữ một DataStore cho file personal_tasks trong process, không tạo lại khi xoay Activity. File nằm trong vùng dữ liệu app theo Context, không hardcode đường dẫn máy.

TaskPreferencesStoreFactory dùng PreferenceDataStoreFactory với OkioStorage/PreferencesSerializer có sẵn từ thư viện hiện tại. Cùng factory dùng trong app và test ghi file thật; không thêm dependency hoặc sửa cache. Serializer tương thích định dạng Preferences.

SessionTaskRepository chụp scope trước điểm suspend và chọn đúng repository theo mode được lưu. HomeViewModel bind session, hủy request và dùng generation để kết quả cũ không ghi đè phiên/nguồn mới. Đổi nguồn xóa task/detail/draft trong state rồi tải từ nguồn mới, không xóa dataset đã lưu.

## Giới hạn API được giữ nguyên

Code HTTP, token cấu hình, các endpoint GET/POST/PATCH/DELETE và guard response không khớp vẫn giữ. Khi chọn API, tạo “Lau nha” mà server trả việc khác phải giữ draft/báo không khớp, không lưu âm thầm vào thiết bị.

Prototype/live API vẫn không đọc được từ môi trường agent; chưa xác minh server có persist đúng POST hay không. Mốc này giải quyết việc sử dụng app với kho thật trên thiết bị, không thay đổi hoặc khẳng định đã sửa server mock.

## Kiểm tra

| Kiểm tra | Kết quả trong môi trường agent |
|---|---|
| Kotlin 2.2.10/JUnit chạy riêng | 114/114 đạt: toàn bộ test JUnit hiện có, gồm 13 lưu bền/chọn nguồn |
| File DataStore thực | Tạo/đóng/mở lại, nhiều lần sửa/xóa, 20 create đồng thời; nhóm 13 test đạt 25 lượt liên tiếp |
| Biên dịch Kotlin/Compose | 61 file đạt, header R/BuildConfig dùng để kiểm tra nguồn; chưa tạo APK |
| Biên dịch Android test | 36 test, gồm 4 test mới cho lưu thiết bị/chuyển nguồn; chưa chạy thiết bị |
| AAPT2 compile resources | Đạt |
| git diff --check, XML/kiểm tra token | Đạt |
| assembleDebug/testDebugUnitTest/lintDebug | Bị chặn quyền mở file khóa Gradle wrapper trước khi chạy task |
| UI Android/connectedDebugAndroidTest | Chưa chạy trong môi trường agent |
| Live API | Không xác minh được, không ghi request vào dataset người dùng |

13 test mới bao phủ file thật/reopen/CRUD/counter, tạo đồng thời, Guest/UID, mode lưu bền, không fallback API, JSON hỏng, signed out, ID trùng số giữa nguồn, VM tạo-refresh-detail, lỗi ghi giữ draft, đổi nguồn/xóa draft và kết quả API trả muộn. Không dùng lớp diagnostic thay nguồn production trong lần kiểm tra cuối.

4 Android test mới kiểm tra tạo “Lau nha” → refresh/detail đúng nội dung, đăng xuất/dùng lại Guest giữ task, đổi nguồn và trở lại kho thiết bị, sửa/xóa rồi refresh. Test dùng DataStore trong bộ nhớ để cách ly dữ liệu app người dùng. Việc đóng/mở process và Android file system vẫn cần thử thủ công.

Còn cảnh báo deprecation createComposeRule có sẵn. Giữ Gradle/SDK/AGP/Kotlin/JDK/Firebase BoM, đường dẫn máy, local.properties, cache và thiết bị. Không reset app/emulator, tạo/chuyển nhánh, stage, commit hoặc push bằng agent.

## Người dùng kiểm tra trên app

Trên nhánh hiện tại:

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

Sync/Run rồi thực hiện:

1. Xác nhận dòng Công việc trên thiết bị. Nếu đang ở mẫu: Hồ sơ → Chọn dữ liệu công việc → Công việc trên thiết bị.
2. Tạo “Lau nha”, chọn ngày phù hợp, thêm ghi chú/việc nhỏ. Nếu Home đang chọn ngày khác, vào Danh sách → Tất cả và xóa tìm kiếm để xem.
3. Bấm tải lại → mở Chi tiết; tên và các trường phải đúng nội dung đã lưu.
4. Đóng app, mở lại bằng biểu tượng/Run; dùng lại cùng tài khoản hoặc Guest, kiểm tra còn “Lau nha”. Không Clear data hoặc Wipe device.
5. Sửa tên/ghi chú, tick việc nhỏ/done, tải lại và mở lại app; trạng thái phải giữ đúng.
6. Xóa có xác nhận → tải lại/mở app; công việc không xuất hiện trở lại.
7. Tắt mạng vẫn tạo/sửa/xóa được ở kho thiết bị.
8. Chuyển sang dữ liệu mẫu, xem label và nội dung API; chuyển lại thiết bị vẫn còn task cá nhân chưa xóa.
9. Đổi tài khoản phải tách dữ liệu; dùng lại tài khoản trước phải thấy dữ liệu trước đó trên máy.

Muốn chạy test Android, giữ thiết bị kết nối:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

## Stage, commit, push sau khi chạy đạt

```powershell
git add README.md docs/BRANCH-ROADMAP.md docs/TASK-CREATE-REPORT.md docs/TASK-PERSISTENCE-REPORT.md
git add app/src/main app/src/androidTest
git add app/src/test/java/com/example/uth_todo/TaskPersistenceTest.kt
git diff --cached --stat
git diff --cached
git commit -m "fix: persist tasks and serialize device storage access"
git push -u origin feature/task-create-api
```

Không stage token, Firebase config, local.properties, cấu hình IDE/công cụ hoặc cache. Agent chưa thực thi các lệnh Git ghi.

Sau khi người dùng nghiệm thu, tiếp tục phần Thông báo/nhắc theo giờ và Cài đặt. Task cloud sync và giới hạn server mock vẫn là các phạm vi riêng chưa hoàn tất.

## Tài liệu kỹ thuật

- [Android DataStore](https://developer.android.com/topic/libraries/architecture/datastore).
- [PreferenceDataStoreFactory](https://developer.android.com/reference/kotlin/androidx/datastore/preferences/core/PreferenceDataStoreFactory).
