//
// Created by Ankio on 2024/5/14.
//

#ifndef AUTOACCOUNTING_WEBSOCKETSERVER_H
#define AUTOACCOUNTING_WEBSOCKETSERVER_H
#include "../wsServer/include/ws.h"
#include <map>
class WebSocketServer {
public:
    explicit WebSocketServer(int port);
private:
    //client map
    static std::map<ws_cli_conn_t *, bool> clients;
    static void onOpen(ws_cli_conn_t *client);
    static void onClose(ws_cli_conn_t *client);
    static void onMessage(ws_cli_conn_t *client, const unsigned char *msg, uint64_t size, int type);
    //生成8位随机字符串
    static std::string generateRandomString(int count = 8);
    //初始化token
    static void initToken();
    static void publishToken();
    static std::string token;
};


#endif //AUTOACCOUNTING_WEBSOCKETSERVER_H
