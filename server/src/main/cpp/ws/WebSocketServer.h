//
// Created by Ankio on 2024/5/14.
//

#ifndef AUTOACCOUNTING_WEBSOCKETSERVER_H
#define AUTOACCOUNTING_WEBSOCKETSERVER_H

#define LOG_LEVEL_DEBUG 0
#define LOG_LEVEL_INFO 1
#define LOG_LEVEL_WARN 2
#define LOG_LEVEL_ERROR 3


#include "../wsServer/include/ws.h"
#include "../quickjspp/quickjspp.hpp"
#include "json/value.h"
#include <map>
#include <thread>
class WebSocketServer {
public:
    explicit WebSocketServer(int port);
    static void log(const std::string &msg,int level);

private:
    static void startServer(int port);
    //client map
    static std::map<ws_cli_conn_t *, bool> clients;
    static void onOpen(ws_cli_conn_t *client);
    static void onClose(ws_cli_conn_t *client);
    static void onMessage(ws_cli_conn_t *client, const unsigned char *msg, uint64_t size, int type);
    static std::string runJs(const std::string &js);
    static std::string token;
    static std::map<std::thread::id, std::string> resultMap;
    static std::mutex resultMapMutex;
    static void print(qjs::rest<std::string> args);
    struct ws_server wsServer{};
    static std::string version;
    static std::string getVersion();

};


#endif //AUTOACCOUNTING_WEBSOCKETSERVER_H
