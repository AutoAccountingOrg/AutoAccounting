cmake_minimum_required(VERSION 3.22.1)
project("auto")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED True)


add_executable(auto_accounting_starter starter.cpp



        utils/trim.cpp

        file/File.cpp
        file/File.h
        js/quickjspp.hpp
        utils/ThreadLocalStorage.h

        http/handler/Handler.h
        http/handler/Handler.cpp
        http/server/Server.cpp
           http/server/Server.h



)

set_target_properties(auto_accounting_starter PROPERTIES
        RUNTIME_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../assets/shell/${ANDROID_ABI}"
)


add_subdirectory(js/quickjs)

add_library(quickjspp INTERFACE)
target_link_libraries(auto_accounting_starter PRIVATE quickjs ${CMAKE_REQUIRED_LIBRARIES})
target_compile_features(quickjspp INTERFACE cxx_std_17)
target_include_directories(quickjspp INTERFACE .)
set_target_properties(quickjspp PROPERTIES PUBLIC_HEADER js/quickjspp.hpp)
# 确保不包含调试信息在发布版本
if(NOT CMAKE_BUILD_TYPE MATCHES Debug)
    set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -s") # 添加 -s 来移除符号表
endif()
