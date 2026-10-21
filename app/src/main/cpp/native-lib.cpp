#include <jni.h>
#include <android/log.h>
#include <unistd.h>
#include <cmath>
#include <cstring>
#include <string>
#include <vector>
#include <fstream>
#include <dirent.h>
#include <sys/uio.h>
#include <pthread.h>

#define LOG_TAG "FF_OVERLAY"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

bool g_running = true;
// Forçando a caixa a ficar no meio da tela (para teste)
float outX = 400.0f;
float outY = 800.0f;
float outW = 300.0f;
float outH = 600.0f;

void* ScannerThread(void*) {
    LOGI("Motor de teste iniciado.");
    while (g_running) {
        // Apenas mantém a thread viva
        sleep(1);
    }
    return nullptr;
}

extern "C" JNIEXPORT void JNICALL
Java_com_cheat_overlay_OverlayService_StartCheat(JNIEnv*, jobject) {
    g_running = true;
    pthread_t thread;
    pthread_create(&thread, nullptr, ScannerThread, nullptr);
}

// Retorna a caixa fixa para o Java
extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_cheat_overlay_OverlayService_GetESPBox(JNIEnv* env, jobject) {
    jfloatArray result = env->NewFloatArray(4);
    float temp[4] = {outX, outY, outW, outH};
    env->SetFloatArrayRegion(result, 0, 4, temp);
    return result;
}
