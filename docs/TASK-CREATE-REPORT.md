# Báo cáo Thêm công việc mới

Ngày: 20/11/2026. Base: Chi tiết commit `b27f9d3`, người dùng xác nhận đã test/push. Nhánh dự kiến: `feature/task-create-api`.

## Lỗi nghiệm thu “Lau nha”

Người dùng báo tạo “Lau nha” nhưng ứng dụng thêm một việc ôn tập chương 4 không liên quan. **Mốc tạo mới chưa đạt nghiệm thu.** Mã trước chấp nhận object task trong response POST mà chưa so sánh nội dung với bản nháp; đây là lỗi xử lý phía ứng dụng. Không phải tên công việc cần được AI nhận diện.

Đã bổ sung đối chiếu tên, nhóm, ngày/giờ, ghi chú, ưu tiên, remind, done và việc nhỏ sau khi trim chuỗi. Chỉ nhận ID từ server; không thay response khác nội dung thành bản nháp để giả đã lưu. Decoder và state dùng chung đều từ chối task không khớp, giữ draft và không phát thông báo thành công. Có thông báo riêng cho trường hợp này.

Hiện tượng phù hợp với mock trả mẫu cố định, nhưng chưa đọc được live response/prototype để kết luận server. Cần đối chiếu POST thật và xác nhận GET trả lại đúng công việc vừa tạo; chưa chuyển sang Thông báo/Cài đặt hoặc chốt task là hoàn thành.

## Phạm vi

Nối nút + ở Home và Danh sách vào màn hình Thêm công việc, thay thông báo chưa sẵn sàng. Tái sử dụng TaskEditor với phần sửa Chi tiết để nhập/kiểm tra dữ liệu nhất quán.

| Điểm vào | Giá trị ban đầu |
|---|---|
| Home → + | Ngày đang chọn ở Home; nhóm Học tập |
| Danh sách Học tập → + | Học tập, ngày hiện tại |
| Danh sách Cuộc sống → + | Cuộc sống, ngày hiện tại |
| Danh sách Tất cả → + | Học tập, ngày hiện tại |

Form có tên, nhóm, ngày/giờ (nhập hoặc picker), ưu tiên, remind, ghi chú và việc nhỏ. Mặc định ưu tiên vừa, remind tắt, chưa hoàn thành, chưa có việc nhỏ. Người dùng có thể bỏ ngày/giờ và thêm/sửa/bỏ việc nhỏ trước khi gửi.

Tên/việc nhỏ không được trống; ngày và giờ phải hợp lệ. Dùng cùng validation với phần sửa; không gửi request khi nhập sai.

## API và giới hạn xác minh

POST `https://share.tryapps.store/mock/teach/tasks`, Authorization Bearer bằng token môn học trong cấu hình cục bộ.

Body dùng các trường/kiểu của công việc hiện có: title, category, date, time, note, priority, remind, done, subtasks. Không gửi id; done của cha luôn false khi tạo. Chuỗi được trim; ngày/giờ bỏ trống gửi null, việc nhỏ có title/done.

Prototype/host không truy cập được trong môi trường agent (DNS). File đề có nút Thêm mới/+ và schema GET; chưa có response POST hoặc tài liệu POST đọc được. Đường dẫn collection/method và payload tạo là lựa chọn triển khai theo mô hình API hiện có, **chưa xác minh hợp đồng POST thật**. Cần đối chiếu response khi thử trên thiết bị, không coi kết quả test là đã gọi API thành công.

Client hỗ trợ ACK 2xx trả object task theo schema GET hoặc object chỉ có id số dương. Nếu chỉ có id, dùng các trường đã gửi cùng ID server trả. Object đầy đủ phải khớp các trường đã gửi sau khi trim chuỗi; không chấp nhận tên/nội dung khác và không ghi đè response bằng draft để che lỗi.

Không tự sinh ID bằng thời gian, số ngẫu nhiên hoặc max(id)+1. Empty response, ACK không có ID, ID sai kiểu/không dương/overflow, JSON sai hoặc ID trùng dữ liệu đã biết hoặc nội dung không khớp bản nháp đều không được báo tạo thành công; giữ task hiện có và bản nháp.

