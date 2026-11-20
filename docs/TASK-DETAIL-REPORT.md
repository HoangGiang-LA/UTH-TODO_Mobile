# Báo cáo flow Chi tiết công việc

Ngày: 20/11/2026. Base: Danh sách commit `4d0d14e`, người dùng đã test/push. Nhánh dự kiến: `feature/task-detail-api`.

**Nghiệm thu sau triển khai:** người dùng xác nhận đã test và push thành công; commit `b27f9d3` trên `feature/task-detail-api`. Nội dung bên dưới lưu kết quả tại mốc Chi tiết. Mốc Thêm công việc tiếp theo được ghi trong [TASK-CREATE-REPORT.md](TASK-CREATE-REPORT.md).

## Phạm vi đã triển khai

Đối chiếu ảnh “Dựng Flow Chi tiết (UI + API)” trong `Khởi tạo App Mobile(Task).docx`: tên, nhóm/ưu tiên, hạn chót, nhắc nhở, ghi chú, tiến độ việc nhỏ, nút Sửa, hoàn thành và xóa.

- Bấm thẻ công việc ở Home hoặc Danh sách mở màn hình Chi tiết riêng, tải bằng GET theo ID.
- Không dùng object list để giả tải chi tiết thành công. Có loading, lỗi/thử lại, thông báo không tìm thấy và nút tải lại.
- Hiển thị đúng dữ liệu ngày/giờ, ghi chú, ưu tiên và remind. Không tự đổi ngày mẫu thành hôm nay.
- Sửa tên, nhóm, ngày/giờ, ưu tiên, tùy chọn nhắc, ghi chú; có DatePicker/TimePicker hoặc nhập ngày/giờ.
- Thêm/sửa/bỏ việc nhỏ trong form rồi Lưu; checkbox trong Chi tiết cập nhật riêng mảng subtasks.
- Tiến độ việc nhỏ dựa trên số done. Hoàn thành hết việc nhỏ không tự đổi done của cha.
- Đánh dấu hoàn thành/chưa hoàn thành cho công việc cha.
- Xóa cần xác nhận; lỗi giữ công việc, chỉ đóng Chi tiết/bỏ khỏi list sau ACK 2xx.

Tên và tên việc nhỏ không được trống. Kiểm tra ngày thực tế theo yyyy-MM-dd, giờ HH:mm, nhóm và ưu tiên hợp lệ. Có thể xóa ngày/giờ, gửi null rõ ràng; có thể bỏ toàn bộ việc nhỏ, gửi mảng rỗng.

## API và phần cần xác minh trên dịch vụ thật

| Hành động | Request |
|---|---|
| Tải chi tiết | GET `https://share.tryapps.store/mock/teach/tasks/{id}` |
| Lưu thông tin | PATCH cùng đường dẫn ID |
| Hoàn thành | PATCH `{"done":true}` hoặc false |
| Đổi checkbox việc nhỏ | PATCH `{"subtasks":[{"title":"…","done":true}]}` |
| Xóa | DELETE cùng đường dẫn ID |

Ảnh đề xác nhận ba method GET/PATCH/DELETE theo ID, object GET có title/category/date/time/note/priority/remind/done/subtasks. Decoder yêu cầu object và ID khớp yêu cầu; JSON sai hoặc mảng/envelope không được coi là tải thành công. Subtask theo schema đang có title/done, không tự đặt ID riêng.

Payload sửa lấy tên trường/kiểu từ GET: title, category, date, time, note, priority, remind, done, subtasks; không gửi id trong body. Đây là lựa chọn triển khai từ schema đọc, chưa được đối chiếu tài liệu PATCH/live response vì prototype và host không truy cập được trong môi trường agent. Cần thử trên mock thật; nếu server yêu cầu payload khác, sửa theo hợp đồng thực tế.

Authorization tiếp tục dùng token mock của đề qua cấu hình cục bộ. Không thêm API con cho subtask hoặc dùng Firebase ID token thay token mock.

401/403, 404, 429, timeout, lỗi mạng/server và response sai được phân biệt. Không in header/body/token vào log. PATCH/DELETE chấp nhận ACK 2xx, không yêu cầu server trả nguyên task. Sau ACK, giao diện áp dụng các trường vừa gửi; chưa khẳng định server đã lưu lâu dài hoặc không chuẩn hóa trường.

Mock có thể trả mẫu cố định hoặc không giữ thay đổi. Nếu GET một ID trả ID khác, ứng dụng báo response không hợp lệ để tránh hiển thị nhầm công việc; cần đối chiếu hành vi mock trong trường hợp này.

## Đồng bộ và vòng đời

HomeViewModel tiếp tục quản lý dữ liệu chung cho Home/Danh sách/Chi tiết để không có ba bản task độc lập. TaskDetailUiState và TaskDraft là các giá trị bất biến; composable gửi callback, repository chịu trách nhiệm HTTP/JSON.

Lưu/hoàn thành/xóa chỉ cập nhật state sau ACK. Chặn thao tác lặp khi request đang chạy. Bản nháp được giữ khi lưu lỗi hoặc xoay màn hình nhờ ViewModel; Back/Hủy khi có thay đổi hỏi trước khi bỏ. Khi đang gửi, khóa Back và form cho tới kết quả hoặc timeout.

Sau GET hoặc mutation Chi tiết thành công, lần quay về màn hình list/Home đầu tiên giữ kết quả hiện tại, không tự GET ngay để ghi đè kết quả ACK. Người dùng vẫn tải lại được; lần vào màn hình khác sau đó có thể GET theo cơ chế hiện có.

