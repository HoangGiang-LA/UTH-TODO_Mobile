# Báo cáo flow danh sách công việc

Ngày: 20/11/2026. Base: BTVN 04, commit `1fbfbf0`. Nhánh dự kiến: `feature/task-list-api`.

**Nghiệm thu sau triển khai:** người dùng xác nhận đã test và push hoàn tất; commit `4d0d14e` trên `feature/task-list-api`. Nội dung bên dưới ghi lại phạm vi/kết quả tại mốc Danh sách. Flow Chi tiết kế tiếp được ghi riêng trong [TASK-DETAIL-REPORT.md](TASK-DETAIL-REPORT.md).

## Phạm vi

Tách danh sách xem nhanh của Home thành màn hình “Danh sách” riêng, đối chiếu ảnh trong `Khởi tạo App Mobile(Task).docx`. Giữ repository, API, Firebase và giao diện Home hiện có.

| Điểm vào | Bộ lọc ban đầu |
|---|---|
| Home → Xem tất cả | Tất cả |
| Thanh điều hướng → Danh sách | Tất cả |
| Thẻ Học tập | Học tập |
| Thẻ Cuộc sống | Cuộc sống |

Thanh điều hướng đánh dấu đúng Danh sách. Back hệ thống hoặc nút Home trở lại Home, không thêm một Home mới vào lịch sử. Hồ sơ mở từ danh sách; Back trở lại danh sách đang xem.

## Giao diện và dữ liệu

- Tiêu đề, số việc cần làm, nút tải lại, tìm kiếm và bộ lọc Tất cả/Học tập/Cuộc sống.
- Tìm kiếm không phân biệt dấu tiếng Việt; nút xóa chỉ xóa từ khóa, giữ bộ lọc.
- Nhóm Cần làm/Đã xong lấy từ kết quả lọc/tìm kiếm. Số cần làm là số trong kết quả đang hiển thị.
- Mỗi việc có ngày/giờ thật: Hôm nay, Ngày mai hoặc ngày cụ thể; không sửa ngày dữ liệu mẫu để giả thành hôm nay.
- Header và nội dung cùng cuộn; thanh điều hướng nằm dưới. Bố cục cho phép xuống dòng khi màn hình hẹp/cỡ chữ lớn.
- Phân biệt tải lần đầu, tải lỗi/thử lại, toàn bộ danh sách rỗng và không có kết quả tìm kiếm. Khi tải lại lỗi, giữ dữ liệu đã có.
- Bấm công việc mở hộp xem nhanh nội dung từ list; chưa tải chi tiết riêng theo ID.

Danh sách dùng chung HomeViewModel với Home, không sao chép task sang một kho khác. Khi vào màn hình hoặc tải lại, gọi GET qua repository hiện có. Request đang chạy được kiểm soát để tránh chồng thao tác cập nhật.

Checkbox gửi PATCH và chờ ACK 2xx rồi mới đổi trạng thái. Lỗi giữ dữ liệu cũ và hiện thông báo. Sau thành công, công việc chuyển nhóm và tiến độ/thẻ đếm ở Home cùng cập nhật.

| API | Sử dụng |
|---|---|
| GET `/mock/teach/tasks` | Tải danh sách |
| PATCH `/mock/teach/tasks/{id}` | Cập nhật `done` |

Tìm kiếm và lọc chạy ở ứng dụng trên dữ liệu GET, không tự thêm query hoặc endpoint. API mock dùng bộ dữ liệu chung theo token môn học, chưa phải kho riêng theo Firebase UID. Tải lại có thể trả về mẫu của mock; chưa xác minh khả năng lưu lâu dài.

## State và điều hướng

TaskListViewModel chỉ giữ từ khóa và nhóm lọc bằng SavedStateHandle, nhận nhóm ban đầu từ route. Không lưu toàn bộ task vào saved state. NavBackStackEntry quản lý vòng đời của ViewModel này; dữ liệu task vẫn do HomeViewModel quản lý.

LazyListState lưu vị trí cuộn. Bàn phím có hành động Search; điều hướng đóng focus. Đi Hồ sơ rồi Back giữ từ khóa/bộ lọc và trạng thái cuộn. Khi đã Back/Home khỏi danh sách, lần mở mới nhận nhóm được chọn từ Home.

Đăng xuất hoặc phiên không còn hợp lệ sẽ xóa dữ liệu dùng chung và quay về màn hình chào, không cho Back vào Home/Danh sách/Hồ sơ được bảo vệ. Không thay đổi cơ chế đăng nhập.

## Cấu trúc và OOP

Các đường dẫn Kotlin tính từ `app/src/main/java/com/example/uth_todo/`.

| Thành phần | Thay đổi |
|---|---|
| `navigation/AppDestination.kt` | Route danh sách, tham số nhóm và kiểm tra route cần phiên |
| `navigation/UthTodoApp.kt` | Đăng ký route, bind state và điều hướng |
| `ui/tasklist/TaskListViewModel.kt` | State tìm kiếm/bộ lọc, tách kết quả Cần làm/Đã xong |
| `ui/tasklist/TaskListScreen.kt` | Màn hình danh sách, tìm kiếm, nhóm việc và trạng thái tải |
| `ui/components/TaskBottomBar.kt` | Thanh điều hướng dùng chung Home/Danh sách |
| `ui/home/` | Điểm vào màn hình riêng, dùng chung thẻ/xem nhanh, bỏ sheet danh sách |
| `data/tasks/TaskDates.kt` | Tính ngày kế tiếp đúng qua tháng/năm/nhuận |
| `res/values/strings.xml` | Chuỗi giao diện mới |
| `src/test/.../TaskListViewModelTest.kt` | 6 unit test mới |
| `src/androidTest/.../OnboardingFlowTest.kt` | Thêm 6 test điều hướng/tương tác |

