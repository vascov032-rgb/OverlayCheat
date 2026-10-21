#define _GNU_SOURCE
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

extern "C" ssize_t process_vm_readv(pid_t pid, const struct iovec *local_iov, unsigned long liovcnt, const struct iovec *remote_iov, unsigned long riovcnt, unsigned long flags);

int get_pid() {
    DIR* dir = opendir("/proc");
    if (!dir) return -1;
    struct dirent* entry;
    while ((entry = readdir(dir)) != NULL) {
        if (entry->d_type == DT_DIR) {
            bool is_num = true;
            for (int i = 0; entry->d_name[i] != '\0'; i++) {
                if (!isdigit(entry->d_name[i])) { is_num = false; break; }
            }
            if (is_num) {
                char cmdline_path[256];
                snprintf(cmdline_path, sizeof(cmdline_path), "/proc/%s/cmdline", entry->d_name);
                std::ifstream f(cmdline_path);
                if (f) {
                    std::string line;
                    std::getline(f, line);
                    if (line.find("com.dts.freefireth") != std::string::npos || line.find("com.dts.freefiremax") != std::string::npos) {
                        closedir(dir);
                        return atoi(entry->d_name);
                    }
                }
            }
        }
    }
    closedir(dir);
    return -1;
}

void* ScannerThread(void*) {
    LOGI("Thread iniciada. Verificando Root...");
    if (getuid() != 0) {
        LOGE("SEM ROOT!");
    }

    while (g_running) {
        int pid = get_pid();
        if (pid != -1) {
            // Aqui a gente coloca a lógica de scan segura que já fizemos
            // e atualiza as variáveis para o ESP desenhar
            sleep(1);
        }
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

extern "C" JNIEXPORT jstring JNICALL
Java_com_cheat_overlay_OverlayService_GetCheatStatus(JNIEnv* env, jobject) {
    if (getuid() == 0) {
        return env->NewStringUTF("Root OK, Injetado.");
    } else {
        return env->NewStringUTF("Aguardando Root...");
    }
}
