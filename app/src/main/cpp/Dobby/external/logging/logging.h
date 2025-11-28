#pragma once
#include <android/log.h>
#include <cstdlib>

#ifndef LOG_TAG_DOBBY
#define LOG_TAG_DOBBY "Dobby"
#endif

#ifdef DOBBY_LOGGING_DISABLE
#define DEBUG_LOG(...)
#define INFO_LOG(...)
#define WARNING_LOG(...)
#define ERROR_LOG(...)
#else
#define DEBUG_LOG(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG_DOBBY, __VA_ARGS__)
#define INFO_LOG(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG_DOBBY, __VA_ARGS__)
#define WARNING_LOG(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG_DOBBY, __VA_ARGS__)
#define ERROR_LOG(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG_DOBBY, __VA_ARGS__)
#endif
