
cmake_minimum_required(VERSION 3.22.1)

project("auto_accounting_starter")

set(WEBSOCKET_FILES
        wsServer/src/ws.c
        wsServer/src/base64.c
        wsServer/src/sha1.c
        wsServer/src/handshake.c
        wsServer/src/utf8.c
        ws/WebSocketServer.cpp
)

include_directories(wsServer/include)

set(SQLITE_FILES
        sqlite/sqlite3.c
)

set(JSON_FILES
        jsoncpp/src/lib_json/json_reader.cpp
        jsoncpp/src/lib_json/json_valueiterator.inl
        jsoncpp/src/lib_json/json_value.cpp
        jsoncpp/src/lib_json/json_writer.cpp
)
include_directories(jsoncpp/include)


set(BASE64
    base64/include/base64.hpp
)

add_subdirectory(quickjspp/quickjs)

add_library(quickjspp INTERFACE)
target_link_libraries(quickjspp INTERFACE quickjs ${CMAKE_REQUIRED_LIBRARIES})
target_compile_features(quickjspp INTERFACE cxx_std_17)
target_include_directories(quickjspp INTERFACE .)
set_target_properties(quickjspp PROPERTIES PUBLIC_HEADER quickjspp/quickjspp.hpp)

add_executable(
        ${CMAKE_PROJECT_NAME}
        server.cpp
        common.cpp
        ${WEBSOCKET_FILES}
        ${SQLITE_FILES}
        ${JSON_FILES}
        db/DbManager.cpp
        ${BASE64}

)

target_link_libraries(${CMAKE_PROJECT_NAME} quickjspp)

set_target_properties(${CMAKE_PROJECT_NAME} PROPERTIES
        RUNTIME_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/../assets/shell/${ANDROID_ABI}"
)
