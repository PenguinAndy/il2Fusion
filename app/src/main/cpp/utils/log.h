#ifndef IL2FUSION_LOG_H
#define IL2FUSION_LOG_H

#include <android/log.h>

#define LOG_TAG "[il2Fusion]"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#endif  // IL2FUSION_LOG_H
