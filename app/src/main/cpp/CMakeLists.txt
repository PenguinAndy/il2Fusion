cmake_minimum_required(VERSION 3.22.1)

project("TextExtractTool")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(
        native_hook
        SHARED
        native_hook.cpp
        il2CppDumper/hack.cpp
        il2CppDumper/il2cpp_dump.cpp
        il2CppDumper/xdl/xdl.c
        il2CppDumper/xdl/xdl_iterate.c
        il2CppDumper/xdl/xdl_linker.c
        il2CppDumper/xdl/xdl_lzma.c
        il2CppDumper/xdl/xdl_util.c
        utils/db.cpp
        utils/sqlite3.c
        utils/utils.cpp
)

target_include_directories(
        native_hook PRIVATE
        ${CMAKE_SYSROOT}/usr/include
        ${CMAKE_CURRENT_SOURCE_DIR}
        ${CMAKE_CURRENT_SOURCE_DIR}/utils
        ${CMAKE_CURRENT_SOURCE_DIR}/il2CppDumper
        ${CMAKE_CURRENT_SOURCE_DIR}/il2CppDumper/xdl/include
)

add_library(DOBBY_LIB STATIC IMPORTED)

set_target_properties(DOBBY_LIB PROPERTIES IMPORTED_LOCATION
${CMAKE_CURRENT_SOURCE_DIR}/libs/${CMAKE_ANDROID_ARCH_ABI}/libdobby.a
)

target_link_libraries(
        native_hook PRIVATE
        DOBBY_LIB
        log
)
