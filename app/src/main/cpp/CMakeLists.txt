cmake_minimum_required(VERSION 3.22.1)

project("ecolens")

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

add_library(
        native-lib
        SHARED
        native-lib.cpp
)

target_compile_definitions(
        native-lib
        PRIVATE APP_SECRET="${APP_SECRET}"
)

find_library(log-lib log)

target_link_libraries(
        native-lib
        ${log-lib}
)

if (ANDROID)
    add_android_openssl_libraries(native-lib)
endif()