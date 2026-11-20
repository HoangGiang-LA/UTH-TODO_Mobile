# BTVN 04 — Trang chủ UI/API

Cập nhật 20/11/2026, kế thừa BTVN 03 commit `028f378`.

**Trạng thái chốt theo người dùng:** đã cung cấp ảnh BUILD SUCCESSFUL cho assembleDebug/testDebugUnitTest/lintDebug (2 phút 54 giây), sau đó xác nhận đã build và chạy thử thành công hết các luồng đã kiểm tra. Sẵn sàng commit/push phần Home BTVN 04 trên `feature/btvn04-home-api`. Agent đã chạy riêng 51 unit test, biên dịch nguồn 46 file Kotlin/Compose và resources; không tự vận hành thiết bị hoặc xác minh từng API live. Chưa stage/commit/push hay tạo/chuyển nhánh bằng agent.

## Căn cứ và phạm vi

Đọc file đề cục bộ `Khởi tạo App Mobile(Task).docx`, gồm ảnh Home, ảnh flow danh sách/chi tiết và ảnh response GET thành công. Figma/prototype không truy cập được qua công cụ; download bản đính kèm cũng bị DNS nên không dùng để khẳng định đã đối chiếu pixel. Ảnh đầu vào và dữ liệu truy cập chỉ lưu trong thư mục build/check được Git bỏ qua, không đưa vào source.

Đợt này tập trung Home UI/API danh sách và thống kê theo lộ trình. Có danh sách xem nhanh dạng bottom sheet để các thẻ nhóm/Xem tất cả có điểm đến hữu ích, có tìm kiếm/lọc, và dialog xem dữ liệu của một việc. Đây chưa phải màn hình flow danh sách/chi tiết độc lập đầy đủ theo các ảnh kế tiếp. Không thêm POST, sửa nội dung, xóa việc hoặc CRUD việc nhỏ. Nút thêm hiển thị thông báo chưa sẵn sàng, không giả tạo công việc thành công.

## Người dùng nhìn thấy

- Avatar/tên lấy từ phiên Auth và tên RTDB đã đồng bộ; bấm avatar hoặc Cài đặt mở Hồ sơ cũ.
- Dòng ngày chọn được bằng DatePicker. Mặc định là ngày thực tế trên thiết bị.
- Thẻ tiến độ, vòng tròn %, hai thẻ Học tập/Cuộc sống, danh sách việc trong ngày, footer và nút thêm theo bố cục ảnh mẫu.
- Home xem tối đa 8 việc; Xem tất cả mở toàn bộ dữ liệu đã tải. Tìm kiếm không cần dấu, lọc nhóm và chia Cần làm/Đã xong.
- Checkbox chờ PATCH thành công rồi mới cập nhật trạng thái/tiến độ. Khi đang gửi chỉ có một thay đổi được xử lý, ngăn bấm lặp.
- Lỗi tải có thử lại; tải lại giữ dữ liệu cũ nếu lỗi. JSON lỗi không trở thành empty state giả.
- Chuông/Thông báo mở card quyền FCM đã có. Banner Remote Config, Profile, Google/email/Guest và Back stack được giữ.

Tất cả chữ UI mới đặt trong resources. Nội dung Home/dialog có cuộn, filter chips cuộn ngang và metadata của dòng task có thể xuống hàng khi font lớn. Các biểu tượng được vẽ bằng Compose thay cho asset gốc chưa lấy được; chưa tự kiểm tra hiển thị trên thiết bị.

## API và cấu hình

| Hành động | Request |
|---|---|
| Tải công việc | GET `https://share.tryapps.store/mock/teach/tasks` |
| Đổi hoàn thành | PATCH `https://share.tryapps.store/mock/teach/tasks/{id}`, JSON `{"done":true}` hoặc false |
| Xác thực | Authorization Bearer bằng token mock được cấp trong đề, độc lập Firebase |

Ảnh đề xác nhận GET trả **mảng trực tiếp**, với id số, title/category/date/time/done và các trường note/priority/remind/subtasks. Decoder kiểm tra kiểu, ngày/giờ, ID dương duy nhất; optional/null note/date/time/remind/subtasks được xử lý rõ. Category chưa biết được gán Khác, không tự gán Học tập hoặc Cuộc sống. Không suy diễn envelope data/tasks chưa có trong đề.

Ảnh chi tiết có PATCH cùng đường dẫn ID; payload done được dựng từ trường trạng thái của task. Client nhận mọi ACK 2xx, không phụ thuộc hình dạng response PATCH chưa được xác minh live. Tính tương thích request/response với server thật vẫn phải thử.

