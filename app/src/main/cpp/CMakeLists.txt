cmake_minimum_required(VERSION 3.22.1)

project("ecolens")

# Cấu hình tải và tích hợp thư viện OpenSSL cho Android
if (ANDROID)
    include(FetchContent)
    FetchContent_Declare(
        android_openssl
        DOWNLOAD_EXTRACT_TIMESTAMP true
        URL https://github.com/KDAB/android_openssl/archive/refs/heads/master.zip
    )
    FetchContent_MakeAvailable(android_openssl)
    include(${android_openssl_SOURCE_DIR}/android_openssl.cmake)
endif()

# Khởi tạo thư viện native
add_library(
        native-lib
        SHARED
        native-lib.cpp
)

# Truyền biến APP_SECRET vào code C++
target_compile_definitions(
        native-lib
        PRIVATE APP_SECRET="${APP_SECRET}"
)

# Tìm thư viện log hệ thống Android
find_library(log-lib log)

# Liên kết các thư viện (Đã thêm PRIVATE để khắc phục lỗi xung đột signature)
target_link_libraries(
        native-lib
        PRIVATE
        ${log-lib}
)

# Liên kết thư viện OpenSSL vào native-lib
if (ANDROID)
    add_android_openssl_libraries(native-lib)
endif()