Từ Danh sách → Chi tiết → Back giữ từ khóa/bộ lọc/vị trí cuộn của Danh sách. Chi tiết → Back quay về đúng nơi mở, không tạo Home mới.

Route truyền ID số, không truyền toàn bộ object. Cùng NavBackStackEntry không tải lại đè bản nháp khi Compose tái tạo. Nếu process bị kết thúc, task được tải lại theo route; bản nháp Chi tiết chưa được lưu bền qua process death.

Hủy/loại kết quả list GET cũ khi bắt đầu đọc Chi tiết để tránh snapshot cũ ghi đè dữ liệu mới, kể cả route được khôi phục lúc app mở. Back trước khi GET Chi tiết xong cho phép tải list lại. Đổi tài khoản/đăng xuất hủy request, xóa task/bản nháp và bỏ kết quả trả muộn của phiên cũ.

## Kết quả kiểm tra

| Kiểm tra trong môi trường agent | Kết quả |
|---|---|
| JUnit chạy riêng với Kotlin 2.2.10 và thư viện cache thực | 74/74 đạt: 57 test hồi quy + 17 test Chi tiết/API mới |
| Biên dịch Kotlin/Compose với thư viện có sẵn | 50 file đạt; dùng header R/BuildConfig phục vụ kiểm tra nguồn, không tạo APK |
| Biên dịch Android test | 23 test, trong đó 8 test Chi tiết mới; đạt biên dịch, chưa chạy trên thiết bị |
| AAPT2 compile resources | Đạt |
| git diff --check và kiểm tra XML/token trong nguồn/tài liệu | Đạt |
| assembleDebug/testDebugUnitTest/lintDebug qua wrapper | Bị chặn trước khi chạy task: file khóa wrapper Access is denied |
| UI, connectedDebugAndroidTest, live API | Chưa chạy; chờ người dùng kiểm tra |

17 unit test mới bao phủ GET object/ID/error, field types/null/subtasks của PATCH, DELETE, đọc dữ liệu mới, giữ bản nháp, validation, ACK, thêm/sửa/bỏ việc nhỏ, checkbox, xóa lỗi/thành công, timeout, kết quả trả muộn theo entry/tài khoản và xung đột list/Detail khi khôi phục route.

8 Android test mới bao phủ Home → Detail, List → Detail → Back giữ tìm kiếm/nhóm, lưu chờ ACK, xóa có xác nhận/chờ ACK, bỏ bản nháp, GET lỗi/thử lại, mất phiên khi đang sửa và màn hình 320×480/cỡ chữ 2. Các test dùng fake, không gọi Firebase/API thật hoặc xóa dữ liệu app người dùng.

Còn cảnh báo deprecation createComposeRule có sẵn. Không đổi Gradle, SDK, AGP, Kotlin, JDK, Firebase BoM, đường dẫn máy, local.properties hoặc cache để vượt lỗi wrapper. Không thêm dependency.

## Người dùng build và thử

Nếu chưa tạo nhánh mới, tại gốc dự án:

```powershell
git switch -c feature/task-detail-api
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

Sync/Run trên thiết bị hiện có, không reset app:

1. Từ Home/Danh sách mở một việc; kiểm tra loading rồi dữ liệu chi tiết đúng ID, Back đúng màn hình trước.
2. Ở Danh sách nhập tìm kiếm/chọn nhóm; mở Chi tiết rồi Back, kiểm tra còn state.
3. Sửa tên, nhóm, ghi chú, ngày/giờ và ưu tiên → Lưu; kiểm tra Chi tiết, Home và Danh sách khớp sau ACK.
4. Thêm, sửa, bỏ việc nhỏ trong form → Lưu; tick checkbox, kiểm tra tiến độ và không tự đổi done cha.
5. Đánh dấu hoàn thành/chưa hoàn thành, quay về Home kiểm tra tiến độ trong đúng ngày.
6. Bấm Xóa → Hủy: còn task. Xóa → xác nhận: chờ request, quay về đúng màn trước và task biến mất.
7. Tắt mạng trước khi Lưu/Xóa: giữ bản nháp/task và hiện lỗi; bật mạng rồi thử lại. Timeout có thể chưa rõ server đã xử lý hay chưa, tải lại để đối chiếu trước thao tác tiếp.
8. Sửa rồi Back/Hủy: có hỏi bỏ thay đổi. Xoay màn hình khi đang sửa; thử bàn phím, cỡ chữ lớn và cuộn tới các nút.
9. Đăng xuất hoặc mất phiên trong Chi tiết: không Back vào màn hình được bảo vệ.

Có thể chạy bộ test Android với thiết bị kết nối:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

## Stage, commit, push sau khi chạy đạt

```powershell
git branch --show-current
git add README.md docs/BRANCH-ROADMAP.md docs/TASK-LIST-REPORT.md docs/TASK-DETAIL-REPORT.md
git add app/src/main app/src/androidTest
git add app/src/test/java/com/example/uth_todo/HomeViewModelTest.kt app/src/test/java/com/example/uth_todo/TaskDetailTest.kt
git diff --cached --stat
git diff --cached
git commit -m "feat: implement task detail editing and deletion"
git push -u origin feature/task-detail-api
```

Agent chưa stage/commit/push/tạo hoặc chuyển nhánh. Giữ token, Firebase config, local.properties, cấu hình IDE/công cụ và cache ngoài commit.

## Tiếp tục hoàn thiện app

Mốc tiếp theo: Thêm công việc mới theo POST và màn hình trong đề. Tùy chọn remind hiện được lưu qua API, chưa lập lịch thông báo theo giờ; phần này tiếp tục ở flow Thông báo. Nút thêm công việc mới vẫn hiện thông báo chưa sẵn sàng.