Đã tạo `api.local.properties` trên máy hiện tại từ token trong đề; file được Git bỏ qua. Không sửa `local.properties`, SDK path, JDK, Gradle, AGP, Kotlin, min/target SDK, versionName, BoM hoặc cấu hình Firebase. Chỉ thêm OkHttp 4.12.0/Gson 2.11.0 và BuildConfig field phục vụ API.

Khi clone sang máy khác:

```powershell
Copy-Item api.local.properties.example api.local.properties
```

Mở file và điền giá trị MOCK_API_TOKEN từ đề rồi Sync/build. Có thể dùng biến môi trường UTH_MOCK_API_TOKEN; nó được ưu tiên. Token không được log, không ghi vào README/report/test fixture hoặc file template. Android client cần gửi token nên APK chứa giá trị biên dịch; đây là key cho mock môn học, không phải thiết kế giữ bí mật khóa máy chủ production.

HTTP dùng TLS mặc định, không đổi certificate policy, không follow redirect để tránh gửi header sai host. Callback bất đồng bộ đọc/đóng body với giới hạn 1 MiB, cancellation hủy Call; JSON được parse ngoài main thread. Có connect/read/write/call timeout và thời hạn ViewModel. Phân biệt thiếu config, 401/403, 429, timeout, network, server và JSON lỗi.

## Thống kê và ngày

| Nội dung | Cách tính |
|---|---|
| Tiến độ | Task có date bằng ngày đang chọn; completed/total |
| Phần trăm | completed × 100 / total; tổng 0 thì 0%, không chia 0 |
| Hai nhóm | Tất cả task chưa done của study/life, kể cả ngày khác |
| Việc trong ngày | Chọn đúng date, sắp theo time rồi id |
| Danh sách đầy đủ | Cần làm trước Đã xong, theo lịch; có lọc và search |
| Việc chưa đặt hạn | Có trong danh sách đầy đủ, không gán vào hôm nay |

Nhóm đếm toàn bộ pending để khớp ý nghĩa ảnh mẫu, trong đó thẻ tiến độ chỉ tính ngày hiện tại. Mẫu fixture 6 task có 4 task trong ngày, 2 done, tiến độ 50%, mỗi nhóm 2 pending tính cả ngày sau.

Không hardcode ngày trong ảnh thành hôm nay. Nếu live API trả dữ liệu mẫu có ngày cố định, chọn ngày ấy bằng header hoặc Xem tất cả. Thay đổi sau PATCH lưu trong state hiện tại sau ACK; khi GET lại, dữ liệu server là nguồn cập nhật. Nếu dịch vụ mock luôn trả sample cũ, trạng thái có thể quay về sample; chưa có bằng chứng persistence lâu dài.

API mock dùng dữ liệu chung theo token, chưa phải task riêng theo Firebase UID. Dù vậy ViewModel vẫn hủy request, xóa dữ liệu và bỏ kết quả muộn khi đổi/đăng xuất phiên để không giữ state của phiên trước.

## OOP và file

| Thành phần | Trách nhiệm |
|---|---|
| model/TodoTask.kt | Data class task/subtask/summary; enum nhóm/priority/lỗi/filter |
| data/tasks/TaskRepository.kt | Hợp đồng suspend load/setDone, tách UI khỏi HTTP |
| MockTaskRepository.kt | HTTP transport, bearer, status/timeout/cancellation và PATCH |
| TaskJsonDecoder.kt | Parse và kiểm tra contract bằng JSON tree, không reflection model Kotlin |
| TaskDates.kt, TaskQueries.kt | Ngày, thống kê, sắp xếp, lọc/tìm kiếm; test được không cần thiết bị |
| ui/home/HomeViewModel.kt | Loading/error/notice/mutation/phiên, chống lặp và stale response |
| HomeScreen/HomeComponents/TaskSheets | UI, chọn ngày, xem/lọc/xem nhanh task; callback vào ViewModel/NavHost |
| MainActivity/UthTodoApp | Factory, bind phiên, inject state và snackbar theo resource của Compose |
| Gradle/catalog/.gitignore/template | Dependency và token local, giữ cấu hình môi trường |
| TaskApiTest/HomeViewModelTest/test fixture | 25 test mới về contract, thống kê và hành vi |
| README/roadmap/báo cáo | Cách chạy, trạng thái và giới hạn |

Fixture nằm trong `app/src/test/resources/tasks-list.json`, được dựng thủ công theo cấu trúc nhìn thấy trong đề. Fixture không nằm ở runtime và không được dùng để thay thế kết quả API lỗi.

