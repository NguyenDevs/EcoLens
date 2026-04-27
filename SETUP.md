# EcoLens - Hướng Dẫn Thiết Lập Môi Trường Phát Triển

Tài liệu này hướng dẫn chi tiết các bước thiết lập EcoLens từ đầu để lập trình viên có thể phát triển và triển khai dự án.

---

## 1. Yêu Cầu Tiên Quyết (Prerequisites)

Đảm bảo các công cụ sau được cài đặt trên máy:

| Công Cụ | Phiên Bản Tối Thiểu | Ghi Chú |
|---------|-------------------|--------|
| **Android Studio** | 2023.1.1+ (Hedgehog) | IDE chính cho phát triển Android |
| **JDK (Java Development Kit)** | 11 | Cấu hình trong `app/build.gradle.kts` |
| **Android SDK** | API 34 (Target), API 31 (Min) | Cài qua Android Studio SDK Manager |
| **Gradle** | 8.14 | Tự động tải qua Gradle Wrapper |
| **Kotlin** | 2.0.21 | Ngôn ngữ chính của dự án |
| **CMake** | 3.22.1+ | Cho mô-đun native C++ |
| **Node.js** | 18.x LTS hoặc cao hơn | Cho Cloudflare Worker |
| **npm** | 9.x+ | Quản lý package cho Node.js |
| **Wrangler CLI** | 3.x+ | Công cụ Cloudflare Workers |
| **Git** | 2.30+ | Kiểm soát phiên bản |

### Cài Đặt Công Cụ

**Windows PowerShell:**

```powershell
# Cài đặt Node.js (nếu chưa có)
# Download từ https://nodejs.org và chạy installer

# Cài đặt Wrangler CLI
npm install -g @cloudflare/wrangler

# Xác minh cài đặt
node --version
npm --version
wrangler --version
```


---

## 2. Thiết Lập Repository

### Clone Repository

```bash
git clone https://github.com/NguyenDevs/EcoLens.git
cd EcoLens
```

### Cấu Trúc Thư Mục Chính

```
EcoLens/
├── app/                              # Module ứng dụng Android chính
│   ├── build.gradle.kts              # Build configuration cho app
│   ├── google-services.json          # Firebase configuration
│   ├── proguard-rules.pro            # ProGuard rules cho release builds
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/                 # Kotlin source code
│       │   ├── res/                  # Resources (layouts, strings, etc.)
│       │   └── cpp/                  # Native C++ code (JNI)
│       └── androidTest/              # Instrumented tests
├── CloudFlare-Worker/                # Cloudflare Workers gateway
│   ├── EcoLens_Worker/               # Main API gateway worker
│   │   ├── src/worker.js             # Worker logic
│   │   └── wrangler.toml             # Wrangler configuration
│   └── EcoLens_Inaturalist_Renewer/
│       ├── .github/workflows/        # GitHub Actions workflows
│       └── scripts/renew.js          # Token renewal script
├── build.gradle.kts                  # Root Gradle configuration
├── gradle.properties                 # Gradle properties & API keys
├── settings.gradle.kts               # Gradle settings
└── gradle/wrapper/                   # Gradle wrapper files
```

**Giải Thích các thư mục:**