ID trùng thường có thể do mock trả mẫu cố định; không tự đổi ID để che lỗi. Nếu POST thật dùng envelope, Location header hoặc schema khác, cần sửa decoder theo tài liệu/response thực tế.

## State, kết quả gửi và điều hướng

TaskDraft/TaskEditError được chuyển vào model để dùng chung tạo/sửa. TaskEditor tách sang ui/components, giữ nguyên tag mặc định cho test Chi tiết. TaskCreateUiState theo NavBackStackEntry; HomeViewModel tiếp tục điều phối repository và dữ liệu chung.

Trong khi gửi: khóa form/Back/nút gửi, chặn request lặp. Chỉ khi nhận task với ID hợp lệ chưa có và nội dung khớp bản nháp mới thêm vào state, hiện thông báo đã tạo và quay về màn hình mở form. Không reset ngày, tìm kiếm hoặc nhóm đang chọn; công việc có thể nằm ngoài kết quả đang xem, thông báo nêu điều này.

Sau khi tạo thành công, lần quay về Home/List đầu tiên giữ kết quả nếu trước đó đã tải được list. Nếu list chưa từng tải được, ứng dụng vẫn cần GET để lấy bộ dữ liệu đầy đủ; dữ liệu cuối cùng phụ thuộc server. Tải lại sau đó có thể trả mẫu cũ của mock; chưa xác nhận khả năng lưu lâu dài.

Lỗi giữ bản nháp. Với kết quả chưa rõ (timeout, mất kết nối, lỗi server hoặc response thành công không có ID đáng tin), không tự POST lại. Có nút Kiểm tra danh sách; màn hình review mở trên form, Back hoặc + trở lại bản nháp. Chỉ sau khi người dùng xác nhận đã kiểm tra mới cho gửi lại; thao tác xác nhận không tự gửi POST.

HTTP client tắt retryOnConnectionFailure để không tự lặp request ghi. Không thêm cơ chế idempotency không có trong hợp đồng API. Kiểm tra danh sách chỉ hỗ trợ người dùng đối chiếu, không suy đoán hai task trùng tên là cùng một lần tạo.

401/403 và 429 là lỗi từ chối, vẫn giữ draft và cho thử lại theo trạng thái. Đổi tài khoản/đăng xuất hủy request, xóa draft/state và bỏ kết quả trả muộn; không hiện thông báo thành công cho phiên cũ.

Bản nháp giữ khi xoay/recompose với cùng entry nhờ ViewModel; process death chưa lưu bền draft. Back/Hủy khi đã sửa hỏi trước khi bỏ; form chưa sửa quay lại ngay và không POST.

Hủy GET cũ khi mở/gửi form, dùng generation để snapshot list trước đó không ghi đè task vừa tạo. Xem review rồi trở lại form không khởi tạo lại draft.

## Kiểm tra đã chạy

| Kiểm tra trong môi trường agent | Kết quả |
|---|---|
| JUnit chạy riêng bằng Kotlin 2.2.10/thư viện thực có sẵn | 92/92 đạt: 74 hồi quy + 18 test tạo mới/API |
| Biên dịch Kotlin/Compose | 54 file đạt, dùng header R/BuildConfig phục vụ kiểm tra nguồn; chưa tạo APK |
| Biên dịch Android test | 32 test, gồm 9 test tạo mới; đạt biên dịch, chưa chạy trên thiết bị |
| AAPT2 compile resources | Đạt |
| git diff --check, kiểm tra XML và token trong nguồn/tài liệu | Đạt |
| assembleDebug/testDebugUnitTest/lintDebug qua wrapper | Bị chặn trước khi chạy task: Access is denied với file khóa wrapper |
| connectedDebugAndroidTest, UI và POST thật | Chưa chạy trong môi trường agent |

