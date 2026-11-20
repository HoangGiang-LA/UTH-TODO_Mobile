# Báo cáo BTVN 02 — Firebase Authentication

Ngày: 20/11/2026.

**Trạng thái chốt ngày 20/11:** người dùng xác nhận đã kiểm thử lại thành công sau sửa luồng Google và yêu cầu hướng dẫn commit/push. Nhánh hiện tại là `feature/btvn02-firebase-auth`, mã đang chưa stage/commit. Trước sửa Google đã có `BUILD SUCCESSFUL` với 19 unit Gradle đạt, lint 0 lỗi/23 warning; sau sửa Google, 12 unit Auth chạy riêng đạt và Google client biên dịch riêng được. Kết quả UI/xác thực thật sau sửa do người dùng xác nhận, không phải agent trực tiếp kiểm thử tài khoản.

## 1. Căn cứ và Git

Đã đọc tài liệu `Khởi tạo App Mobile(Task)(1).docx` và nội dung ảnh Authentication: đăng nhập có kết quả thành công/lỗi, thành công tới trang chủ, từ trang chủ vào hồ sơ. Chọn Google và email cho mốc này; Facebook/phone chưa được thêm.

Nhánh ban đầu là `feature/btvn01-navigation-oop`, HEAD `c4a0623`, khớp remote; BTVN 01 đã commit/push và người dùng đã báo chạy app thành công. Không có sửa đổi nguồn tracked trước task.

Giữ quy tắc Git thủ công: agent không tạo/chuyển nhánh, stage, commit hoặc push. Người dùng đã tạo và chuyển sang `feature/btvn02-firebase-auth` từ HEAD BTVN 01; thay đổi BTVN 02 hiện ở working tree chưa commit. Xem [BRANCH-ROADMAP.md](BRANCH-ROADMAP.md).

## 2. Mã đã triển khai

| Phần | Hành vi |
|---|---|
| Google | Credential Manager lấy ID token, Firebase Auth xác nhận trước khi có phiên SignedIn |
| Email | Đăng nhập/đăng ký, validation, gửi hướng dẫn đặt lại mật khẩu |
| Phản hồi | Snackbar lỗi/thành công; hủy Google không báo thành công, nút được giải phóng |
| Guest | Phiên cục bộ trong tiến trình, không tạo UID hoặc người dùng Firebase giả |
| Home | Điểm đến tối thiểu của BTVN 02, không gán dữ liệu công việc/thống kê giả |
| Profile | Tên, email, UID, xác minh từ Firebase User; khách có nội dung riêng |
| Phiên | Theo dõi AuthStateListener, khôi phục qua SDK, gỡ listener khi ViewModel kết thúc |
| Đăng xuất | Xóa phiên và Back stack, dọn Credential Manager trong tối đa 3 giây |
| Thiếu config | SignedOut và thông báo chưa sẵn sàng; không tạo config giả, vẫn dùng Guest được |
| UI | Form cuộn với bàn phím/font lớn, giữ onboarding và logic system bar |

Mật khẩu không lưu vào saved instance state, DataStore hoặc tài liệu. Email/chế độ form có saved state. Không lưu ID token thủ công. Busy được đặt trước khi chạy thao tác để chặn bấm lặp; xác thực timeout 90 giây, tính cả thời gian chọn tài khoản. Coroutine cancellation được truyền tiếp.

## 3. File sửa/thêm

Đường dẫn Kotlin tính từ `app/src/main/java/com/example/uth_todo/`.

| File | Mục đích |
|---|---|
| `model/AuthSession.kt` | UserProfile, các kiểu phiên, lỗi và thông báo |
| `data/auth/AuthRepository.kt` | Hợp đồng xác thực cho production/test |
| `data/auth/FirebaseAuthRepository.kt` | Gọi Firebase thật, listener, chuyển lỗi và đăng xuất |
| `data/auth/GoogleCredentialClient.kt` | Google ID token và Web OAuth client ID từ resources |
| `ui/auth/AuthViewModel.kt` | StateFlow, validation, busy, timeout, sự kiện |
| `ui/auth/AuthMessages.kt` | Thông báo tiếng Việt từ resources |
| `ui/auth/EmailAuthScreen.kt` | Form email và quên mật khẩu |
| `ui/home/HomeScreen.kt` | Home tối thiểu sau xác thực/dùng thử |
| `ui/profile/ProfileScreen.kt` | Hồ sơ và đăng xuất |
| `navigation/AppDestination.kt`, `UthTodoApp.kt` | Route EmailAuth/Home/Profile, bảo vệ theo phiên |
| `MainActivity.kt` | Factory AuthViewModel/Firebase repository |
| `ui/welcome/WelcomeScreen.kt` | Google/Guest/email và trạng thái chờ |
| `ui/components/PrimaryButton.kt` | Test tag cho các nút màn hình mới |
| `app/src/main/res/values/strings.xml` | Chuỗi Auth/Home/Profile |
| `app/src/main/AndroidManifest.xml` | Quyền Internet |
| Version catalog và hai `build.gradle.kts` | Dependency mới, Google Services áp dụng khi có config |
| `.gitignore` | Bỏ qua config Firebase Android riêng |
| `app/src/test/.../AuthViewModelTest.kt` | 10 ca unit với fake repository |
| `app/src/androidTest/.../OnboardingFlowTest.kt` | Giữ onboarding, cập nhật hành vi Welcome, thêm Guest/hồ sơ/email |
| README, báo cáo này, BRANCH-ROADMAP | Hướng dẫn, kiểm tra và lộ trình |

