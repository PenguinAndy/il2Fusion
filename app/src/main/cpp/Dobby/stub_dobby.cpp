#include "dobby.h"
#include <android/log.h>

#define LOG_TAG "DobbyStub"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" {

int DobbyHook(void *address, void *fake_func, void **out_origin_func) {
  LOGE("DobbyStub: DobbyHook called but stubbed");
  return -1;
}

int DobbyInstrument(void *address, dobby_instrument_callback_t pre_handler) {
  LOGE("DobbyStub: DobbyInstrument called but stubbed");
  return -1;
}

int DobbyDestroy(void *address) { return 0; }

int DobbyCodePatch(void *address, uint8_t *buffer, uint32_t buffer_size) {
  LOGE("DobbyStub: DobbyCodePatch called but stubbed");
  return -1;
}

const char *DobbyGetVersion() { return "DobbyStub"; }

void *DobbySymbolResolver(const char *image_name, const char *symbol_name) {
  (void)image_name;
  (void)symbol_name;
  return nullptr;
}

int DobbyImportTableReplace(char *image_name, char *symbol_name, void *fake_func, void **orig_func) {
  (void)image_name;
  (void)symbol_name;
  (void)fake_func;
  (void)orig_func;
  LOGE("DobbyStub: ImportTableReplace called but stubbed");
  return -1;
}

void dobby_set_near_trampoline(bool) {}
void dobby_register_alloc_near_code_callback(dobby_alloc_near_code_callback_t) {}
void dobby_set_options(bool, dobby_alloc_near_code_callback_t) {}

}
