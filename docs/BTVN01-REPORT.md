# Báo cáo triển khai BTVN 01 — UTH Todo

Ngày: 09/11/2026. Nhánh đã kiểm tra: `feature/btvn01-navigation-oop`.

**Kết luận hiện tại:** đã triển khai mã BTVN 01 và các kiểm thử tập trung. Chưa xác nhận build thành công hoặc hành vi UI thực tế. Bài tập còn chờ nghiệm thu trên môi trường Android Studio của người dùng.

## 1. Yêu cầu và phạm vi

Nguồn yêu cầu là file `Pasted markdown.md` người dùng cung cấp: một Activity, Splash thương hiệu, ba trang onboarding, màn hình chào, Navigation Compose, DataStore Preferences, OOP dễ giải thích và tài liệu nghiệm thu. Không triển khai đăng nhập thật, Firebase, REST API, quản lý công việc, quyền thông báo hay cơ chế nhắc việc trong BTVN 01.

Không tìm thấy file AGENTS.md trên đĩa trong repository; đã áp dụng chỉ dẫn AGENTS.md người dùng gửi trong phiên làm việc. Nhánh thực tế đúng yêu cầu trước khi sửa. Ban đầu Git chỉ theo dõi README; toàn bộ bộ khung Android đang là file untracked. Không tạo/chuyển nhánh, stage, commit, push hay merge; giữ quyền thao tác Git cho người dùng.

Giữ SDK 37/minSdk 24, AGP 9.4.1, Kotlin Compose plugin 2.2.10, Gradle 9.6.0, tiêu chí daemon JDK 25, Java source/target 11, applicationId và versionName 1.0. Không sửa `local.properties`, SDK path hay cấu hình IDE.

## 2. Kết quả triển khai

| Yêu cầu | Cách triển khai | Mức xác nhận |
|---|---|---|
| Splash | Nền #3D55DF, checklist, tên app, khẩu hiệu, chỉ báo và versionName thực tế; đồng bộ system splash qua SplashScreen API | Đã viết mã; chưa chạy UI |
| Ba trang onboarding | Nội dung tiếng Việt đúng đề, minh họa checklist/nhóm việc/chuông, chỉ báo trang, nút tiếp tục và bỏ qua | Đã viết mã; chưa đối chiếu hình ảnh thực tế |
| Chuyển trang | HorizontalPager, Tiếp tục và vuốt cập nhật cùng pager state | Có UI test, chưa chạy |
| Back | Trang 2/3 về trang trước; trang 1 dùng hành vi Android thông thường | Có UI test và checklist thủ công, chưa chạy |
| Hoàn tất/bỏ qua | Chờ ghi DataStore thành công rồi đến màn hình chào; xóa luồng giới thiệu khỏi stack | Có test ViewModel/UI, chưa chạy |
| Bấm nhanh | Cờ isSaving đặt đồng bộ trước khi khởi chạy ghi; launchSingleTop và xóa stack khi đổi luồng | Có test bấm lặp, chưa chạy |
| Khôi phục trang | rememberPagerState với Saver, NavController và ViewModel theo Activity | Có test saved-state restoration; chưa thử xoay Activity thật |
| Mở lại app | Splash đọc DataStore rồi quyết định, không chủ động ghé onboarding nếu đã hoàn tất | Có test người dùng quay lại và lưu file; chưa chạy |
| Lỗi lưu trữ | Corruption handler khôi phục preferences rỗng; lỗi đọc về onboarding kèm thông báo; lỗi ghi giữ nguyên luồng và có Thử lại; timeout đọc/ghi 4 giây | Có test lỗi/timeout, chưa chạy |
| Màn hình chào | Google và dùng thử có Snackbar nói rõ sẽ kết nối ở BTVN 02 | Có UI test, chưa chạy |
| Hiển thị linh hoạt | Safe drawing insets, giới hạn bề rộng, nội dung cuộn, bố cục cuộn toàn màn hình cho chiều cao thấp/font lớn | Có UI test 320 × 480 dp/font scale 2; chưa chạy |
| Accessibility | Tiêu đề có heading, chỉ báo đọc số trang, minh họa trang trí không đọc lặp, vùng chạm Material | Đã rà soát mã; chưa thử TalkBack |

Splash có độ trễ trình diễn tối thiểu khoảng 1 giây. Đây không phải thời gian tải mạng. ViewModel dùng coroutine, không chặn main thread bằng sleep. Nếu đọc DataStore không kết thúc, timeout giải phóng Splash để tiếp tục được.

## 3. File sửa/thêm

Đường dẫn Kotlin bên dưới tính từ `app/src/main/java/com/example/uth_todo/`. “Sửa” chỉ việc chỉnh nội dung file vốn có trên máy; phần lớn file chưa được Git theo dõi từ trước task.

