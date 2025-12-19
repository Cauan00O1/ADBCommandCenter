#include <jni.h>
#include <cstring>
#include <cstdlib>
#include <cinttypes>
#include <android/log.h>
#include <openssl/curve25519.h>
#include <openssl/hkdf.h>
#include <openssl/evp.h>

#define LOG_TAG "AdbPairClientNative"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static constexpr spake2_role_t kClientRole = spake2_role_alice;
static const uint8_t kClientName[] = "adb pair client";
static const uint8_t kServerName[] = "adb pair server";
static constexpr size_t kHkdfKeyLength = 16;

struct PairingContextNative {
    SPAKE2_CTX *spake2_ctx;
    uint8_t key[SPAKE2_MAX_MSG_SIZE];
    size_t key_size;
    EVP_AEAD_CTX *aes_ctx;
    uint64_t dec_sequence;
    uint64_t enc_sequence;
};

extern "C" JNIEXPORT jlong JNICALL
Java_com_joaomgcd_adb_PairingContext_nativeConstructor(JNIEnv *env, jclass clazz, jboolean isClient, jbyteArray jPassword) {
    spake2_role_t spake_role = kClientRole;
    auto spake2_ctx = SPAKE2_CTX_new(spake_role, kClientName, sizeof(kClientName), kServerName, sizeof(kServerName));
    
    if (spake2_ctx == nullptr) return 0;

    auto pswd_size = env->GetArrayLength(jPassword);
    auto pswd = env->GetByteArrayElements(jPassword, nullptr);

    size_t key_size = 0;
    uint8_t key[SPAKE2_MAX_MSG_SIZE];
    int status = SPAKE2_generate_msg(spake2_ctx, key, &key_size, SPAKE2_MAX_MSG_SIZE, (uint8_t *) pswd, pswd_size);
    
    env->ReleaseByteArrayElements(jPassword, pswd, 0);

    if (status != 1 || key_size == 0) {
        SPAKE2_CTX_free(spake2_ctx);
        return 0;
    }

    auto ctx = (PairingContextNative *) malloc(sizeof(PairingContextNative));
    memset(ctx, 0, sizeof(PairingContextNative));
    ctx->spake2_ctx = spake2_ctx;
    memcpy(ctx->key, key, SPAKE2_MAX_MSG_SIZE);
    ctx->key_size = key_size;
    return (jlong) ctx;
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_joaomgcd_adb_PairingContext_nativeMsg(JNIEnv *env, jobject obj, jlong ptr) {
    auto ctx = (PairingContextNative *) ptr;
    jbyteArray our_msg = env->NewByteArray(ctx->key_size);
    env->SetByteArrayRegion(our_msg, 0, ctx->key_size, (jbyte *) ctx->key);
    return our_msg;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_joaomgcd_adb_PairingContext_nativeInitCipher(JNIEnv *env, jobject obj, jlong ptr, jbyteArray jTheirMsg) {
    auto ctx = (PairingContextNative *) ptr;
    auto their_msg_size = env->GetArrayLength(jTheirMsg);
    auto their_msg = env->GetByteArrayElements(jTheirMsg, nullptr);

    size_t key_material_len = 0;
    uint8_t key_material[SPAKE2_MAX_KEY_SIZE];
    int status = SPAKE2_process_msg(ctx->spake2_ctx, key_material, &key_material_len, sizeof(key_material), (uint8_t *) their_msg, their_msg_size);

    env->ReleaseByteArrayElements(jTheirMsg, their_msg, 0);

    if (status != 1) return JNI_FALSE;

    uint8_t key[kHkdfKeyLength];

    uint8_t info[] = "adb pairing_auth aes-128-gcm key";

    // Match Shizuku: Pass nullptr and 0 for salt
    if (HKDF(key, sizeof(key), EVP_sha256(), key_material, key_material_len, nullptr, 0, info, sizeof(info) - 1) != 1) {
        return JNI_FALSE;
    }

    ctx->aes_ctx = EVP_AEAD_CTX_new(EVP_aead_aes_128_gcm(), key, sizeof(key), EVP_AEAD_DEFAULT_TAG_LENGTH);
    return ctx->aes_ctx ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_joaomgcd_adb_PairingContext_nativeEncrypt(JNIEnv *env, jobject obj, jlong ptr, jbyteArray jIn) {
    auto ctx = (PairingContextNative *) ptr;
    auto in = env->GetByteArrayElements(jIn, nullptr);
    auto in_size = env->GetArrayLength(jIn);

    auto out_size = (size_t) in_size + EVP_AEAD_max_overhead(EVP_AEAD_CTX_aead(ctx->aes_ctx));
    uint8_t out[out_size];
    uint8_t nonce[12] = {0};
    memcpy(nonce, &ctx->enc_sequence, sizeof(ctx->enc_sequence));

    size_t written_sz;
    int status = EVP_AEAD_CTX_seal(ctx->aes_ctx, out, &written_sz, out_size, nonce, sizeof(nonce), (uint8_t *) in, in_size, nullptr, 0);

    env->ReleaseByteArrayElements(jIn, in, 0);

    if (!status) return nullptr;
    ++ctx->enc_sequence;

    jbyteArray jOut = env->NewByteArray(written_sz);
    env->SetByteArrayRegion(jOut, 0, written_sz, (jbyte *) out);
    return jOut;
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_joaomgcd_adb_PairingContext_nativeDecrypt(JNIEnv *env, jobject obj, jlong ptr, jbyteArray jIn) {
    auto ctx = (PairingContextNative *) ptr;
    auto in = env->GetByteArrayElements(jIn, nullptr);
    auto in_size = env->GetArrayLength(jIn);

    auto out_size = (size_t) in_size;
    uint8_t out[out_size];
    uint8_t nonce[12] = {0};
    memcpy(nonce, &ctx->dec_sequence, sizeof(ctx->dec_sequence));

    size_t written_sz;
    int status = EVP_AEAD_CTX_open(ctx->aes_ctx, out, &written_sz, out_size, nonce, sizeof(nonce), (uint8_t *) in, in_size, nullptr, 0);

    env->ReleaseByteArrayElements(jIn, in, 0);

    if (!status) return nullptr;
    ++ctx->dec_sequence;

    jbyteArray jOut = env->NewByteArray(written_sz);
    env->SetByteArrayRegion(jOut, 0, written_sz, (jbyte *) out);
    return jOut;
}

extern "C" JNIEXPORT void JNICALL
Java_com_joaomgcd_adb_PairingContext_nativeDestroy(JNIEnv *env, jobject obj, jlong ptr) {
    auto ctx = (PairingContextNative *) ptr;
    SPAKE2_CTX_free(ctx->spake2_ctx);
    if (ctx->aes_ctx) EVP_AEAD_CTX_free(ctx->aes_ctx);
    free(ctx);
}