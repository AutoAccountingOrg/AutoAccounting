//
// Created by Ankio on 2024/5/14.
//

#ifndef AUTOACCOUNTING_WEBSOCKETSERVER_H
#define AUTOACCOUNTING_WEBSOCKETSERVER_H




#include "../wsServer/include/ws.h"
#include "../quickjspp/quickjspp.hpp"
#include "json/value.h"
#include <map>
#include <thread>
class WebSocketServer {
public:
    explicit WebSocketServer(int port);

private:
    static void startServer(int port);
    //client map
    static std::map<ws_cli_conn_t *, bool> clients;
    static void onOpen(ws_cli_conn_t *client);
    static void onClose(ws_cli_conn_t *client);
    static void onMessage(ws_cli_conn_t *client, const unsigned char *msg, uint64_t size, int type);
    struct ws_server wsServer{};


};


#endif //AUTOACCOUNTING_WEBSOCKETSERVER_H
