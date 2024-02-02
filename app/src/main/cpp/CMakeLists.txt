cmake_minimum_required(VERSION 3.22.1)
project("auto")

# Set C++ standard to C++11
set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED True)

add_executable(starter starter.cpp)

set_target_properties(starter PROPERTIES
        RUNTIME_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../assets/shell/${ANDROID_ABI}"
)