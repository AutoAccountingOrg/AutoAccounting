
cmake_minimum_required(VERSION 3.22.1)

project("server")

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

set(JS_FILES
        quickjspp/qjs.cpp
        quickjspp/quickjspp.hpp
        quickjspp/quickjs/quickjs.c
        quickjspp/quickjs/libbf.c
        quickjspp/quickjs/libunicode.c
        quickjspp/quickjs/libregexp.c
        quickjspp/quickjs/cutils.c
        quickjspp/quickjs/quickjs-libc.c
)

set(BASE64
    base64/include/base64.hpp
)

add_executable(
        auto_${CMAKE_PROJECT_NAME}
        server.cpp
        common.cpp
        ${WEBSOCKET_FILES}
        ${SQLITE_FILES}
        ${JSON_FILES}
        db/DbManager.cpp
        ${JS_FILES}
        ${BASE64}

)