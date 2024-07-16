//
// Created by Ankio on 2024/5/14.
//

#include <cstdio>
#include <unistd.h>
#include "WebSocketServer.h"
#include "../jsoncpp/include/json/value.h"
#include "../jsoncpp/include/json/reader.h"
#include "../common.h"
#include "BaseHandler.h"
#include "TokenManager.h"
#include "RouteManager.h"
#include "VersionManager.h"
#include <random>
#include <sys/stat.h>
#include "Logger.h"


WebSocketServer::WebSocketServer(int port) {
    TokenManager::initToken();
    RouteManager::initRoute();
    VersionManager::initVersion();
    startServer(port);
}


void WebSocketServer::startServer(int port) {

    struct ws_server server{};
    server.host = "0.0.0.0";
    server.port = static_cast<uint16_t>(port);
    server.thread_loop = 0;
    server.timeout_ms = 1000;
    server.evs.onopen = &WebSocketServer::onOpen;
    server.evs.onclose = &WebSocketServer::onClose;
    server.evs.onmessage = &WebSocketServer::onMessage;

    ws_socket(&server);
}


/**
 * @brief This function is called whenever a new connection is opened.
 * @param client Client connection.
 */
void WebSocketServer::onOpen(ws_cli_conn_t *client) {

    Json::Value json;
    json["type"] = "auth";
    ws_sendframe_txt(client, json.toStyledString().c_str());
}

/**
 * @brief This function is called whenever a connection is closed.
 * @param client Client connection.
 */
void WebSocketServer::onClose(ws_cli_conn_t *client) {
    //从客户端列表删除
    clients.erase(client);
}

/**
 * @brief Message events goes here.
 * @param client Client connection.
 * @param msg    Message content.
 * @param size   Message size.
 * @param type   Message type.
 */
void WebSocketServer::onMessage(ws_cli_conn_t *client,
                                const unsigned char *msg, uint64_t size, int type) {





        Json::Value json;
        Json::Reader reader;
        if (!reader.parse((const char *) msg, json)) {
            Logger::log("json parse error: "+std::string((char *)msg), LOG_LEVEL_ERROR);
            return;
        }


        std::string message_id = json["id"].asString();
        std::string message_type = json["type"].asString();

    Json::Value ret;
    ret["type"] = message_type;
    ret["id"] = message_id;

    auto pos = message_type.find('/');
    if (pos == std::string::npos) {
        Logger::log("Invalid message type: " + message_type, LOG_LEVEL_ERROR);
        return;
    }

    std::string module = message_type.substr(0, pos);
    std::string function = message_type.substr(pos + 1);

    // login/login
    // 如果请求不是login，检查token
    if (module != "login" && clients.find(client) == clients.end()) {
        ret["data"] = "Unauthorized";
        ws_sendframe_txt(client, ret.toStyledString().c_str());
        ws_close_client(client);
        return;
    }


    auto it = RouteManager::getHandler(module);
    if (it != nullptr) {
        try {
            ret["data"] = it->handle(function, json["data"]);
        } catch (std::exception &e) {
            Logger::log(e.what(), LOG_LEVEL_ERROR);
            ret["data"] = e.what();
        }

    } else {
        auto invalidModule = "Invalid module " + std::string(module);
        ret["data"] = invalidModule;
       Logger::log(invalidModule, LOG_LEVEL_ERROR);
    }


    if(module == "login" && ret["data"]["status"].asInt() == 0){
        clients[client] = true;
    }

    ws_sendframe_txt(client, ret.toStyledString().c_str());


  //ws_sendframe_txt(client, "hello");


}



std::map<ws_cli_conn_t *, bool> WebSocketServer::clients{};



