# Lộ trình nhánh UTH Todo

Cập nhật 20/11/2026. Mỗi mốc được kiểm thử và commit riêng; thao tác Git làm thay đổi trạng thái do người dùng thực hiện.

| Mốc | Nhánh | Trạng thái |
|---|---|---|
| BTVN 01 | `feature/btvn01-navigation-oop` | Commit `c4a0623`, người dùng đã thử |
| BTVN 02 | `feature/btvn02-firebase-auth` | Commit `4f3002e`, người dùng xác nhận Google/email chạy được |
| BTVN 03 | `feature/btvn03-firebase-services` | Commit `028f378`, người dùng đã build và xác nhận tự kiểm thử thành công |
| BTVN 04 | `feature/btvn04-home-api` | Đã được người dùng build và xác nhận chạy thử thành công; 51 test riêng/compile nguồn đạt; đang ở đúng nhánh, sẵn sàng commit/push |
| Flow danh sách | `feature/task-list-api` | BTVN 04 đã có danh sách xem nhanh; màn hình riêng và flow đầy đủ còn đối chiếu |
| Flow chi tiết | `feature/task-detail-api` | BTVN 04 chỉ xem nhanh dữ liệu list; GET theo ID, sửa/xóa và CRUD chưa triển khai |
| BTVN 05 | Chưa đặt | Đề chưa đủ mô tả, không tự gán tính năng |

BTVN 04 được đối chiếu ảnh đề cục bộ: Home, ảnh flow danh sách/chi tiết và ảnh response GET có mảng JSON. Figma/prototype và live API chưa truy cập được. Không coi việc compile với fixture là đã chạy API thật.

## Kiểm tra nhánh BTVN 04

Base là BTVN 03 ở `028f378`. Người dùng đã tạo/chọn `feature/btvn04-home-api`; mã BTVN 04 đang trong working tree và chưa stage. Agent không tự tạo/chuyển nhánh, stage, commit, push hoặc merge.

Người dùng chạy:

```powershell
git status
git branch --show-current
```

Nhánh hiện tại đã kế thừa BTVN 03, không cần tạo lại. Đọc status/lịch sử trước khi thao tác Git; không reset/clean để bỏ thay đổi.

## Build và nghiệm thu

Token mock từ đề đã được lưu trong `api.local.properties` bị Git bỏ qua. Khi pull ở máy khác, tạo file từ template hoặc cung cấp biến `UTH_MOCK_API_TOKEN`. Xem [BTVN04-REPORT.md](BTVN04-REPORT.md).

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

Người dùng đã cung cấp ảnh build thành công và xác nhận đã chạy thử thành công hết các luồng đã kiểm tra. Sẵn sàng chốt phần Home BTVN 04 theo nghiệm thu người dùng; agent không tự vận hành thiết bị hoặc coi xác nhận này là log riêng cho từng API. Người dùng tự stage:

```powershell
git diff --stat
git diff
git ls-files --others --exclude-standard
git add .gitignore README.md api.local.properties.example app/build.gradle.kts gradle/libs.versions.toml
git add app/src/main/java/com/example/uth_todo app/src/main/res/values/strings.xml
git add app/src/test/java/com/example/uth_todo/HomeViewModelTest.kt app/src/test/java/com/example/uth_todo/TaskApiTest.kt app/src/test/resources/tasks-list.json
git add docs/BRANCH-ROADMAP.md docs/BTVN04-REPORT.md
git diff --cached --stat
git diff --cached
git commit -m "BTVN 04: Home UI and task API"
git push -u origin feature/btvn04-home-api
```

Đây là hướng dẫn, chưa được agent thực thi. Không dùng git add toàn bộ thư mục gốc: cấu hình công cụ/IDE, config Firebase, token cục bộ và cache không thuộc commit.

## Mốc sau

- Hoàn thiện các flow danh sách/chi tiết theo hợp đồng API, không giả báo đã lưu/xóa khi chưa được API xác nhận.
- Nếu main đã được người dùng merge mốc trước, tạo nhánh mới từ main đã cập nhật.
- Nếu chưa merge, nhánh tiếp theo kế thừa commit mốc trước đã nghiệm thu và chọn base PR phù hợp.
- Không tạo hàng loạt nhánh rỗng hoặc gộp toàn bộ tính năng vào một lần push main.
