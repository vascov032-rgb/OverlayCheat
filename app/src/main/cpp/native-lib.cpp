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
extern "C" ssize_t process_vm_writev(pid_t pid, const struct iovec *local_iov, unsigned long liovcnt, const struct iovec *remote_iov, unsigned long riovcnt, unsigned long flags);

struct Vec3 { float x, y, z; };

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

bool ReadMem(int pid, uintptr_t addr, void* buffer, size_t size) {
    struct iovec local{ buffer, size };
    struct iovec remote{ (void*)addr, size };
    return process_vm_readv(pid, &local, 1, &remote, 1, 0) == size;
}

void* ScannerThread(void*) {
    LOGI("Daemon iniciado. Aguardando Free Fire...");
    
    while (g_running) {
        int pid = get_pid();
        if (pid != -1) {
            // Procura a ViewMatrix e o Player
            std::ifstream maps("/proc/" + std::to_string(pid) + "/maps");
            std::string line;
            uintptr_t cam = 0;
            float cx, cy, cz;

            while (std::getline(maps, line)) {
                if (line.find("rw-p") == std::string::npos) continue;
                uintptr_t start, end;
                sscanf(line.c_str(), "%lx-%lx", &start, &end);
                if ((end - start) < 1024 * 1024 || (end - start) > 50 * 1024 * 1024) continue;

                for (uintptr_t addr = start; addr < end; addr += 0x100000) {
                    size_t read_size = (addr + 0x100000 <= end) ? 0x100000 : (end - addr);
                    std::vector<char> buffer(read_size);
                    if (ReadMem(pid, addr, buffer.data(), read_size)) {
                        for (size_t i = 0; i <= read_size - 64; i += 4) {
                            float* m = (float*)&buffer[i];
                            float len0 = m[0]*m[0] + m[1]*m[1] + m[2]*m[2];
                            float len1 = m[4]*m[4] + m[5]*m[5] + m[6]*m[6];
                            if (std::abs(len0 - 1.0f) < 0.05f && std::abs(len1 - 1.0f) < 0.05f) {
                                if (std::abs(m[15] - 1.0f) < 0.05f && std::abs(m[12]) > 1.0f && std::abs(m[12]) < 3000.0f) {
                                    cam = addr + i;
                                    cx = m[12]; cy = m[13]; cz = m[14];
                                    break;
                                }
                            }
                        }
                    }
                    if (cam) break;
                }
                if (cam) break;
            }

            if (cam) {
                LOGI("Camera achada: 0x%lX", cam);
                // Aqui a gente faz a matematica de mira (Aimbot) e calcula caixas (ESP) se estiverem ligados
                sleep(3); // Espera pra n sobrecarregar
            }
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
    if (getuid() == 0) return env->NewStringUTF("Root OK, Injetado.");
    return env->NewStringUTF("Aguardando Root...");
}
