#include <jni.h>
#include <string>
#include <cstring>          // Thêm dòng này cho strlen, sprintf
#include <openssl/hmac.h>
#include <openssl/sha.h>

#ifndef APP_SECRET
#error "APP_SECRET is not defined"
#endif

static const char* SECRET = APP_SECRET;

extern "C"
JNIEXPORT jstring JNICALL
Java_com_nguyendevs_ecolens_network_NativeSecurityManager_calculateHMAC(
        JNIEnv* env,
        jobject /* this */,
        jstring jMessage
) {
    const char* message = env->GetStringUTFChars(jMessage, nullptr);
    if (message == nullptr) {
        return nullptr;
    }

    unsigned char hash[SHA256_DIGEST_LENGTH];

    HMAC(
            EVP_sha256(),
            SECRET,
            strlen(SECRET),
            (const unsigned char*) message,
            strlen(message),
            hash,
            nullptr
    );

    env->ReleaseStringUTFChars(jMessage, message);

    // Tạo hex string an toàn hơn (tránh sprintf không kiểm soát)
    char hex[SHA256_DIGEST_LENGTH * 2 + 1];
    for (int i = 0; i < SHA256_DIGEST_LENGTH; i++) {
        snprintf(hex + (i * 2), 3, "%02x", hash[i]);  // snprintf an toàn hơn
    }
    hex[SHA256_DIGEST_LENGTH * 2] = '\0';

    return env->NewStringUTF(hex);
}