set(LOGGING_SRC ${CMAKE_CURRENT_SOURCE_DIR}/logging.cpp)
add_library(logging STATIC ${LOGGING_SRC})
target_include_directories(logging PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/include)