TaskRepository tiếp tục là abstraction cho dữ liệu và cho phép inject fake trong test. TaskListUiState/TaskListContent biểu diễn dữ liệu bất biến; composable nhận state/callback để tách giao diện khỏi xử lý trạng thái. Enum TaskFilter/TaskTab biểu diễn nhóm và tab rõ ràng.

## Kết quả kiểm tra trong môi trường agent

| Kiểm tra | Kết quả |
|---|---|
| JUnit qua trình biên dịch Kotlin riêng, thư viện có sẵn | 57/57 đạt: 12 Auth + 14 Firebase + 13 TaskApi + 12 HomeViewModel + 6 TaskList |
| Biên dịch nguồn Kotlin/Compose với thư viện thực có sẵn | 49 file đạt; dùng header R/BuildConfig phục vụ kiểm tra nguồn, chưa tạo APK |
| Biên dịch mã Android test | Đạt; tổng 15 test, trong đó 6 test mới; chưa chạy trên thiết bị |
| AAPT2 compile resources | Đạt |
| git diff --check | Đạt |
| Wrapper assembleDebug/testDebugUnitTest/lintDebug | Bị chặn trước khi chạy task: không mở được file khóa Gradle wrapper, Access is denied |
| Thử màn hình mới/API thật trên thiết bị | Chưa chạy, cần người dùng nghiệm thu |

6 unit test mới kiểm tra khôi phục từ khóa/nhóm, nhóm không hợp lệ, xóa tìm kiếm, đếm theo kết quả, chuyển nhóm sau dữ liệu ACK và ngày mai qua tháng/năm/nhuận.

6 Android test mới bao phủ thẻ nhóm, Xem tất cả/thanh Danh sách và Back, tìm kiếm/PATCH chờ ACK/Home đồng bộ, Hồ sơ rồi Back giữ state, phiên hết hạn khi đang ở Danh sách, màn hình 320×480/cỡ chữ 2. Các test dùng repository giả có kiểm soát; compile đạt không chứng minh thiết bị hoặc live API đã chạy đạt. Chưa thực thi test tái tạo process/Activity trên thiết bị.

Còn cảnh báo deprecation createComposeRule từ bộ test hiện có. Không đổi thư viện hoặc framework test để xử lý cảnh báo này.

Kết quả người dùng đã build/chạy thành công BTVN 04 là nghiệm thu Home trước mốc này. Cần build/chạy lại cho màn hình danh sách mới.

Không đổi Gradle, SDK, AGP, Kotlin, JDK, Firebase BoM, đường dẫn máy, cấu hình riêng hoặc xóa cache. Không stage/commit/push/chuyển nhánh, không reset dữ liệu app/emulator.

## Người dùng chạy và kiểm tra

Tại thư mục gốc dự án, nếu chưa tạo nhánh này:

```powershell
git switch -c feature/task-list-api
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

Sync/Run bằng Android Studio với thiết bị đang hoạt động, rồi kiểm tra:

1. Home → Xem tất cả/thanh Danh sách hiển thị Tất cả; hai thẻ nhóm mở đúng nhóm.
2. Tìm từ khóa có/không dấu, đổi nhóm, xóa từ khóa; số cần làm và hai nhóm khớp kết quả.
3. Đánh dấu hoàn thành/chưa xong, chờ request; kiểm tra chuyển nhóm và tiến độ ở Home sau khi trở về.
4. Danh sách → Hồ sơ → Back giữ từ khóa/bộ lọc; Back/Home từ danh sách về Home, mở lại lấy đúng nhóm mới.
5. Xoay màn hình, mở bàn phím, thử màn hình hẹp/cỡ chữ lớn; cuộn được tới việc cuối và dùng thanh điều hướng.
6. Đăng xuất hoặc thử phiên không hợp lệ; không Back vào màn hình được bảo vệ.
7. Thử tải lại khi mạng lỗi rồi bật lại mạng để Retry; thông báo và dữ liệu giữ lại đúng.

Nếu muốn chạy bộ Android test, giữ thiết bị kết nối rồi chạy:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

Test sử dụng dữ liệu giả trong test, không cần đổi token hoặc reset app. Kết quả thử cấu hình hiển thị/khôi phục thực tế vẫn cần kiểm tra trên thiết bị.

## Chốt mốc và bước sau

Sau khi build/chạy đạt, người dùng xem diff rồi commit/push theo [BRANCH-ROADMAP.md](BRANCH-ROADMAP.md). Không stage cấu hình IDE/công cụ, token, Firebase config hoặc cache.

Mốc tiếp theo là flow chi tiết: GET theo ID và xử lý sửa/xóa theo hợp đồng API. Nút thêm mới vẫn hiện thông báo chưa sẵn sàng; màn hình danh sách hiện tại chưa triển khai CRUD đầy đủ.
