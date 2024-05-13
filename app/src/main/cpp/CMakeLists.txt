cmake_minimum_required(VERSION 3.22.1)
project("auto")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED True)


add_executable(auto_accounting_starter

        starter.cpp
        starter.h

        File.cpp
        File.h

        quickjspp/quickjspp.hpp

        Handler.h
        Handler.cpp

        Server.cpp
        Server.h

        misc.cpp
        misc.h


)

set_target_properties(auto_accounting_starter PROPERTIES
        RUNTIME_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../assets/shell/${ANDROID_ABI}"
)



# 确保不包含调试信息在发布版本
if(NOT CMAKE_BUILD_TYPE MATCHES Debug)
    set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -s") # 添加 -s 来移除符号表
endif()
