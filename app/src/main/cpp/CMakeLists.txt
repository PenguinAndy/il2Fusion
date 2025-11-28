cmake_minimum_required(VERSION 3.22.1)

project("TextExtractTool")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Dobby inline hook stub (replace with full upstream to enable real hooks)
add_library(dobby STATIC Dobby/stub_dobby.cpp)
target_include_directories(dobby PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/Dobby/include)

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