## Kiểm tra thực tế

| Kiểm tra | Kết quả |
|---|---|
| Kotlin/JUnit chạy riêng | **51/51 đạt**: TaskApiTest 13, HomeViewModelTest 12, Auth 12, Firebase Services 14 |
| Các trường hợp mới | Parse đúng/sai/empty/ID trùng, ngày/giờ, category lạ, nhóm/ngày/search, request GET/PATCH/header/status, retry/timeout, ACK/chống lặp, đổi phiên/cancel |
| Compile source Kotlin/Compose | **46 file đạt** với compiler/Compose plugin 2.2.10, thư viện thật có sẵn và header R/BuildConfig phát sinh để kiểm tra nguồn |
| AAPT2 compile resources | Đạt |
| Resource references, JSON/XML/TOML và diff whitespace | Đã kiểm tra |
| Wrapper assembleDebug/testDebugUnitTest/lintDebug, lần kiểm tra đầu task | Bị chặn trước compile: file khóa gradle-9.6.0-bin.zip.lck, Access is denied |
| Live GET/API/prototype | Host không truy cập được, lỗi DNS getaddrinfo failed; web tool cũng không đọc được trang |
| Build Gradle BTVN 04 trên máy người dùng | Ảnh BUILD SUCCESSFUL trong 2 phút 54 giây cho assembleDebug/testDebugUnitTest/lintDebug; chưa đọc được đầy đủ báo cáo test/lint mới trong workspace nên không tự gán số lượng |
| UI/API theo nghiệm thu người dùng | Người dùng xác nhận đã build và chạy thử thành công; agent không tự vận hành thiết bị hoặc thu log riêng từng endpoint |

Compile nguồn riêng không thay thế Gradle dependency resolution hoặc build APK. R/BuildConfig headers chỉ phục vụ type check; không đóng gói token thật trong artifact kiểm tra nguồn. Không nâng Kotlin, không bỏ qua metadata check hoặc chỉnh cache/đường dẫn máy để vượt blocker.

## Nghiệm thu người dùng

Ngày 20/11, sau khi cung cấp ảnh build thành công và xử lý bước Run, người dùng thông báo đã build và chạy test thành công hết. Ghi nhận các luồng người dùng đã tự thử là đạt, sẵn sàng commit/push phần Home BTVN 04. Đây là xác nhận của người dùng, không phải kết quả kiểm thử thiết bị/API tự động do agent thực hiện.

## Tái kiểm thử khi cần

1. Kiểm tra nhánh BTVN 04 hiện tại:

```powershell
git branch --show-current
```

2. Sync Gradle rồi chạy:

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

3. Run và đăng nhập hoặc dùng Guest. Kiểm tra load, lỗi/thử lại và dữ liệu thực từ API.
4. Kiểm tra tiến độ đúng ngày. Nếu hôm nay không có việc nhưng API có dữ liệu mẫu ngày khác, bấm header chọn ngày hoặc Xem tất cả.
5. Thử Học tập/Cuộc sống, tìm kiếm có/không dấu, xem task, tick/bỏ tick và xem tiến độ thay đổi sau ACK.
6. Mất mạng/thử lại: giữ dữ liệu cũ, không giả cập nhật thành công. Tải lại để đối chiếu trạng thái server mock.
7. Thử Avatar/Cài đặt/Hồ sơ, tên RTDB, chuông/FCM, banner, Google/email/Guest và logout/Back; thử màn hình nhỏ/landscape/font lớn.
8. Chỉ commit/push sau nghiệm thu. Lệnh chọn file có trong BRANCH-ROADMAP.md; không stage file token, config máy hoặc thư mục IDE/công cụ.

Người dùng đã tạo/chọn `feature/btvn04-home-api`; không cần tạo lại nhánh. Mã đang trong working tree và chưa stage/commit. Agent chưa thực thi Git ghi. Mốc tiếp theo là hoàn thiện màn hình danh sách riêng, rồi flow chi tiết theo hợp đồng API; không tự gán tên BTVN 05 khi đề chưa có mô tả.

## Nguồn

- File đề cục bộ và ảnh GET response/ảnh Home kèm trong đó; API/prototype theo URL của đề.
- [Gson strictness và troubleshooting](https://google.github.io/gson/Troubleshooting.html).
- Dependency OkHttp 4.12.0/Gson 2.11.0 được chọn theo phiên bản có sẵn, tương thích source Java 11/Kotlin 2.2.10.