- **app/**: Chứa toàn bộ code Android app, resources, tests
- **CloudFlare-Worker/**: Chứa infrastructure backend với 2 workers:
  - `EcoLens_Worker`: Gateway chính xử lý Gemini API, HMAC auth, rate limiting
  - `EcoLens_Inaturalist_Renewer`: Tự động gia hạn token iNaturalist hàng ngày
- **build.gradle.kts**: Khai báo plugins, phiên bản Gradle
- **gradle.properties**: Cấu hình biến toàn cục và API keys
- **settings.gradle.kts**: Cấu hình repositories, includes modules

### Mở Dự Án trong Android Studio

1. Mở **Android Studio**
2. Chọn **File** → **Open** → Chọn thư mục `EcoLens`
3. Android Studio sẽ tự động nhận diện cấu trúc Gradle
4. Đợi Gradle sync hoàn tất (kiểm tra thanh trạng thái dưới cùng)
5. Nếu có lỗi, chọn **File** → **Sync Now** hoặc nhấn **Ctrl+Shift+S**

---

## 3. Cấu Hình Ứng Dụng Android

### Thiết Lập local.properties

File `local.properties` chứa đường dẫn SDK Android cục bộ:

```properties
sdk.dir=C:\\Users\\YourUsername\\AppData\\Local\\Android\\sdk
```


Android Studio tự động tạo file này khi mở dự án lần đầu.

### Thiết Lập gradle.properties

File `gradle.properties` ở thư mục root chứa các biến cấu hình chung:

```properties
# API Gateway URL (Cloudflare Worker)
WORKER_URL=https://your-worker-name.your-domain.workers.dev/

# Firebase Realtime Database URL
FIREBASE_URL=https://your-project-id-default-rtdb.region.firebasedatabase.app/

# APP_SECRET dùng cho HMAC-SHA256 signature verification
# ⚠️ CẢNH BÁO: Giữ bí mật, không commit vào git
APP_SECRET=YourVerySecureRandomStringHere12345678901234567890
```

**Để cấu hình tùy chỉnh:**

`WORKER_URL` và `FIREBASE_URL` sẽ được compile vào `BuildConfig` class.
Nếu muốn thay đổi khi build, chạy:

```bash
./gradlew assembleDebug -PWORKER_URL="https://your-worker.workers.dev/" -PFIREBASE_URL="https://your-database.firebasedatabase.app/"
```

### Gradle Sync & JDK Configuration

1. Sau khi clone hoặc sửa `build.gradle.kts`, đồng bộ Gradle:
   - **File** → **Sync Now** hoặc **Ctrl+Shift+S**

2. Xác nhận JDK:
   - **File** → **Project Structure** → **SDK Location**
   - Đảm bảo **JDK location** trỏ đến JDK 11+
   - **Android SDK** và **Android Gradle Plugin** cần phải là API 34

### Cấu Trúc Biên Dịch

**compileSdk:** 34 (Android 14)  
**minSdk:** 31 (Android 12)  
**targetSdk:** 34 (Android 14)  
**Java Target:** JVM 11  

**Build Features Enabled:**
- View Binding: Truy cập view an toàn
- Data Binding: Binding data từ ViewModel
- BuildConfig: Truy cập WORKER_BASE_URL, FIREBASE_DATABASE_URL

### Quyền Ứng Dụng

App cần các quyền sau (khai báo trong `AndroidManifest.xml`):

| Quyền | Mục Đích |
|-------|---------|
| `CAMERA` | Chụp ảnh xác định loài |
| `READ_MEDIA_IMAGES` | Đọc ảnh từ thư viện (Android 13+) |
| `READ_EXTERNAL_STORAGE` | Đọc ảnh từ thư viện (Android ≤ 12) |
| `WRITE_EXTERNAL_STORAGE` | Ghi ảnh/báo cáo (Android ≤ 9 chỉ) |
| `ACCESS_FINE_LOCATION` | Vị trí chính xác để gắn tag địa lý |
| `ACCESS_COARSE_LOCATION` | Vị trí gần đúng |
| `INTERNET` | Kết nối mạng (API, Firebase) |
| `ACCESS_NETWORK_STATE` | Kiểm tra trạng thái mạng |
| `VIBRATE` | Phản hồi rung |

### Chạy trên Emulator

```bash
# Liệt kê các emulator có sẵn
emulator -list-avds

# Khởi động emulator
emulator -avd Pixel_6_API_34

# Sau đó:
./gradlew installDebug
```

### Chạy trên Thiết Bị Vật Lý

1. Kích hoạt **Developer Mode** trên thiết bị:
   - **Settings** → **About Phone** → Nhấn 7 lần **Build Number**
   
2. Bật **USB Debugging**:
   - **Settings** → **Developer Options** → **USB Debugging** (On)
   
3. Kết nối qua USB

4. Quyền cấp phép:
   - Chờ popover yêu cầu trên thiết bị → **Allow**

5. Build và chạy:
   ```bash
   ./gradlew installDebug
   # hoặc chọn device trong Android Studio và nhấn Run (Shift+F10)
   ```

### Vector Drawable Quirks

- File `ic_launcher-playstore.png` là raster image (không phải SVG)
- Tất cả icon vector được định nghĩa trong `res/drawable/` dưới dạng XML SVG
- Nếu add icon mới, sử dụng Android Studio drawable importer hoặc XML vector format

---

## 4. Cấu Hình Firebase

EcoLens sử dụng **Firebase** cho xác thực, lưu trữ cơ sở dữ liệu thời gian thực, và lưu trữ file.

### Tạo Firebase Project

1. Truy cập [Firebase Console](https://console.firebase.google.com/)
2. Nhấn **Create a new project** hoặc **Add project**
3. Điền thông tin:
   - **Project Name:** `EcoLens-Dev` (hoặc tùy chỉnh)
   - **Region:** `Asia Pacific (Singapore)` hoặc gần nhất
4. Nhấn **Create project** và chờ khởi tạo hoàn tất

### Thêm Ứng Dụng Android

1. Trong Firebase Console, click **Add App** → **Android**
2. Điền form:
   - **Package name:** `com.nguyendevs.ecolens`
   - **App nickname:** `EcoLens Sample` (tùy chỉnh)
   - **SHA-1 certificate hash:** (xem hướng dẫn phía dưới)
3. Nhấn **Register app**
4. Download `google-services.json`
5. Sao chép vào `app/google-services.json`

### Lấy SHA-1 Certificate Fingerprint

```bash
./gradlew signingReport
```

Output sẽ hiển thị:

```
...
signingReport
Variant: debug
Config: debug
Store: ~/.android/debug.keystore
Alias: AndroidDebugKey
MD5: AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99
SHA1: ABCDEF0123456789ABCDEF0123456789ABCDEF01
SHA-256: ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789
...
```

Sao chép **SHA1** value (dạng hex 40 ký tự) vào form Firebase.

### Kích Hoạt Firebase Services

Sau khi app đã đăng ký, kích hoạt các dịch vụ sau:

#### 4.1 Realtime Database

1. **Firebase Console** → **Build** → **Realtime Database**
2. Nhấn **Create Database**
3. Chọn **Asia-southeast1** (Singapore)
4. Chọn **Start in test mode** (để phát triển)
5. Nhấn **Enable**

**Quản lý quyền:** Vào **Rules** tab:
```json
{
  "rules": {
    ".read": "root.child('users').child(auth.uid).exists()",
    ".write": "root.child('users').child(auth.uid).exists()"
  }
}
```

#### 4.2 Cloud Storage

1. **Firebase Console** → **Build** → **Storage**
2. Nhấn **Get Started**
3. Chọn khu vực `asia-southeast1`
4. Chọn **Start in test mode**
5. Nhấn **Done**

#### 4.3 Authentication

1. **Firebase Console** → **Build** → **Authentication**
2. Nhấn **Get Started**
3. Enable **Google Sign-In**:
   - Click **Google** on the list
   - **Status:** On
   - Nhấn **Save**
4. Cụ thể, thêm email support nếu cần

#### 4.4 Cloud Messaging (Optional)

Nếu muốn push notification:
1. **Firebase Console** → **Build** → **Cloud Messaging**
2. Chọn **Android** platform
3. Firebase tự động cấp **Server key**

### File google-services.json

**⚠️ CẢNH BÁO QUAN TRỌNG:**

File `google-services.json` chứa khóa công khai Firebase và phải **KHÔNG BAO GIỜ** commit vào Git (nó đã được thêm vào `.gitignore`).

Cấu trúc cơ bản:
```json
{
  "project_info": {
    "project_number": "123456789012",
    "firebase_url": "https://your-project-id-default-rtdb.region.firebasedatabase.app",
    "project_id": "your-project-id",
    "storage_bucket": "your-project-id.firebasestorage.app"
  },
  "client": [
    {
      "client_info": {
        "mobilesdk_app_id": "1:123456789012:android:abc123def456ghi789",
        "android_client_info": {
          "package_name": "com.nguyendevs.ecolens"
        }
      },
      "oauth_client": [
        {
          "client_id": "123456789012-abcdefghijklmnopqrstuvwxyz123456.apps.googleusercontent.com",
          "client_type": 1,
          "android_info": {
            "package_name": "com.nguyendevs.ecolens",
            "certificate_hash": "ABCDEF0123456789ABCDEF0123456789ABCDEF01"
          }
        }
      ],
      "api_key": [
        {
          "current_key": "AIzaSyXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
        }
      ]
    }
  ],
  "configuration_version": "1"
}
```

Đặt file này vào: `app/google-services.json`

---

## 5. Cấu Hình Cloudflare Worker

EcoLens sử dụng **Cloudflare Workers** làm API Gateway để quản lý Gemini API keys, xác thực HMAC, và rate limiting.

### Kiến Trúc Workers

```
CloudFlare-Worker/
├── EcoLens_Worker/              # Main API Gateway
│   ├── src/worker.js            # Worker code
│   ├── wrangler.toml            # Configuration
│   └── .wrangler/               # Build output
└── EcoLens_Inaturalist_Renewer/ # Token renewal
    ├── scripts/renew.js         # Renewal logic
    └── .github/workflows/       # GitHub Actions
```

### Thiết Lập Wrangler CLI

1. **Cài đặt Wrangler** (nếu chưa):
   ```bash
   npm install -g @cloudflare/wrangler
   ```

2. **Đăng nhập vào Cloudflare**:
   ```bash
   wrangler login
   ```
   Browser sẽ mở, đăng nhập bằng tài khoản Cloudflare

3. **Xác nhận**:
   ```bash
   wrangler whoami
   ```

### Cấu Hình wrangler.toml

File `CloudFlare-Worker/EcoLens_Worker/wrangler.toml`:

```toml
name = "ecolens"
main = "src/worker.js"
compatibility_date = "2025-12-03"

# KV Namespace cho iNaturalist token
kv_namespaces = [
    { binding = "INATURALIST_KV", id = "abc123def456ghi789jkl012mno345pq" }
]

[env.production]
name = "ecolens"

[observability.logs]
enabled = true
persist = true
```

### Biến Môi Trường & Secrets

Worker sử dụng các biến từ Cloudflare Dashboard:

| Biến | Loại | Mục Đích | Nơi Thiết Lập |
|------|------|---------|-------------|
| `GEMINI_API_KEY` | Secret | API key cho Google Gemini | Dashboard > Variables |
| `INATURALIST_KV` | KV Namespace | Lưu token iNaturalist | wrangler.toml |

**Thiết lập tại Cloudflare Dashboard:**

1. **Cloudflare Dashboard** → **Workers & Pages**
2. Chọn worker `ecolens`
3. **Settings** → **Variables**
4. Nhấn **Add secret**:
   ```
   Variable name: GEMINI_API_KEY
   Secret value: (paste your Gemini API key)
   ```
5. Nhấn **Deploy**

**Hoặc dùng Wrangler CLI:**

```bash
cd CloudFlare-Worker/EcoLens_Worker
wrangler secret put GEMINI_API_KEY
# Nhập API key khi được prompt
```

### Chạy Worker Cục Bộ

```bash
cd CloudFlare-Worker/EcoLens_Worker
wrangler dev
```

Worker sẽ chạy tại `http://localhost:8787`

Test:
```bash
curl -X POST http://localhost:8787/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Hello"}'
```

### Triển Khai Worker

```bash
cd CloudFlare-Worker/EcoLens_Worker
wrangler deploy
```

Worker sẽ được deployed tới: `https://your-worker-name.your-domain.workers.dev/`

### Worker Chức Năng Chính

**EcoLens_Worker** thực hiện:

1. **Gemini API Proxy**
   - Quản lý pool 30+ Gemini API keys
   - Auto-rotate khi hit rate limits (429)
   - Blacklist keys khi daily quota exceeded

2. **HMAC-SHA256 Authentication**
   - Xác minh signature từ Android app
   - Ngăn replay attacks bằng timestamp + request ID
   - Chỉ cho phép requests từ app đúng (`com.nguyendevs.ecolens`)

3. **Rate Limiting**
   - Max 100 requests/hour per IP
   - Rate limit headers trong response

4. **iNaturalist Token Management**
   - Relay requests tới iNaturalist API
   - Lưu token vào KV Store

---

## 6. Thiết Lập GitHub Actions / Workflows

Workflows nằm trong `CloudFlare-Worker/EcoLens_Inaturalist_Renewer/.github/workflows/`

### Workflow: renew-token.yml

File: `CloudFlare-Worker/EcoLens_Inaturalist_Renewer/.github/workflows/renew-token.yml`

```yaml
name: Renew iNaturalist Token

on:
  schedule:
    - cron: '0 18 * * *'  # Hàng ngày lúc 6 PM UTC
  workflow_dispatch:       # Manual trigger

jobs:
  renew:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Login & push token to Cloudflare KV
        run: node scripts/renew.js
        env:
          INAT_EMAIL:         ${{ secrets.INAT_EMAIL }}
          INAT_PASSWORD:      ${{ secrets.INAT_PASSWORD }}
          CF_ACCOUNT_ID:      ${{ secrets.CF_ACCOUNT_ID }}
          CF_KV_NAMESPACE_ID: ${{ secrets.CF_KV_NAMESPACE_ID }}
          CF_API_TOKEN:       ${{ secrets.CF_API_TOKEN }}
```

**Chức năng:**
- Chạy tự động hàng ngày lúc 6 PM UTC
- Có thể trigger thủ công từ GitHub Actions tab
- Gọi `scripts/renew.js` để refresh iNaturalist token
- Lưu token mới vào Cloudflare KV Store

### Cấu Hình GitHub Secrets

Để workflow hoạt động, cồng GitHub Secrets vào repository:

1. **GitHub Repository** → **Settings** → **Secrets and variables** → **Actions**
2. Nhấn **New repository secret** cho mỗi secret:

| Secret Name | Giá Trị | Lấy Từ Đâu |
|-------------|--------|----------|
| `INAT_EMAIL` | Email iNaturalist | iNaturalist account |
| `INAT_PASSWORD` | Password iNaturalist | iNaturalist account |
| `CF_ACCOUNT_ID` | Cloudflare Account ID | Cloudflare Dashboard > Account |
| `CF_KV_NAMESPACE_ID` | KV Namespace ID | Cloudflare Dashboard > Workers > KV |
| `CF_API_TOKEN` | Cloudflare API Token | Cloudflare Dashboard > My Profile > API Tokens |

**Tạo Cloudflare API Token:**

1. **Cloudflare Dashboard** → **My Profile** → **API Tokens**
2. Nhấn **Create Token**
3. Chọn template **Edit Cloudflare Worker** hoặc **All Account Services**
4. Nhấn **Create** và copy token
5. Paste vào GitHub Secrets `CF_API_TOKEN`

### Trigger Workflow Thủ Công

1. **GitHub Repository** → **Actions**
2. Chọn workflow **Renew iNaturalist Token**
3. Nhấn **Run workflow** → **Run workflow**

Workflow sẽ chạy ngay lập tức.

---

## 7. Bảng Tham Chiếu Biến Môi Trường

| Tên Biến | Nơi Sử Dụng | Cách Thiết Lập | Ví Dụ | Bắt Buộc? |
|---------|-----------|---------------|--------|---------|
| `WORKER_URL` | `gradle.properties` | Thêm vào `gradle.properties` | `https://your-worker.your-domain.workers.dev/` | ✓ Yes |
| `FIREBASE_URL` | `gradle.properties` | Thêm vào `gradle.properties` | `https://your-project-id-default-rtdb.region.firebasedatabase.app/` | ✓ Yes |
| `APP_SECRET` | `gradle.properties` | CMAKE args, chuyền lúc build | `YourVerySecureRandomString...` | ✓ Yes |
| `GEMINI_API_KEY` | Cloudflare Worker | Dashboard > Settings > Variables | `AIzaSyXXXXXXXXXXXXXXXXXX...` | ✓ Yes |
| `INATURALIST_KV` | Cloudflare Worker | `wrangler.toml` kv_namespaces | `abc123def456ghi789jkl...` | ✓ Yes |
| `INAT_EMAIL` | GitHub Actions Secret | **Settings** > **Secrets** | `your-email@inaturalist.com` | ✓ Yes* |
| `INAT_PASSWORD` | GitHub Actions Secret | **Settings** > **Secrets** | `yourSecurePassword123` | ✓ Yes* |
| `CF_ACCOUNT_ID` | GitHub Actions Secret | **Settings** > **Secrets** | `abc123def456ghi789jkl...` | ✓ Yes* |
| `CF_KV_NAMESPACE_ID` | GitHub Actions Secret | **Settings** > **Secrets** | `abc123def456ghi789jkl...` | ✓ Yes* |
| `CF_API_TOKEN` | GitHub Actions Secret | **Settings** > **Secrets** | `v1.0_abc123def456ghi789...` | ✓ Yes* |

*Chỉ cần thiết cho quy trình "Gia hạn token iNaturalist"

---

## 8. Các Vấn Đề Thường Gặp & Cách Khắc Phục

### 8.1 Lỗi: "Failed to resolve google-services.json"

**Triệu chứng:**
```
Error: Failed to resolve com.google.gms:google-services
No matching configuration of project : com.google.gms:google-services
```

**Nguyên nhân:** File `app/google-services.json` bị thiếu hoặc sai vị trí

**Khắc phục:**
```bash
# Xác minh file tồn tại
ls -la app/google-services.json

# Nếu thiếu, download từ Firebase Console:
# Firebase Console > Project > App Settings > Download google-services.json
# Sao chép vào: app/google-services.json

# Sync lại Gradle
./gradlew sync
```

### 8.2 Lỗi: "CMake build failed"

**Triệu chứng:**
```
C/C++: Error in cmake execution
...error: APP_SECRET undefined
```

**Nguyên nhân:** `APP_SECRET` không được truyền từ Gradle vào CMake

**Khắc phục:**
```bash
# Xác minh gradle.properties chứa APP_SECRET
cat gradle.properties | grep APP_SECRET

# Nếu thiếu, thêm vào gradle.properties:
# APP_SECRET=EcoLensq8NZ7rP2KxA9D4FJcE5WnB0Hf6sVYt3UeRLaQmC1bKXdS9N7pT

# Clean rebuild
./gradlew clean
./gradlew assembleDebug
```

### 8.3 Lỗi: "Incompatible API Version" trên Firebase

**Triệu chứng:**
```
PluginException: Plugin not found: com.google.gms.google-services version 4.4.2
```

**Nguyên nhân:** Plugin Google Services chưa được cài hoặc phiên bản không khớp

**Khắc phục:**
```bash
# Sync Gradle files
./gradlew sync

# Nếu vẫn lỗi, rebuild:
./gradlew clean
./gradlew assembleDebug
```

### 8.4 Lỗi: "HMAC Signature Verification Failed"

**Triệu chứng:**
```
Worker response: 401 Unauthorized - HMAC signature mismatch
```

**Nguyên nhân:** `APP_SECRET` hay `WORKER_URL` không match giữa app và worker

**Khắc phục:**
1. Xác minh `APP_SECRET` trong `gradle.properties` match với worker config
2. Xác minh `WORKER_URL` là URL của worker đúng
3. Rebuild app:
   ```bash
   ./gradlew clean assembleDebug
   ```
4. Test worker cục bộ:
   ```bash
   cd CloudFlare-Worker/EcoLens_Worker
   wrangler dev
   ```

### 8.5 Lỗi: "Rate Limit Exceeded" từ Gemini API

**Triệu chứng:**
```
Worker error: 429 Too Many Requests - Quota exceeded
```

**Nguyên nhân:** Tất cả 30+ Gemini API keys đã hit quota hoặc rate limit

**Khắc phục:**
1. **Thêm Gemini API Keys mới** vào worker:
   - Cloudflare Dashboard > Worker Settings > Variables
   - Thêm key mới vào `GEMINI_API_KEY` (format dấu phân cách)

2. **Kiểm tra status của worker:**
   ```bash
   cd CloudFlare-Worker/EcoLens_Worker
   wrangler tail
   ```

3. **Giảm tần suất request** từ app hoặc implement caching

### 8.6 Lỗi: "gradlew permission denied" (Windows)

**Triệu chứng:**
```
'gradlew' is not recognized as an internal or external command
```

**Khắc phục:**

Đảm bảo bạn đang chạy lệnh từ thư mục gốc của project:

```bash
# Kiểm tra vị trí hiện tại
cd D:\Source Code\EcoLens

# Chạy Gradle với đặc quyền
./gradlew --version

# Nếu vẫn không hoạt động, thử chạy PowerShell as Administrator
```

Nếu lỗi vẫn xảy ra, cài đặt Windows Build Tools hoặc sử dụng WSL (Windows Subsystem for Linux).

---

## 9. Danh Sách Kiểm Tra Lần Đầu Tiên (First Run Checklist)

Làm theo các bước này để đưa project từ zero tới chạy được:

### Bước 1: Chuẩn Bị Môi Trường

- [ ] Cài đặt Android Studio (Hedgehog 2023.1.1+)
- [ ] Cài đặt JDK 11 (hoặc sử dụng bundled JDK trong Android Studio)
- [ ] Cài đặt Node.js 18+ LTS
- [ ] Cài đặt Wrangler CLI: `npm install -g @cloudflare/wrangler`
- [ ] Cài đặt Git

### Bước 2: Clone Repository

```bash
git clone https://github.com/NguyenDevs/EcoLens.git
cd EcoLens
```

- [ ] Repository cloned thành công

### Bước 3: Android App Configuration

- [ ] Đặt `sdk.dir` trong `local.properties`
- [ ] Xác minh `gradle.properties` chứa `WORKER_URL`, `FIREBASE_URL`, `APP_SECRET`
- [ ] Open project trong Android Studio
- [ ] Hệ thống tự động download SDK, Gradle (chờ 5-10 phút)

### Bước 4: Firebase Setup

- [ ] Tạo Firebase Project tại [console.firebase.google.com](https://console.firebase.google.com/)
- [ ] Thêm Android app với package `com.nguyendevs.ecolens`
- [ ] Lấy SHA-1 từ `./gradlew signingReport`
- [ ] Download `google-services.json`
- [ ] Sao chép vào `app/google-services.json`
- [ ] Kích hoạt: Realtime Database, Cloud Storage, Authentication (Google Sign-In)
- [ ] Cập nhật `gradle.properties` với Firebase URL nếu khác

### Bước 5: Cloudflare Worker Setup

```bash
wrangler login
cd CloudFlare-Worker/EcoLens_Worker
wrangler dev
```

- [ ] Đợi prompt "Ready on http://localhost:8787"
- [ ] Test worker: `curl http://localhost:8787` (phải trả response)

### Bước 6: Cấu Hình Worker Secrets

```bash
wrangler secret put GEMINI_API_KEY
# Paste Gemini API key
```

- [ ] GEMINI_API_KEY được set thành công

### Bước 7: Build App

```bash
./gradlew assembleDebug
```

- [ ] Build thành công (không có error, chỉ có thể có warning)
- [ ] APK được tạo tại: `app/build/outputs/apk/debug/app-debug.apk`

### Bước 8: Chạy trên Emulator hoặc Device

**Emulator:**
```bash
emulator -avd Pixel_6_API_34 &
./gradlew installDebug
```

**Physical Device:**
```bash
adb devices  # Xác minh device kết nối
./gradlew installDebug
```

- [ ] App installed thành công
- [ ] App khởi động mà không crash

### Bước 9: Kiểm Tra Kết Nối

- [ ] Đăng nhập với Google bằng Firebase
- [ ] Chụp ảnh hoặc chọn từ gallery
- [ ] Testing xác định loài (phải gọi tới worker)

### Bước 10: GitHub Actions Setup (Optional)

- [ ] Fork repository (nếu đóng góp)
- [ ] Thêm GitHub Secrets: `INAT_EMAIL`, `INAT_PASSWORD`, `CF_ACCOUNT_ID`, `CF_KV_NAMESPACE_ID`, `CF_API_TOKEN`
- [ ] Trigger workflow **Renew iNaturalist Token** thủ công
- [ ] Kiểm tra log workflow được chạy thành công

---

## 10. Các Lệnh Hữu Ích

```bash
# ===== Android Gradle =====

# Sync Gradle files
./gradlew sync

# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install on connected device/emulator
./gradlew installDebug

# Run all tests
./gradlew test

# Get signing report (SHA-1, SHA-256, etc.)
./gradlew signingReport

# ===== Cloudflare Worker =====

# Login to Cloudflare
wrangler login

# Run worker locally
cd CloudFlare-Worker/EcoLens_Worker && wrangler dev

# Deploy worker
wrangler deploy

# View worker logs in real-time
wrangler tail

# Create KV namespace
wrangler kv:namespace create "INATURALIST_KV"

# Manage KV values
wrangler kv:key put --namespace-id "a3441a2cf..." "key_name" "value"
wrangler kv:key get --namespace-id "a3441a2cf..." "key_name"

# ===== Git =====

# Clone repository
git clone https://github.com/NguyenDevs/EcoLens.git

# Create feature branch
git checkout -b feature/your-feature

# Commit changes
git add .
git commit -m "feat: description"

# Push to remote
git push origin feature/your-feature

# ===== Android Device Management =====

# List connected devices
adb devices

# Forward port for remote debugging
adb forward tcp:5555 tcp:5555

# Clear app data
adb shell pm clear com.nguyendevs.ecolens

# View logcat
adb logcat -s EcoLens

# Take screenshot
adb shell screencap -p /sdcard/screenshot.png
adb pull /sdcard/screenshot.png ./
```

---

## 11. Tài Liệu Tham Khảo & Liên Kết

- **Android Documentation:** https://developer.android.com/docs
- **Firebase Console:** https://console.firebase.google.com/
- **Cloudflare Workers:** https://workers.cloudflare.com/
- **Google Gemini AI:** https://ai.google.dev/
- **iNaturalist API:** https://www.inaturalist.org/pages/developers
- **GBIF API:** https://www.gbif.org/developer/summary
- **IUCN Red List:** https://www.iucnredlist.org/

---

## 12. Liên Hệ & Hỗ Trợ

Nếu gặp vấn đề:

1. **GitHub Issues:** https://github.com/NguyenDevs/EcoLens/issues
2. **Email:** tainguyen.devs@gmail.com
3. **Android Logcat** - Kiểm tra log:
   ```bash
   adb logcat | grep EcoLens
   ```
4. **Cloudflare Worker Logs:**
   ```bash
   wrangler tail
   ```

---

**Hạn chế kỹ thuật:**

⚠️ TODO: Chi tiết về cấu hình SSL/TLS certificates cho worker  
⚠️ TODO: Hướng dẫn setup CI/CD pipeline cho releases  
⚠️ TODO: Proguard/R8 obfuscation rules chi tiết cho release build  
⚠️ TODO: Backup & recovery strategy cho Firebase data  

---

Tài liệu này cập nhật lần cuối: **April 27, 2026**  
Project: **EcoLens v1.0**  
Team: **NguyenDevs**