Không đổi Gradle, AGP, SDK, Kotlin Compose hoặc JDK đã có; không sửa `local.properties`, đường dẫn máy, xóa cache hay dữ liệu app.

## 4. Điều hướng

Splash chờ cờ onboarding và phiên. Chưa hoàn tất giới thiệu → onboarding; đã hoàn tất → Home nếu có SignedIn/Guest, hoặc Welcome nếu SignedOut.

Welcome → EmailAuth giữ Welcome để Back quay lại. Đăng nhập/dùng thử tới Home và xóa luồng chào. Home → Profile giữ Home. Đăng xuất tới Welcome và xóa Home/Profile. Việc chọn route chờ thao tác xác thực kết thúc.

Guest chỉ ở bộ nhớ tiến trình. Sau khi Android tạo tiến trình mới, Guest trở về Welcome; Firebase User được SDK khôi phục. Cờ onboarding không dùng làm trạng thái đăng nhập.

## 5. Kiểm tra thực tế

| Kiểm tra | Kết quả |
|---|---|
| Nhánh, status, lịch sử | Xác nhận BTVN 01 đã commit/push, không thao tác Git ghi |
| Baseline và sau sửa: `.\gradlew.bat assembleDebug testDebugUnitTest lintDebug` | Bị chặn trước task đầu tiên: FileNotFoundException / Access is denied tại khóa wrapper |
| Biên dịch riêng AuthSession/AuthRepository/AuthViewModel/AuthViewModelTest | Đạt, compiler Kotlin 2.2.10, JVM target 11, dependency đã có |
| `org.junit.runner.JUnitCore com.example.uth_todo.AuthViewModelTest` | **OK (10 tests)**, thời gian JUnit 0,199 giây |
| Compile adapter Firebase/Google, Compose và APK | Chưa xác nhận |
| testDebugUnitTest / lintDebug qua Gradle | Chưa chạy được; JUnit riêng không thay thế hai task này |
| UI test và Run thiết bị | Chưa chạy trong lần này |
| Firebase/Google thật | Chưa chạy: thiếu `app/google-services.json` và cấu hình provider |
| Rà soát tĩnh cuối task | XML/TOML/resource và UTF-8 tài liệu đạt; git diff --check không lỗi; khối SystemBarStyle, wrapper, tiêu chí JDK, gradle.properties và settings giữ nguyên |

JUnit riêng chỉ dùng compiler/JAR/AAR có sẵn, giải nén ViewModel vào `.gradle/btvn02-auth-check/` bị Git bỏ qua; không sửa cache hoặc cấu hình máy. Log compile/JUnit nằm trong thư mục tạm đó.

Mười ca đã chạy: thiếu config không mở Google/không giả thành công; hủy chooser; bấm nhanh chỉ một request; lỗi mạng và thử lại; input đăng ký sai; đăng ký theo kết quả repository; reset không tạo phiên; Guest không phải SignedIn; khôi phục/đăng xuất; timeout giải phóng busy. Các ca dùng fake repository, không phải 10 lượt xác thực Firebase thật.

Báo cáo có sẵn từ BTVN 01 ghi 9 unit không lỗi và lint 0 lỗi/13 warning. Đó là kết quả lịch sử, không xác nhận thay đổi BTVN 02.

## 6. Cấu hình để nghiệm thu thật