| File | Loại | Mục đích |
|---|---|---|
| `MainActivity.kt` | Sửa | Khởi tạo SplashScreen API, ViewModel và nội dung app |
| `navigation/AppDestination.kt` | Thêm | Sealed class ba destination |
| `navigation/UthTodoApp.kt` | Thêm | NavHost, chọn luồng, dọn stack, Snackbar, system bar |
| `model/OnboardingPage.kt` | Thêm | Data class nội dung trang và enum minh họa |
| `data/preferences/OnboardingPreferences.kt` | Thêm | Đóng gói instance DataStore dùng application context |
| `ui/onboarding/OnboardingViewModel.kt` | Thêm | Trạng thái và sự kiện đọc/hoàn tất/thử lại |
| `ui/onboarding/OnboardingScreen.kt` | Thêm | Pager, BackHandler, nút và chỉ báo |
| `ui/splash/SplashScreen.kt` | Thêm | Splash Compose |
| `ui/welcome/WelcomeScreen.kt` | Thêm | Màn hình chào trước đăng nhập |
| `ui/components/Brand.kt` | Thêm | Logo và header dùng lại |
| `ui/components/PrimaryButton.kt` | Thêm | Nút chính dùng lại, có tùy chọn icon |
| `ui/components/OnboardingArtwork.kt` | Thêm | Minh họa offline bằng Compose và Canvas |
| `ui/theme/Color.kt`, `Theme.kt`, `Type.kt` | Sửa | Màu/typography/theme sáng theo thương hiệu |
| `app/src/main/AndroidManifest.xml` | Sửa | Gán launch theme cho MainActivity |
| `app/src/main/res/values/strings.xml` | Sửa | Chuỗi UI tiếng Việt trong resources |
| `app/src/main/res/values/colors.xml`, `themes.xml` | Sửa | Màu nền và theme system splash |
| `app/src/main/res/drawable/ic_uth_logo.xml`, `ic_uth_splash.xml`, `ic_google.xml` | Thêm | Logo, icon khởi động và biểu tượng Google |
| `gradle/libs.versions.toml` | Sửa | Navigation, DataStore, SplashScreen, Lifecycle Compose và coroutine test |
| `app/build.gradle.kts` | Sửa | Khai báo dependency và bật BuildConfig để đọc versionName |
| `app/src/test/java/com/example/uth_todo/OnboardingViewModelTest.kt` | Thêm | 7 ca trạng thái và xử lý lỗi/bấm lặp |
| `app/src/test/java/com/example/uth_todo/OnboardingPreferencesTest.kt` | Thêm | 1 ca lưu bền vững qua việc tạo lại DataStore |
| `app/src/androidTest/java/com/example/uth_todo/OnboardingFlowTest.kt` | Thêm | 6 ca UI/navigation/saved state/font lớn |
| `README.md` | Sửa | Cách chạy, sơ đồ, cấu trúc và giới hạn |
| `docs/BTVN01-REPORT.md` | Thêm | Báo cáo và hướng dẫn nghiệm thu này |

## 4. Navigation và OOP để trình bày

`AppDestination` là sealed class: Splash, Onboarding và Welcome tập trung ở một nơi, giảm lỗi viết sai route. `UthTodoApp` quản lý NavHost; các màn hình không tự điều khiển NavController.

`OnboardingPage` là data class: gom tiêu đề, mô tả và loại minh họa thành một đối tượng. Ba trang dùng cùng một cấu trúc và cùng một composable thay vì chép lại UI. `OnboardingIllustration` là enum giúp biểu diễn đúng ba lựa chọn minh họa; biểu thức when phải xử lý đủ các trường hợp.

`OnboardingPreferences` thể hiện đóng gói: che chi tiết key và thao tác DataStore sau `completed`/`complete()`. UI không biết tên file hoặc key lưu trữ. Constructor nhận DataStore cho phép test dùng store riêng; constructor nhận Context dùng application context để tránh giữ Activity.

`OnboardingViewModel` kế thừa ViewModel để quản lý lifecycle và StateFlow. UI chỉ đọc state bất biến từ bên ngoài. Các trạng thái loading/saving/completed/error tách thời điểm bắt đầu thao tác khỏi thời điểm lưu thành công; không đánh dấu hoàn tất khi chỉ mở trang cuối.

Callback như `onComplete`, `onGoogleClick`, `onGuestClick` tách UI khỏi nghiệp vụ và điều hướng. Ví dụ nút Bắt đầu ngay gọi `onComplete`; ViewModel lưu thành công mới cập nhật completed; NavHost quan sát state rồi chuyển tới Welcome.

