# Lộ trình nhánh UTH Todo

Cập nhật 20/11/2026. Mỗi mốc có code, nghiệm thu và commit riêng. Căn cứ tài liệu `Khởi tạo App Mobile(Task)(1).docx`; phần BTVN 05 trích xuất chưa đủ mô tả nên không tự gán số bài cho các flow sau.

| Mốc | Nhánh dự kiến | Phạm vi và trạng thái |
|---|---|---|
| BTVN 01 | `feature/btvn01-navigation-oop` | Splash/onboarding; đã commit/push `c4a0623`, người dùng đã thử app |
| BTVN 02 | `feature/btvn02-firebase-auth` | Người dùng đã xác nhận kiểm thử lại thành công, gồm Google; sẵn sàng review/commit/push; chưa commit |
| BTVN 03 | `feature/btvn03-firebase-services` | Dịch vụ Firebase theo đề chi tiết và nhu cầu app; chưa triển khai |
| BTVN 04 | `feature/btvn04-home-api` | Home UI/API: công việc và thống kê; chưa triển khai |
| Flow danh sách | `feature/task-list-api` | Chờ đối chiếu đầy đủ đề/API |
| Flow chi tiết | `feature/task-detail-api` | Chi tiết/hoàn tất/xóa theo hợp đồng API; chưa triển khai |

Không coi việc thêm dependency là đã tích hợp Firebase. BTVN 03 có mô tả “toàn bộ các dịch vụ Firebase”; cần bám bản đề và dịch vụ app thực tế sử dụng. Không gộp mọi tính năng thành một lần push vào main.

## Kiểm tra nhánh BTVN 02 hiện tại

Agent giữ Git thủ công theo yêu cầu trước: chưa tạo/chuyển nhánh, stage, commit hoặc push. Người dùng đã chuyển sang `feature/btvn02-firebase-auth`; thay đổi BTVN 02 đang chưa stage/commit trên nhánh này.

Người dùng kiểm tra:

```powershell
git status
git branch --show-current
git log -3 --oneline --decorate
```

Kết quả nhánh phải là `feature/btvn02-firebase-auth`; nhánh đã kế thừa HEAD có BTVN 01. Không cần tạo lại nhánh. Nếu trạng thái khác, đọc status/lịch sử trước, không reset/clean để loại bỏ thay đổi.

## Nghiệm thu và commit

Thêm config Firebase, Sync và kiểm tra theo [BTVN02-REPORT.md](BTVN02-REPORT.md):

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

Sau khi chạy app/checklist đạt, người dùng tự stage đúng file:

```powershell
git diff --stat
git diff
git ls-files --others --exclude-standard
git add .gitignore README.md build.gradle.kts gradle/libs.versions.toml app/build.gradle.kts app/src/main/AndroidManifest.xml app/src/main/java/com/example/uth_todo app/src/main/res/values/strings.xml app/src/test/java/com/example/uth_todo/AuthViewModelTest.kt app/src/androidTest/java/com/example/uth_todo/OnboardingFlowTest.kt docs/BTVN02-REPORT.md docs/BRANCH-ROADMAP.md
git diff --cached --stat
git diff --cached
git commit -m "feat: implement BTVN02 Firebase authentication and profile"
git push -u origin feature/btvn02-firebase-auth
```

Các lệnh này chưa được agent chạy. Đọc file untracked trong IDE vì git diff trước stage chỉ hiển thị file đã theo dõi. Không đưa cấu hình IDE/công cụ, local.properties, config Firebase riêng hoặc cache/build output vào commit.

## Mốc sau

- Nếu main đã được người dùng merge mốc trước, tạo nhánh mới từ main đã cập nhật.
- Nếu chưa merge main, tạo nhánh kế tiếp từ commit mốc trước đã nghiệm thu; chọn base PR phù hợp để diff chỉ chứa phần bài mới.
- Tạo từng nhánh khi bắt đầu mốc, không tạo hàng loạt nhánh rỗng.
- Giữ token mock API ở cấu hình cục bộ khi đến mốc API, không đưa vào source/tài liệu Git.
- Sau Auth: đối chiếu dịch vụ BTVN 03, tiếp theo Home API và các flow danh sách/chi tiết.
