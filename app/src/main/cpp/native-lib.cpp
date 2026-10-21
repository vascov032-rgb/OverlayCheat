#include <jni.h>
#include <android/log.h>

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_cheat_overlay_OverlayService_GetESPBox(JNIEnv* env, jobject) {
    jfloatArray result = env->NewFloatArray(4);
    // CAIXA FIXA NO MEIO DA TELA (TESTE)
    float temp[4] = {400.0f, 800.0f, 300.0f, 600.0f}; 
    env->SetFloatArrayRegion(result, 0, 4, temp);
    return result;
}

extern "C" JNIEXPORT void JNICALL
Java_com_cheat_overlay_OverlayService_StartCheat(JNIEnv*, jobject) {
    __android_log_print(ANDROID_LOG_INFO, "FF_OVERLAY", "StartCheat chamado");
}