1. [Firebase Console](https://console.firebase.google.com/): tạo/chọn UTH Todo, đăng ký Android package `com.example.uth_todo`.
2. Chạy `.\gradlew.bat signingReport` trong Terminal Android Studio; thêm SHA-1/SHA-256 debug vào Android app Firebase.
3. Authentication → Sign-in method: bật Google và Email/Password, chọn email hỗ trợ nếu được yêu cầu.
4. Tải lại config Android sau khi bật Google; đặt `google-services.json` tại `app/`.
5. Sync Gradle. Google Services plugin sinh `default_web_client_id` khi file này có mặt.
6. Run trên thiết bị Google Play có tài khoản Google; thử xác thực và đối chiếu Firebase Authentication → Users.

Cần config Android UTH Todo, không phải khóa Admin SDK/service account hoặc project English Battle. Không dựng giá trị Firebase/OAuth giả. Hướng dẫn kỹ thuật nằm trong tài liệu; UI chỉ thông báo dịch vụ chưa sẵn sàng khi thiếu cấu hình.

## 7. Checklist

- [x] Sync dependency và chạy `.\gradlew.bat assembleDebug testDebugUnitTest lintDebug` — người dùng cung cấp BUILD SUCCESSFUL ngày 20/11.
- [ ] Giữ luồng onboarding: Tiếp tục/vuốt/Back/xoay/Bỏ qua/Bắt đầu ngay.
- [ ] Không config: không crash, Guest tới Home/hồ sơ và quay lại đăng nhập.
- [ ] Input sai, email trùng, mật khẩu sai có phản hồi.
- [ ] Đăng ký/email/Google tạo phiên đúng; UID/email khớp Firebase Console.
- [ ] Quên mật khẩu; hủy Google; bấm nhanh.
- [ ] Mở lại phiên Firebase tới Home; Guest ở tiến trình mới về Welcome.
- [ ] Đăng xuất rồi Back không vào Home/Profile.
- [ ] Màn hình nhỏ/font lớn/landscape/bàn phím vẫn thao tác được.
- [ ] Khi thiết bị kết nối, chạy `.\gradlew.bat connectedDebugAndroidTest`.

## 8. Phần còn lại

### Sửa lỗi tương thích Firebase Auth

Build người dùng cung cấp dừng tại `compileDebugKotlin`: Auth 25.0.0 do BoM 35.2.0 kéo vào có metadata 2.4.0, trong khi compiler dự án đọc metadata 2.2.0. Đã đổi duy nhất phiên bản Firebase BoM trong catalog thành 34.7.0; theo bảng phiên bản chính thức, BoM này dùng Auth 24.0.1 và Firebase Common 22.0.1. Không ép riêng Auth trong khi giữ BoM mới, không thêm cờ bỏ qua kiểm tra metadata, không đổi Kotlin/Gradle/SDK hoặc xóa cache. Cần chạy lại build đầy đủ để xác nhận việc resolve dependency và lỗi tiếp theo nếu có.

Kiểm tra sau sửa: catalog hợp lệ, Kotlin/AGP giữ nguyên; `google-services.json` khớp package app và có Web OAuth client. Đã chạy lại ba task qua wrapper nhưng bị chặn tại khóa Gradle trước biên dịch, nên chưa xác nhận build thành công. Người dùng cần Sync rồi chạy lại cùng lệnh trên môi trường Android Studio.

Nguồn đối chiếu: [Firebase Android release notes — BoM 34.7.0](https://firebase.google.com/support/release-notes/android).

### Sửa lỗi Compose lint

Sau sửa phiên bản Firebase, lần chạy của người dùng dừng ở `lintDebug` với `LocalContextGetResourceValueCall`: đọc chuỗi bằng `context.getString()` trong coroutine Snackbar. Đã lấy chuỗi bằng `stringResource()` tại composition, rồi truyền chuỗi vào `LaunchedEffect`; chuỗi cũng là key của effect để thay đổi cấu hình/ngôn ngữ cập nhật thông báo. Giữ nguyên Snackbar và logic điều hướng, không suppress lint hoặc tạo baseline.

Đã đọc các XML kết quả `testDebugUnitTest` ngày 20/11: AuthViewModelTest 10, OnboardingViewModelTest 7, OnboardingPreferencesTest 1, ExampleUnitTest 1; tổng **19 test, 0 failure, 0 error**. Đây là kết quả trước sửa lint. Sau sửa đã chạy lại lệnh đầy đủ nhưng wrapper của agent vẫn trả `Access is denied`, nên chưa xác nhận lint hoặc build đầy đủ đạt. Cảnh báo `sun.misc.Unsafe` trong log không phải lỗi làm lần build này dừng.

### Xác nhận build sau sửa

Người dùng đã cung cấp ảnh `BUILD SUCCESSFUL in 15s` cho đầy đủ ba task ngày 20/11. Báo cáo lint hiện ghi `0 errors, 23 warnings`. Cảnh báo Unsafe của DataStore vẫn xuất hiện nhưng không làm build thất bại. Đã hoàn tất mốc build/unit/lint; không cần chạy lại chỉ để lặp kết quả khi chưa có thay đổi mã mới.

Mốc build được ghi nhận trước sửa Google; xác nhận kiểm thử app sau sửa nằm ở phần chốt nghiệm thu bên dưới. Home BTVN 02 chưa phải Home API của BTVN 04. Dịch vụ Firebase ngoài Auth và API công việc là mốc riêng, chưa triển khai/báo hoàn thành.

### Sửa luồng nút Google và yêu cầu chi phí

Người dùng báo đã thử các luồng và còn lỗi mở đăng nhập Google, đồng thời yêu cầu bỏ qua Google nếu phải nâng gói trả phí. Đã đối chiếu [Firebase Pricing](https://firebase.google.com/pricing): Google Authentication có thể dùng trên Spark trong hạn mức miễn phí. Không cần nâng Blaze chỉ để thử Google; agent không thay gói, bật billing hoặc thực hiện thanh toán.

Đã chuyển từ `GetGoogleIdOption` (bottom sheet) sang `GetSignInWithGoogleOption` cho thao tác bấm nút. Theo [hướng dẫn Android](https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation), luồng nút hỗ trợ trường hợp thiếu tài khoản hoặc cần xác thực lại. SDK Google ID 1.1.1 hiện có hỗ trợ API này, không đổi dependency hoặc cấu hình máy.

`NoCredentialException` được ánh xạ sang thông báo tài khoản Google riêng, vẫn giữ SignedOut và cho phép thử lại. Với lỗi Credential Manager khác, tag `UthGoogleAuth` chỉ ghi loại lỗi, không ghi ID token, email hoặc dữ liệu tài khoản. Hủy chooser vẫn không báo đăng nhập thành công.

Kết quả sau sửa: **12/12 unit AuthViewModel chạy riêng đạt**, gồm hai ca mới cho thiếu credential/thử lại và chỉ báo thành công sau kết quả trao đổi token. `GoogleCredentialClient` và model đã biên dịch riêng bằng Kotlin 2.2.10 với Google ID 1.1.1/Credentials 1.6.0, dùng Android platform đã cài. Kiểm tra XML/resource và whitespace đạt. Lệnh build đầy đủ sau sửa vẫn bị chặn tại khóa wrapper trong môi trường agent, nên chưa xác nhận APK/lint/UI Google thật sau thay đổi này. Mốc 19 unit/build/lint trước đó là trước sửa nút Google.

Config cục bộ khớp package app nhưng chỉ có OAuth client type 3 (Web), chưa thấy type 1 (Android). Cần người dùng đối chiếu SHA-1 debug với Android app trong Firebase Console và tải lại `google-services.json` nếu đã cập nhật. Đây là kiểm tra cấu hình, không phải kết luận chắc chắn Console thiếu OAuth Android. Giả lập hiện dùng Google APIs; không có Play Store không tự chứng minh thiếu Play services, nhưng vẫn cần tài khoản Google hoạt động trên thiết bị. Chưa truy cập được thiết bị để xác nhận nguyên nhân runtime cụ thể.

### Chốt nghiệm thu theo xác nhận người dùng

Sau khi báo đã thử các luồng khác và còn lỗi Google, người dùng đã kiểm thử lại sau sửa Google và xác nhận thành công ngày 20/11. Ghi nhận mốc BTVN 02 sẵn sàng để người dùng review diff, commit và push nhánh riêng. Không suy ra rằng các UI test tự động hoặc kiểm tra thiết bị khác đã được chạy; các kết quả chi tiết ở phần trên được giữ nguyên theo thời điểm thực hiện.

Đã kiểm tra Git: đúng nhánh `feature/btvn02-firebase-auth`, staged diff rỗng, remote origin đúng repository. `app/google-services.json` và `local.properties` được ignore. Chỉ stage source, dependency và tài liệu của BTVN 02; giữ cấu hình IDE/công cụ và cache ngoài commit. Agent chỉ in hướng dẫn, người dùng tự stage/commit/push.

Nguồn kỹ thuật: [Firebase Android setup](https://firebase.google.com/docs/android/setup), [Google sign-in](https://firebase.google.com/docs/auth/android/google-signin), [Email/password](https://firebase.google.com/docs/auth/android/password-auth), [Credentials](https://developer.android.com/jetpack/androidx/releases/credentials).