18 unit test tạo mới kiểm tra request POST/field types/null/không id, full response/id-only, ACK không có ID hợp lệ, phân loại lỗi/cancellation, không gửi khi thiếu config/validation, state theo entry, chờ ACK/chặn lặp, giữ draft, ID trùng, đổi tài khoản, GET trả muộn, timeout và review.

9 Android test tạo mới bao phủ Home +/ngày ban đầu, nhóm Cuộc sống/tạo chờ ACK/quay về List, validation, hỏi bỏ draft, review và xác nhận retry, mất phiên giữa POST, cùng entry giữ draft và màn hình 320×480/cỡ chữ 2. Test dùng fake, không gọi Firebase/API thật hoặc thay dữ liệu app người dùng.

Đã thêm unit test qua repository → state tái hiện gửi “Lau nha” nhận “Ôn tập chương 4”, test response có tên đúng nhưng trường khác sai và test state từ chối kết quả sai từ repository khác. Android test thêm ca giữ “Lau nha” trên form, không thêm task sai/không hiện thành công; mới biên dịch, chưa chạy thiết bị.

Còn cảnh báo deprecation createComposeRule của bộ test hiện có. Không đổi SDK, Gradle, AGP, Kotlin, JDK, Firebase BoM, local.properties, đường dẫn máy hoặc cache. Không thêm dependency, reset app/emulator hoặc thao tác Git ghi.

## Build và chạy thử

Người dùng đã tạo nhánh `feature/task-create-api`. Tại gốc dự án:

```powershell
git branch --show-current
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

Sync/Run trên thiết bị đang dùng rồi thử:

1. Home → +: kiểm tra ngày mặc định theo Home. Danh sách → nhóm Cuộc sống → +: kiểm tra nhóm mặc định.
2. Bỏ tên hoặc nhập sai ngày/giờ/việc nhỏ trống → Tạo: báo lỗi, không thoát form.
3. Nhập “Lau nha”, ngày phù hợp, ghi chú và việc nhỏ → Tạo; nếu response trả việc ôn tập khác, phải giữ form/“Lau nha”, báo không khớp, không thêm sai và không báo thành công.
4. Chỉ khi response khớp mới có thông báo thành công, trở về nơi mở; vào đúng ngày/nhóm, xóa tìm kiếm nếu cần để thấy task mới. Mở Chi tiết kiểm tra ID/nội dung thật.
5. Tải lại, mở lại app và đối chiếu dữ liệu server; nếu mock trả mẫu cũ/ID trùng, ghi nhận response thay vì tự đổi ID.
6. Đang nhập rồi Back/Hủy: hỏi bỏ thay đổi. Bỏ: không POST. Xoay màn hình kiểm tra draft; thử bàn phím/cỡ chữ lớn.
7. Tắt mạng khi gửi: draft còn, không tự báo thành công. Kiểm tra Danh sách trước khi cho gửi lại; Back/+ từ review phải giữ draft.
8. Mất phiên khi đang tạo: về Welcome, không Back vào form hoặc nhận thành công muộn.

Có thể chạy bộ Android test khi thiết bị kết nối:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

## Stage, commit, push sau khi chạy đạt

```powershell
git branch --show-current
git add README.md docs/BRANCH-ROADMAP.md docs/TASK-DETAIL-REPORT.md docs/TASK-CREATE-REPORT.md
git add app/src/main app/src/androidTest
git add app/src/test/java/com/example/uth_todo/HomeViewModelTest.kt app/src/test/java/com/example/uth_todo/TaskDetailTest.kt app/src/test/java/com/example/uth_todo/TaskCreateTest.kt
git diff --cached --stat
git diff --cached
git commit -m "feat: add task creation flow"
git push -u origin feature/task-create-api
```

Agent chưa thực thi các lệnh Git ghi. Không stage token, Firebase config, local.properties, cấu hình IDE/công cụ hoặc cache.

Ưu tiên còn lại: xác minh POST lưu và GET đọc lại đúng công việc tự nhập. Chỉ sau khi mốc tạo mới đạt nghiệm thu mới đối chiếu Thông báo/nhắc việc theo giờ và Cài đặt. remind hiện vẫn chỉ là tùy chọn được lưu cùng task.
