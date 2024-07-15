cmake_minimum_required(VERSION 3.22.1)
set(CMAKE_CXX_STANDARD 20)
project("auto_accounting_starter")

# Include directories
include_directories(wsServer/include)
include_directories(jsoncpp/include)
include_directories(db/models)
# WebSocket files
set(WEBSOCKET_FILES
        wsServer/src/ws.c
        wsServer/src/base64.c
        wsServer/src/sha1.c
        wsServer/src/handshake.c
        wsServer/src/utf8.c
)

# SQLite files
set(SQLITE_FILES
        sqlite/sqlite3.c
)

# JSON files
set(JSON_FILES
        jsoncpp/src/lib_json/json_reader.cpp
        jsoncpp/src/lib_json/json_valueiterator.inl
        jsoncpp/src/lib_json/json_value.cpp
        jsoncpp/src/lib_json/json_writer.cpp
)

# QuickJS
add_subdirectory(quickjspp/quickjs)

add_library(quickjspp INTERFACE)
target_link_libraries(quickjspp INTERFACE quickjs ${CMAKE_REQUIRED_LIBRARIES})
target_compile_features(quickjspp INTERFACE cxx_std_17)
target_include_directories(quickjspp INTERFACE .)
set_target_properties(quickjspp PROPERTIES PUBLIC_HEADER quickjspp/quickjspp.hpp)
file(GLOB_RECURSE DB_SOURCES "db/*.cpp")
file(GLOB_RECURSE WS_SOURCES "ws/*.cpp")
# Executable target
add_executable(${CMAKE_PROJECT_NAME}
        server.cpp
        common.cpp
        ${WEBSOCKET_FILES}
        ${SQLITE_FILES}
        ${JSON_FILES}
        ${DB_SOURCES}
        ${WS_SOURCES}
)

# Link libraries to the executable
target_link_libraries(${CMAKE_PROJECT_NAME} quickjspp)

# Set output directory
set_target_properties(${CMAKE_PROJECT_NAME} PROPERTIES
        RUNTIME_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../assets/shell/${ANDROID_ABI}"
)
