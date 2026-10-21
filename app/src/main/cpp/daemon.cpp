#include <unistd.h>
#include <iostream>
#include <fstream>
#include <vector>
#include <cmath>
#include <sys/uio.h>
#include <dirent.h>
#include <cstring>
#include <cstdlib>

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
    if (std::abs(m[15] - 1.0f) > 0.05f) return false;
    if (std::abs(m[14]) < 0.5f) return false;
    return true;
}

int main() {
    // Roda para sempre como root
    while (true) {
        int pid = get_pid();
        if (pid != -1) {
            std::ifstream maps("/proc/" + std::to_string(pid) + "/maps");
            std::string line;
            uintptr_t found_matrix = 0;
            
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
                        found_matrix = start + i;
                        break;
                    }
                }
                if (found_matrix) break;
            }
            
            // Salva o endereço num arquivo que o App vai ler
            std::ofstream out("/data/local/tmp/ff_matrix.txt");
            if (found_matrix) {
                out << "0x" << std::hex << found_matrix;
            } else {
                out << "0x0";
            }
            out.close();
        }
        sleep(3);
    }
    return 0;
}
