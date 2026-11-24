# Cài đặt giao diện và chốt bản dùng trên thiết bị

Ngày 24/11/2026. Base: `c8eece6`. Đang làm trên `feature/task-reminders`; nhánh gợi ý để người dùng chốt thay đổi: `feature/appearance-finalization`. Agent không stage, commit, push hoặc chuyển nhánh.

## Kết quả triển khai

- Hồ sơ → Giao diện: Theo hệ thống (mặc định), Sáng, Tối.
- Áp dụng ngay sau khi lưu thành công cho Home, Danh sách, Chi tiết, Tạo mới, Hồ sơ, Thông báo và các màn hình đăng nhập/giới thiệu.
- Theo hệ thống đọc trạng thái sáng/tối bằng API Compose có theo dõi cấu hình. Hai chế độ cố định không đổi theo thiết bị.
- DataStore lưu lựa chọn riêng trên thiết bị, không phụ thuộc Guest/UID và không đổi nguồn dữ liệu công việc.
- Một instance DataStore cho file giao diện trong process. Android và kiểm tra lưu file dùng cùng factory Okio có sẵn; không thêm dependency.
- Không áp dụng lựa chọn mới trước khi ghi xong. Nút chọn bị khóa trong lúc đọc/ghi, tránh ghi chồng. Lỗi ghi giữ giao diện trước; chọn lại để thử lại.
- Lỗi đọc dùng Theo hệ thống và có nút thử lại. Đọc/ghi có timeout 4 giây, không giữ splash hoặc khóa lựa chọn vô hạn. Không xóa dữ liệu khi đọc lỗi.
- Splash hệ thống chờ lần đọc đầu hoàn tất; màu biểu tượng status/navigation bar cập nhật theo nền thực tế. Splash/logo vẫn giữ màu thương hiệu.
- Bổ sung palette tối, sửa thẻ tiến độ và màu nhóm Cuộc sống, các thẻ minh họa và chỉ báo onboarding để phù hợp nền tối.
- Tăng độ đậm chữ phụ ở chế độ sáng và định nghĩa các lớp nền thẻ nhất quán; kiểm tra số học tương phản cho các cặp chữ/nền chính đạt ít nhất 4,5:1, không thay cho kiểm tra giao diện thực tế.
- Đổi theme chỉ cập nhật màu, không tạo lại Navigation graph, phiên đăng nhập hoặc ViewModel công việc. Bản nháp và bộ lọc vẫn giữ.

## Phạm vi thay đổi

| File/nhóm | Vai trò |
|---|---|
| `model/ThemeMode.kt` | Ba lựa chọn và quy tắc theo hệ thống |
| `data/preferences/AppearancePreferences.kt` | Singleton kho và lưu/đọc lựa chọn |
| `ui/settings/AppearanceViewModel.kt` | Đọc lúc mở app, ghi có xác nhận, lỗi và thử lại |
| `ui/settings/AppearanceScreen.kt` | Radio group có thể cuộn, trạng thái tải/lỗi |
| `ui/theme/Theme.kt` | Palette sáng/tối |
| `MainActivity`, `navigation/`, `ProfileScreen` | Khởi tạo, áp dụng toàn app và route có kiểm tra phiên |
| `HomeComponents`, `OnboardingArtwork`, `OnboardingScreen` | Màu nội dung theo theme |

Không đổi Gradle, SDK, Kotlin, Java, Firebase BoM, dependency, đường dẫn máy hoặc cache để xử lý theme. Không sửa Firebase rules hoặc triển khai cloud sync.

## Kiểm tra

| Kiểm tra | Kết quả |
|---|---|
| JUnit chạy riêng bằng Kotlin và thư viện có sẵn | 148/148 đạt: 137 cũ + 11 mới |
| Biên dịch source với đúng compile classpath do Gradle ghi trong lint model | 75 file Kotlin/Compose đạt, có header R/BuildConfig phục vụ kiểm tra; không phải APK |
| AAPT2 compile tài nguyên | Đạt |
| Biên dịch Android test với compile classpath từ Gradle model | 52 test đạt biên dịch, gồm 6 ca mới: đổi màu toàn app/Back, theo hệ thống, giữ tìm kiếm/nhóm, giữ draft, mất phiên và font lớn; chưa chạy trên thiết bị |
| Gradle assembleDebug/testDebugUnitTest/lintDebug | Bị chặn trước task bởi quyền mở khóa wrapper (`Access is denied`), chưa xác nhận lần build sau thay đổi |
| Run/ảnh giao diện/permission/chuông/reboot trên Android thật hoặc emulator | Chưa chạy trong môi trường agent |

11 unit test mới kiểm tra quy tắc màu, giá trị lưu không hợp lệ, lưu cả ba chế độ qua nhiều lần đóng/mở file thật, đọc lúc mở app, không công bố trước khi ghi xong, chặn thao tác lặp, khôi phục lỗi đọc/ghi, timeout và đọc lại ở ViewModel mới.

## Thử nhanh

1. Build theo lệnh trong [PROJECT-HANDOFF.md](PROJECT-HANDOFF.md), Run bản vừa build.
2. Hồ sơ → Giao diện → Tối, quay lại Home/List/Chi tiết/Tạo mới/Thông báo. Kiểm tra chữ, nền, nút, bàn phím và thanh hệ thống dễ đọc.
3. Chọn Sáng; giao diện đổi về nền sáng. Đóng/mở lại app bình thường, kiểm tra vẫn là Sáng. Không Clear data hoặc gỡ app.
4. Chọn Theo hệ thống, đổi chế độ tối trong cài đặt Android rồi quay lại app. Kiểm tra app đổi theo.
5. Thử đổi cài đặt Android khi app đang chọn Sáng hoặc Tối: app phải giữ lựa chọn cố định.
6. Tìm kiếm/lọc List, vào Hồ sơ → Giao diện, đổi màu rồi Back về List: từ khóa và nhóm phải giữ. Dữ liệu công việc không bị thay đổi.

Các bước kiểm thử tổng thể, APK, cấu hình máy khác và Git nằm trong [PROJECT-HANDOFF.md](PROJECT-HANDOFF.md).
