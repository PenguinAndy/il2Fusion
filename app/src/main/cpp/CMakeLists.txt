cmake_minimum_required(VERSION 3.22.1)

project("TextExtractTool")

# Dobby inline hook engine
add_subdirectory(Dobby)

add_library(
        native_hook
        SHARED
        native_hook.cpp
)

target_include_directories(
        native_hook PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/Dobby/include
)

target_link_libraries(
        native_hook
        dobby
        log
)
