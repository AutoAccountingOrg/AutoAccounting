//
// Created by Ankio on 2024/5/14.
//

#include <cstdio>
#include <unistd.h>
#include "WebSocketServer.h"
#include "../jsoncpp/include/json/value.h"
#include "../jsoncpp/include/json/reader.h"
#include "../db/DbManager.h"
#include <random>

WebSocketServer::WebSocketServer(int port) {
    initToken();
    struct ws_server wsServer{};
    wsServer.host = "0.0.0.0";
    wsServer.port = static_cast<uint16_t>(port);
    wsServer.thread_loop = 0;
    wsServer.timeout_ms = 1000;
    wsServer.evs.onopen = &WebSocketServer::onOpen;
    wsServer.evs.onclose = &WebSocketServer::onClose;
    wsServer.evs.onmessage = &WebSocketServer::onMessage;
    ws_socket(&wsServer);
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

    try {
        Json::Value json;
        Json::Reader reader;
        if (!reader.parse((const char *) msg, json)) {
            printf("json parse error\n");
            return;
        }


        std::string message_id = json["id"].asString();
        std::string message_type = json["type"].asString();

        printf("message_type: %s\n", message_type.c_str());


        Json::Value ret;
        if (message_type == "auth") {
            if (json["data"].asString() != token) {
                printf("token error\n");
                publishToken();
                ws_close_client(client);
                return;
            }
            clients[client] = true;
            ret["type"] = "auth";
            ret["id"] = message_id;
            ret["data"] = "OK";
            ws_sendframe_txt(client, ret.toStyledString().c_str());
            return;
        }

        if (clients.find(client) == clients.end()) {
            printf("client not auth\n");
            ws_close_client(client);
            return;
        }


        ret["data"] = "OK";
        //对于不同的路由进行处理
        Json::Value data = json["data"];


        if (message_type == "log/put") {

            DbManager::getInstance().insertLog(data["date"].asString(), data["app"].asString(),
                                               data["hook"].asInt(), data["thread"].asString(),
                                               data["line"].asString(), data["log"].asString());

        } else if (message_type == "log/get") {
            ret["data"] = DbManager::getInstance().getLog(data["limit"].asInt());
        }



        else if (message_type == "setting/set") {
            std::string app = data["app"].asString();
            std::string key = data["key"].asString();
            std::string value = data["value"].asString();
            DbManager::getInstance().setSetting(app, key, value);
        } else if (message_type == "setting/get") {
            std::string app = data["app"].asString();
            std::string key = data["key"].asString();
            ret["data"] = DbManager::getInstance().getSetting(app, key);
        }



        else if(message_type == "bill/put"){

            int id = data["id"].asInt();
            int _type = data["type"].asInt();
            int currency = data["currency"].asInt();
            int money = data["money"].asInt();
            int fee = data["fee"].asInt();
            int timeStamp = data["timeStamp"].asInt();
            std::string shopName = data["shopName"].asString();
            std::string cateName = data["cateName"].asString();
            std::string extendData = data["extendData"].asString();
            std::string bookName = data["bookName"].asString();
            std::string accountNameFrom = data["accountNameFrom"].asString();
            std::string accountNameTo = data["accountNameTo"].asString();
            std::string fromApp = data["fromApp"].asString();
            int groupId = data["groupId"].asInt();
            std::string channel = data["channel"].asString();
            int syncFromApp = data["syncFromApp"].asInt();
            std::string remark = data["remark"].asString();
            int fromType = data["fromType"].asInt();

            ret["data"]=DbManager::getInstance().insertBill(id, _type, currency, money, fee, timeStamp, shopName, cateName, extendData, bookName, accountNameFrom, accountNameTo, fromApp, groupId, channel, syncFromApp, remark, fromType);
        }else if(message_type == "bill/sync/list"){
            ret["data"]=DbManager::getInstance().getWaitSyncBills();
            //要求账单app每次同步完后都要发送一个消息给服务器，服务器更新状态
        } else if(message_type == "bill/sync/update"){
            int id = data["id"].asInt();
            int status = data["status"].asInt();
            DbManager::getInstance().updateBillSyncStatus(id, status);
        } else if(message_type == "bill/list/group"){
            ret["data"]=DbManager::getInstance().getBillListGroup(data["limit"].asInt());
        } else if(message_type == "bill/list/id"){
            ret["data"]=DbManager::getInstance().getBillByIds(data["ids"].asString());
        } else if(message_type == "bill/list/child"){
            ret["data"]=DbManager::getInstance().getBillByGroupId(data["groupId"].asInt());
        }





        else {
            ret["data"] = "error";
        }


        ret["type"] = message_type;
        ret["id"] = message_id;
        ws_sendframe_txt(client, ret.toStyledString().c_str());
    } catch (std::exception &e) {
        printf("error: %s\n", e.what());
    }


    ws_sendframe_txt(client, "hello");


}

std::string WebSocketServer::generateRandomString(int count) {
    std::string str = "0123456789abcdefghijklmnopqrstuvwxyz";
    std::random_device rd;
    std::mt19937 generator(rd());
    std::shuffle(str.begin(), str.end(), generator);
    return str.substr(0, count);
}

void WebSocketServer::initToken() {
    FILE *file = fopen("token.txt", "r");
    if (file == nullptr) {
        file = fopen("token.txt", "w");
        token = generateRandomString(32);
        fprintf(file, "%s", token.c_str());
    } else {
        char buf[1024];
        fgets(buf, 1024, file);
        token = buf;
    }
    fclose(file);

    publishToken();


}

void WebSocketServer::publishToken() {
    //检查是否存在apps.txt，如果有就逐行读取
    FILE *appsFile = fopen("apps.txt", "r");
    if (appsFile != nullptr) {
        char buf[1024];
        while (fgets(buf, 1024, appsFile) != nullptr) {
            //读取包名拼接目录，将token写入目录
            std::string path = std::string("/sdcard/Android/data/") + buf + "/token.txt";
            FILE *appFile = fopen(path.c_str(), "w");
            fprintf(appFile, "%s", token.c_str());
            fclose(appFile);
        }
        fclose(appsFile);
    }
}

std::map<ws_cli_conn_t *, bool> WebSocketServer::clients{};
std::string WebSocketServer::token;
