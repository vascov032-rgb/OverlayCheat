#include <jni.h>
#include <android/log.h>
#include <dlfcn.h>
#include <unistd.h>
#include <cmath>
#include <cstring>
#include <string>
#include <vector>
#include <fstream>
#include <dirent.h>
#include <sys/uio.h>
#include <pthread.h>
#include <GLES2/gl2.h>

#define LOG_TAG "FF_OVERLAY"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

uintptr_t g_view_matrix = 0;
bool g_running = true;

extern "C" ssize_t process_vm_readv(pid_t pid, const struct iovec *local_iov, unsigned long liovcnt, const struct iovec *remote_iov, unsigned long riovcnt, unsigned long flags);

int get_pid(const char* package_name) {
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
                    if (line.find(package_name) != std::string::npos) {
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

bool ReadMem(int pid, uintptr_t addr, void* buffer, size_t size) {
    struct iovec local{ buffer, size };
    struct iovec remote{ (void*)addr, size };
    return process_vm_readv(pid, &local, 1, &remote, 1, 0) == size;
}

bool IsValidMatrix(float* m) {
    float len0 = m[0]*m[0] + m[1]*m[1] + m[2]*m[2];
    float len1 = m[4]*m[4] + m[5]*m[5] + m[6]*m[6];
    float len2 = m[8]*m[8] + m[9]*m[9] + m[10]*m[10];
    if (std::abs(len0 - 1.0f) > 0.05f || std::abs(len1 - 1.0f) > 0.05f || std::abs(len2 - 1.0f) > 0.05f) return false;
    float dot01 = m[0]*m[4] + m[1]*m[5] + m[2]*m[6];
    float dot02 = m[0]*m[8] + m[1]*m[9] + m[2]*m[10];
    float dot12 = m[4]*m[8] + m[5]*m[9] + m[6]*m[10];
    if (std::abs(dot01) > 0.05f || std::abs(dot02) > 0.05f || std::abs(dot12) > 0.05f) return false;
    if (std::abs(m[3]) > 0.05f || std::abs(m[7]) > 0.05f || std::abs(m[11]) > 0.05f) return false;
    if (std::abs(m[15] - 1.0f) > 0.05f) return false;
    if (std::abs(m[14]) < 0.5f) return false;
    return true;
}

void* ScannerThread(void*) {
    while (g_running) {
        int pid = get_pid("com.dts.freefireth");
        if (pid != -1) {
            std::ifstream maps("/proc/" + std::to_string(pid) + "/maps");
            std::string line;
            while (std::getline(maps, line)) {
                if (line.find("rw-p") == std::string::npos) continue;
                uintptr_t start, end;
                sscanf(line.c_str(), "%lx-%lx", &start, &end);
                size_t size = end - start;
                if (size > 50 * 1024 * 1024) continue;
                std::vector<char> buffer(size);
                if (!ReadMem(pid, start, buffer.data(), size)) continue;
                for (size_t i = 0; i <= size - 64; i += 4) {
                    float* m = (float*)&buffer[i];
                    if (IsValidMatrix(m)) {
                        g_view_matrix = start + i;
                        LOGI("ViewMatrix encontrada em: 0x%lX", g_view_matrix);
                        goto found;
                    }
                }
            }
            found:
            sleep(3);
        }
        sleep(1);
    }
    return nullptr;
}

extern "C" JNIEXPORT void JNICALL
Java_com_cheat_overlay_OverlayService_StartCheat(JNIEnv*, jobject) {
    LOGI("Iniciando Engine C++...");
    g_running = true;
    pthread_t thread;
    pthread_create(&thread, nullptr, ScannerThread, nullptr);
}

extern "C" JNIEXPORT void JNICALL
Java_com_cheat_overlay_OverlayService_StopCheat(JNIEnv*, jobject) {
    g_running = false;
}
