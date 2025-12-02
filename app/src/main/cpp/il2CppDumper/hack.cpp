#include "hack.h"

#include <cstdio>
#include <cstring>
#include <thread>
#include <unistd.h>

#include "il2cpp_dump.h"
#include "../utils/log.h"
#include "xdl/include/xdl.h"

void hack_start(const char *game_data_dir) {
    bool load = false;
    for (int i = 0; i < 10; i++) {
        void *handle = xdl_open("libil2cpp.so", 0);
        if (handle) {
            load = true;
            il2cpp_api_init(handle);
            il2cpp_dump(game_data_dir);
            break;
        } else {
            sleep(1);
        }
    }
    if (!load) {
        LOGI("libil2cpp.so not found in thread %d", gettid());
    }
}

void hack_prepare(const char *game_data_dir, void *data, size_t length) {
    (void) data;
    (void) length;
    LOGI("hack_prepare -> %s", game_data_dir ? game_data_dir : "<null>");
    std::thread hack_thread(hack_start, game_data_dir);
    hack_thread.detach();
}
