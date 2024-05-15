
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



add_executable(
        auto_${CMAKE_PROJECT_NAME}
        server.cpp
        ${WEBSOCKET_FILES}
        ${SQLITE_FILES}
        ${JSON_FILES}
        db/DbManager.cpp
)