Luồng bình thường: Splash → đọc cờ → Onboarding hoặc Welcome. Đổi luồng dùng popUpTo graph để loại bỏ màn hình cũ và launchSingleTop để tránh bản sao destination. Back ở trang 2/3 được BackHandler chuyển về trang trước; Back ở trang 1 hoặc Welcome không có màn hình giới thiệu phía dưới.

## 5. Kiểm tra đã thực sự thực hiện

| Thao tác thực tế | Kết quả |
|---|---|
| `git status`, `git branch --show-current`, `git remote -v`, đọc file dự án và tìm AGENTS.md | Xác nhận đúng dự án/nhánh và bộ khung ban đầu |
| Build ban đầu: `.\gradlew.bat --no-daemon assembleDebug` | Dừng trước biên dịch: không mở được file khóa cache wrapper, báo `Access is denied` |
| Chạy Gradle 9.6.0 đã có với cache tạm, offline | Lần thử cache ngoài thư mục dự án dừng ở native-platform.dll; cache trong thư mục được phép khởi tạo được Gradle |
| Chạy bản Gradle cùng phiên bản wrapper với cache tạm trong `.gradle`, `--no-daemon --offline assembleDebug testDebugUnitTest lintDebug` và các tham số JVM chỉ dùng cho lần chạy | Gradle chọn được JDK 25 đúng tiêu chí dự án nhưng dừng với `Could not connect to the Gradle daemon` trước khi biên dịch hoặc chạy task kiểm thử |
| Thử bỏ tùy chọn JVM của daemon/instrumentation chỉ cho lần chạy, không sửa file cấu hình | Vẫn không kết nối được daemon |
| `adb devices -l` | `could not read ok from ADB Server`, `failed to start daemon`, không có kết nối thiết bị để chạy UI |
| Thử kết nối phiên Android Studio qua điều khiển ứng dụng | Không khởi tạo được dịch vụ điều khiển, trả về `failed to launch codex app-server: program not found`; chưa thao tác hoặc chạy build qua IDE |
| Python parse XML và TOML, kiểm tra resource reference và UTF-8 | Đạt: 13 file XML parse được, version catalog tham chiếu version hợp lệ, các R.string trong mã main có resource, không có ký tự thay thế do lỗi mã hóa |
| Git chỉ đọc và kiểm tra whitespace | Không có thao tác Git thay đổi trạng thái do agent thực hiện |

Các lệnh thử cache/JVM là cấu hình tạm của tiến trình kiểm tra, không thay đổi SDK, `local.properties`, tiêu chí daemon hay JDK đã cấu hình cho dự án. Thông số đường dẫn máy được lược bỏ khỏi báo cáo này.

**Chưa chạy thành công:** assembleDebug, testDebugUnitTest, lintDebug, biên dịch androidTest, connectedDebugAndroidTest, Run app, xoay màn hình thực tế và TalkBack. Không có APK hoặc ảnh nghiệm thu UI được xác nhận từ lần triển khai này. Không được suy ra “build đạt” từ các kiểm tra XML/TOML/resource.

Người dùng mở Android Studio, Sync Gradle để tải dependency mới, rồi chạy lệnh build thông thường bên dưới. Các lỗi môi trường kể trên không phải bằng chứng rằng mã Kotlin đã được biên dịch hoặc đã được compiler xác nhận.

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

Nếu gặp lỗi daemon khi chạy trực tiếp trong Android Studio, kiểm tra Gradle JDK đang khớp tiêu chí JDK của dự án, Gradle không ở Offline Mode khi cần tải dependency mới, sau đó đọc dòng lỗi đầu tiên trong Build Output. Không đổi SDK/JDK hoặc hạ phiên bản thư viện theo phỏng đoán.

## 6. Đối chiếu thiết kế và phần dựng thay thế

Đã đọc nội dung yêu cầu và trích xuất văn bản từ ảnh BTVN 01/Authentication. Không truy cập được Figma và prototype qua công cụ tra cứu; tải ảnh gốc để kiểm tra trực quan cũng không thành công trong môi trường hiện tại. Vì vậy bố cục và thông số được triển khai theo mô tả, chưa xác nhận khớp ảnh gốc ở mức trực quan hoặc pixel.

- Checklist/logo là VectorDrawable tự dựng; không phải asset xuất trực tiếp từ Figma.
- Ba thẻ checklist, hai thẻ nhóm và chuông/thẻ nhắc là Compose/Canvas, chạy offline.
- Minh họa Welcome dùng lại nhóm thẻ công việc và dấu hoàn tất.
- Palette, kích thước, khoảng cách, độ nghiêng thẻ và typography là giá trị gần đúng; cần đối chiếu trên giả lập.
- Version hiển thị là 1.0 theo cấu hình dự án, không chép số phiên bản trong ảnh mẫu.
- Mô tả “Bật thông báo” thuộc nội dung giới thiệu; app chưa yêu cầu quyền hay lập lịch thông báo.

## 7. Checklist nghiệm thu trên giả lập

Chạy các lượt có trạng thái mới theo nhu cầu, giữ nguyên dữ liệu nếu chưa muốn reset.

| Kiểm tra | Mong đợi |
|---|---|
| Run với dữ liệu app mới | Splash có nhận diện rồi đến trang 1 |
| Tiếp tục hai lần | Trang 2 rồi 3, chỉ báo và nút đổi đúng |
| Vuốt trái/phải | Trang, chỉ báo và nhãn nút đồng bộ |
| Back từ 3/2 | Về 2/1, không về Splash |
| Back ở trang 1 | Thoát theo hành vi Android thông thường |
| Xoay khi ở trang 2 và 3 | Vẫn giữ trang, nội dung có thể đọc/cuộn |
| Bỏ qua ở từng trang | Lưu hoàn tất, đến màn hình chào |
| Bắt đầu ngay ở trang 3 | Lưu hoàn tất, đến màn hình chào |
| Bấm nhanh nhiều lần | Không tạo nhiều destination hoặc ghi lặp |
| Back từ màn hình chào | Không trở về Splash/onboarding |
| Đóng/mở app sau hoàn tất | Splash rồi màn hình chào |
| Đóng app giữa onboarding | Chưa lưu hoàn tất; lần mở sau được phép về trang 1 |
| Google và dùng thử | Thông báo sẽ kết nối ở BTVN 02, không báo đăng nhập thành công |
| Màn hình nhỏ, landscape, font lớn | Cuộn được để đọc nội dung và bấm nút, không bị system bar che |

Nếu muốn kiểm tra lại luồng lần đầu, người dùng tự vào Settings → Apps → UTH Todo → Storage & cache → Clear storage hoặc gỡ/cài lại. Thao tác xóa dữ liệu cục bộ, chỉ dùng khi không có dữ liệu cần giữ. Agent không xóa dữ liệu hoặc thiết bị.

Các bài UI test dùng store riêng trong bộ nhớ; bài DataStore dùng file tạm. Sau khi ADB hoạt động và build được, có thể chạy:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

## 8. Git do người dùng tự thực hiện

Chỉ commit sau khi đọc thay đổi và nghiệm thu đạt yêu cầu. Android starter đang untracked nên lần commit này phải chứa cả bộ khung cần thiết, không chỉ các file Kotlin vừa thêm. Không stage `.idea`, `local.properties`, cache hoặc build output. Lệnh bên dưới chỉ là hướng dẫn; agent chưa thực thi.

```powershell
git status
git branch --show-current
git diff -- README.md
git ls-files --others --exclude-standard
```

Đọc file mới trong Android Studio vì `git diff` thông thường không hiển thị nội dung file untracked. Sau đó stage đúng bộ khung và phần BTVN 01:

```powershell
git add .gitignore README.md app build.gradle.kts gradle.properties gradle gradlew gradlew.bat settings.gradle.kts docs/BTVN01-REPORT.md
git diff --cached --stat
git diff --cached
```

`app/.gitignore` đã loại `app/build`; `.gitignore` gốc đã loại cache và `local.properties`. Đọc staged diff để bảo đảm không có file cục bộ ngoài phạm vi. Nếu kết quả kiểm tra và staged diff đều đạt:

```powershell
git commit -m "feat: implement BTVN01 splash onboarding and navigation"
git push -u origin feature/btvn01-navigation-oop
```

Không merge main tự động. Có thể mở Pull Request trên GitHub và tự đọc Files changed trước khi merge.

## 9. Chuẩn bị BTVN 02

Welcome đã có callback Google/dùng thử. BTVN 02 sẽ nối xác thực thật, xử lý lỗi/thành công và destination tiếp theo theo đề. Cờ onboarding độc lập với tài khoản; không dùng nó làm trạng thái đăng nhập. Chưa khai báo SDK Firebase, khóa, token hay dữ liệu tài khoản trong phần BTVN 01.

## Tài liệu kỹ thuật tham chiếu

- [Navigation release notes](https://developer.android.com/jetpack/androidx/releases/navigation)
- [DataStore release notes](https://developer.android.com/jetpack/androidx/releases/datastore)
- [Core và SplashScreen release notes](https://developer.android.com/jetpack/androidx/releases/core)
- [SplashScreen API](https://developer.android.com/develop/ui/views/launch/splash-screen)
- [Pager trong Compose](https://developer.android.com/develop/ui/compose/layouts/pager)
- [Preferences DataStore](https://developer.android.com/codelabs/android-preferences-datastore)
