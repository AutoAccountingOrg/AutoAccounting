//
// Created by Ankio on 2024/5/14.
//

#include <cstdio>
#include <unistd.h>
#include "WebSocketServer.h"
#include "../jsoncpp/include/json/value.h"
#include "../jsoncpp/include/json/reader.h"
#include "../db/DbManager.h"
#include "../common.h"
#include "../base64/include/base64.hpp"
#include <random>
#include <sys/stat.h>
std::string WebSocketServer::version;
WebSocketServer::WebSocketServer(int port) {
    initToken();
    version = getVersion();
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

std::string WebSocketServer::getVersion() {
    FILE *file = fopen("version.txt", "r");
    if (file == nullptr) {
        file = fopen("version.txt", "w");
        fprintf(file, "%s", "1.0.0");
        return "1.0.0";
    } else {
        char buf[1024];
        fgets(buf, 1024, file);
        return buf;
    }
}

/**
 * @brief This function is called whenever a new connection is opened.
 * @param client Client connection.
 */
void WebSocketServer::onOpen(ws_cli_conn_t *client) {
    Json::Value json;
    json["type"] = "auth";
    json["version"] = version;
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
            log("json parse error",LOG_LEVEL_ERROR);
            return;
        }


        std::string message_id = json["id"].asString();
        std::string message_type = json["type"].asString();


        if(json["type"].asString()!="log/put"){
            log("message: " + message_type,LOG_LEVEL_DEBUG);
            log("recived: " + json.toStyledString(),LOG_LEVEL_DEBUG);
        }


        Json::Value ret;
        if (message_type == "auth") {

            std::string localVersion = getVersion();
            if(localVersion!=version){
                log("server need update ( "+version+" => "+localVersion+" )",LOG_LEVEL_WARN);
                ws_close_client(client);
                exit(65);//直接退出进程
                return;
            }

            if (json["data"].asString() != token) {
                log("token error " + json["data"].asString() +" , now token is "+token,LOG_LEVEL_ERROR);
                publishToken();
                ws_close_client(client);
                return;
            }
            clients[client] = true;
            ret["type"] = "auth/success";
            ret["id"] = message_id;
            ret["data"] = version;
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
                                               data["line"].asString(), data["log"].asString(),data["level"].asInt());

        } else if (message_type == "log/get") {
            ret["data"] = DbManager::getInstance().getLog(data["limit"].asInt());
        }else if (message_type == "log/delete/all") {
            DbManager::getInstance().deleteAllLog();
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
            std::string currency = data["currency"].asString();
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
        }else if(message_type == "bill/list/parent"){
        ret["data"]=DbManager::getInstance().getBillAllParents();
    }


        else if(message_type == "data/put"){
            int id = data["id"].asInt();
            std::string _data = data["data"].asString();
            int _type = data["type"].asInt();
            std::string source = data["source"].asString();
            int time = data["time"].asInt();
            int match = data["match"].asInt();
            int issue = data["issue"].asInt();
            std::string rule = data["rule"].asString();
            DbManager::getInstance().insertAppData(id, _data, _type, source,rule, time, match, issue);
        } else if(message_type == "data/get"){
            ret["data"]=DbManager::getInstance().getAppData(data["limit"].asInt());
        }


        else if(message_type == "asset/put"){
            int id = data["id"].asInt();
            std::string name = data["name"].asString();
            int _type = data["type"].asInt();
            int sort = data["sort"].asInt();
            std::string icon = data["icon"].asString();
            std::string extra = data["extra"].asString();
            DbManager::getInstance().insertAsset(id, name, _type, sort, icon, extra);
        } else if(message_type == "asset/get"){
            ret["data"]=DbManager::getInstance().getAsset(data["limit"].asInt());
        } else if(message_type == "asset/get/name"){
            ret["data"]=DbManager::getInstance().getAssetByName(data["name"].asString());
        } else if(message_type == "asset/remove"){
            std::string name = data["name"].asString();
            DbManager::getInstance().removeAsset(name);
        }


        else if(message_type == "asset/map/put"){
            int id = data["id"].asInt();
            std::string name = data["name"].asString();
            std::string mapName = data["mapName"].asString();
            int regex = data["regex"].asInt();
            DbManager::getInstance().insertAssetMap(id,name, mapName, regex);
        } else if(message_type == "asset/map/get"){
            ret["data"]=DbManager::getInstance().getAssetMap();
        } else if(message_type == "asset/map/remove"){
            int id = data["id"].asInt();
            DbManager::getInstance().removeAssetMap(id);
        }


        else if(message_type == "book/put"){
            int id = data["id"].asInt();
            std::string name = data["name"].asString();
            std::string icon = data["icon"].asString();
            DbManager::getInstance().insertBookName(id, name, icon);
        } else if(message_type == "book/get/one"){
            ret["data"] = DbManager::getInstance().getOneBookName();
        } else if(message_type == "book/get/name"){
            ret["data"] = DbManager::getInstance().getBookName(data["name"].asString());
        } else if(message_type == "book/get/all"){
            ret["data"] = DbManager::getInstance().getBookName();
        } else if(message_type == "book/remove"){
            std::string name = data["name"].asString();
            DbManager::getInstance().removeBookName(name);
        }

        else if(message_type == "book/sync"){
            DbManager::getInstance().syncBook(data["data"],data["md5"].asString());
        }
        else if(message_type == "assets/sync"){
            DbManager::getInstance().syncAssets(data["data"],data["md5"].asString());
        }else if(message_type == "app/bill/add"){
            DbManager::getInstance().addBxBills(data["bills"],data["md5"].asString());
        }else if(message_type == "app/bill/get"){
             int limit = data["limit"].asInt();
             int t = data["type"].asInt();
            ret["data"] = DbManager::getInstance().getBxBills(limit,t);
         }

        else if(message_type == "cate/put"){
            int id = data["id"].asInt();
            std::string name = data["name"].asString();
            std::string icon = data["icon"].asString();
            std::string remoteId = data["remoteId"].asString();
            int parent = data["parent"].asInt();
            int book = data["book"].asInt();
            int sort = data["sort"].asInt();
            int _type = data["type"].asInt();
            DbManager::getInstance().insertCate(id, name, icon, remoteId, parent, book, sort, _type);
        } else if(message_type == "cate/get/all"){
            int book = data["book"].asInt();
            int _type = data["type"].asInt();
            int parent = data["parent"].asInt();
            ret["data"] = DbManager::getInstance().getAllCate(book, _type, parent);
        } else if(message_type == "cate/get/name"){
            int book = data["book"].asInt();
            int _type = data["type"].asInt();
            std::string cateName = data["cateName"].asString();
            ret["data"] = DbManager::getInstance().getCate(book, cateName,_type);
        } else if(message_type == "cate/get/remote"){
            int book = data["book"].asInt();
            std::string remoteId = data["remoteId"].asString();
            ret["data"] = DbManager::getInstance().getCateByRemote(book, remoteId);
        } else if(message_type == "cate/remove"){
            int id = data["id"].asInt();
            DbManager::getInstance().removeAssetMap(id);
        }

        else if(message_type == "rule/custom/put"){
            int id = data["id"].asInt();
            std::string js = data["js"].asString();
            std::string text = data["text"].asString();
            std::string element = data["element"].asString();
            int use = data["use"].asInt();
            int sort = data["sort"].asInt();
            int _auto = data["auto"].asInt();
            DbManager::getInstance().insertCustomRule(id, js, text, element, use, sort, _auto);
        } else if(message_type == "rule/custom/get"){
            ret["data"] = DbManager::getInstance().loadCustomRules(data["limit"].asInt());
        } else if(message_type == "rule/custom/remove"){
            int id = data["id"].asInt();
            DbManager::getInstance().removeCustomRule(id);
        } else if(message_type == "rule/custom/get/id"){
            int id = data["id"].asInt();
            ret["data"] = DbManager::getInstance().getCustomRule(id);
        }


        else if(message_type == "rule/put"){
            std::string app = data["app"].asString();
            std::string js = data["js"].asString();
            std::string version = data["version"].asString();
            int _type = data["type"].asInt();
            DbManager::getInstance().insertRule(app, js, version, _type);
        } else if(message_type == "rule/get"){
            std::string app = data["app"].asString();
            int _type = data["type"].asInt();
            ret["data"] = DbManager::getInstance().getRule(app, _type);
        } else if (message_type == "rule/setting/get") {
            int limit = data["limit"].asInt();
            ret["data"] = DbManager::getInstance().getRule(limit);
        } else if (message_type == "rule/setting/put") {
            int id = data["id"].asInt();
            int autoAccounting = data["autoAccounting"].asInt();
            int enable = data["enable"].asInt();
            DbManager::getInstance().ruleSetting(id, autoAccounting, enable);
        } else if (message_type == "rule/remove") {
            int id = data["id"].asInt();
            DbManager::getInstance().removeRule(id);
        }


        else if(message_type == "analyze"){
            std::string _data = data["data"].asString();
            std::string app = data["app"].asString();
            int _type = data["type"].asInt();
            int call = data["call"].asInt();
            int time = std::time(nullptr);
            if (call == 1) {
                // 先存data
                DbManager::getInstance().insertAppData(0, _data, _type, app, "", time, 0, 0);
            }

            //Json::Value rule = DbManager::getInstance().getRule(app, _type);
            std::string rule = DbManager::getInstance().getSetting("server", "rule_js");
            //先执行分析账单内容
            std::string billJs = "var window = {data:JSON.stringify(" + _data + ")};" + rule;

            std::string result = runJs(billJs);

            //获取当前时间戳


            Json::Value _json;
            Json::Reader _reader;
            if (!_reader.parse((const char *) msg, _json)) {
                printf("json parse error\n");
                ret["data"] = "json parse error";
            }else{
                float money = _json["money"].asFloat();
                int bill_type = _json["type"].asInt();
                std::string shopName = replaceSubstring( _json["shopName"].asString(),"'","\"");
                std::string shopItem = replaceSubstring( _json["shopItem"].asString(),"'","\"");
                std::time_t now = std::time(nullptr);
                //time时间戳格式化为：HH:mm
                char buffer[32];
                std::tm *ptm = std::localtime(&now);
                std::strftime(buffer, 32, "%H:%M", ptm);
                std::string timeStr = buffer;

                std::string channel = _json["channel"].asString();

                //自动重新更新，不需要App调用更新

                DbManager::getInstance().insertAppData(0, _data, _type, app, channel, time, 1, 0);

                //分析分类内容
                std::pair<bool, bool> pair = DbManager::getInstance().checkRule(app, _type,
                                                                                channel);

                if (!pair.first && call == 1) {
                    //不启用这个规则
                    ret["data"] = "rule not enable";
                }else{
                    std::string customJs = DbManager::getInstance().getSetting("server",
                                                                               "custom_js");
                    std::string official_cate_js = DbManager::getInstance().getSetting("server",
                                                                                       "official_cate_js");
                    std::string categoryJs =
                            "var window = {money:" + std::to_string(money) + ", type:" +
                            std::to_string(bill_type) + ", shopName:'" + shopName +
                            "', shopItem:'" + shopItem + "', time:'" + timeStr + "'};\n" +
                            "function getCategory(money,type,shopName,shopItem,time){ " + customJs +
                            " return null};\n" +
                            "var categoryInfo = getCategory(window.money,window.type,window.shopName,window.shopItem,window.time);" +
                            "if(categoryInfo !== null) { print(JSON.stringify(categoryInfo));  } else { " +
                            official_cate_js + " }";


                    std::string categoryResult = runJs(categoryJs);
                    Json::Value categoryJson;
                    if (!_reader.parse(categoryResult, categoryJson)) {
                        printf("json parse error\n");
                        ret["data"] = "json parse error";
                    } else {
                        std::string bookName = categoryJson["bookName"].asString();
                        std::string cateName = categoryJson["cateName"].asString();

                        _json["bookName"] = bookName;
                        _json["cateName"] = cateName;




                        //拉起自动记账app
                        if (call == 1) {
                            try {
                                std::string cmd =
                                        R"(am start -a "net.ankio.auto.ACTION_SHOW_FLOATING_WINDOW" -d "autoaccounting://bill?data=)" +
                                        base64::to_base64(_json.toStyledString()) + R"(" --ei ")" +
                                        (pair.second ? "1" : "0") + //判断是否自动记录
                                        R"(" --ez "android.intent.extra.NO_ANIMATION" true -f 0x10000000)";
                                //写日志
                                log("执行命令" + cmd, LOG_LEVEL_INFO);
                                system(cmd.c_str());
                            } catch (const std::exception &e) {
                                log("拉起自动记账失败：" + std::string(e.what()), LOG_LEVEL_ERROR);
                            }

                        }

                        ret["data"] = _json;
                    }
                }



            }




        }


        else {
            ret["data"] = "error";
        }


        ret["type"] = message_type;
        ret["id"] = message_id;
        ws_sendframe_txt(client, ret.toStyledString().c_str());
    } catch (std::exception &e) {
        log("error: " + std::string(e.what()), LOG_LEVEL_ERROR);
    }


  //ws_sendframe_txt(client, "hello");


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

    trim(token);

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
            std::string app = std::string(buf);
            trim(app);
            std::string appPath = std::string("/sdcard/Android/data/") + app;
            if (std::filesystem::exists(appPath)) {
                std::string path = appPath + "/token.txt";
                FILE *appFile = fopen(path.c_str(), "w");
                // 检查文件指针是否为空
                if (appFile == nullptr) {
                    log("open token file error: "+path,LOG_LEVEL_ERROR);
                } else {
                    log("write token to " + path,LOG_LEVEL_INFO);
                    fprintf(appFile, "%s", token.c_str());
                    fclose(appFile);
                    chmod(path.c_str(), 0777);
                }
            }
        }
        fclose(appsFile);
    }
}

std::map<ws_cli_conn_t *, bool> WebSocketServer::clients{};
std::string WebSocketServer::token;

void WebSocketServer::print(qjs::rest<std::string> args) {
    std::lock_guard<std::mutex> lock(resultMapMutex);
    resultMap[std::thread::id()] = args[0];
}

void WebSocketServer::log(const std::string &msg,int level ){
    std::time_t now = std::time(nullptr);
    std::tm *ptm = std::localtime(&now);
    char buffer[32];
    // 格式化日期和时间：YYYY-MM-DD HH:MM:SS
    std::strftime(buffer, 32, "%Y-%m-%d %H:%M:%S", ptm);
    //获取当前时间
    std::string  date = {buffer};
    //获取堆栈信息
    DbManager::getInstance().insertLog(date, "server", 0, "main", "server", msg,level);
    std::string level_str = "";
    switch (level) {
        case LOG_LEVEL_INFO:
            level_str = "INFO";
            break;
        case LOG_LEVEL_WARN:
            level_str = "WARN";
            break;
        case LOG_LEVEL_ERROR:
            level_str = "ERROR";
            break;
        case LOG_LEVEL_DEBUG:
            level_str = "DEBUG";
            break;
        default:
            level_str = "INFO";
            break;
    }
    printf("[ %s ] [ %s ] %s\n", buffer,level_str.c_str(), msg.c_str());
}


std::string WebSocketServer::runJs(const std::string &js) {
    log("执行JS脚本",LOG_LEVEL_INFO);
    log(js,LOG_LEVEL_DEBUG);
    qjs::Runtime runtime;
    qjs::Context context(runtime);
    std::thread::id id = std::this_thread::get_id();
    try {
        auto &module = context.addModule("MyModule");
        module.function<&WebSocketServer::print>("print");
        context.eval(R"xxx(
             import { print } from 'MyModule';
            globalThis.print = print;
        )xxx", "<import>", JS_EVAL_TYPE_MODULE);

        context.eval(js);
        std::lock_guard<std::mutex> lock(resultMapMutex);
        std::string data = resultMap[id];
        resultMap.erase(id);
        return data;
    }
    catch (qjs::exception &e) {
        auto exc = context.getException();
        log("JS Error: " + (std::string) exc,LOG_LEVEL_WARN);
        if ((bool) exc["stack"])
            log("JS Error: " + (std::string) exc["stack"],LOG_LEVEL_WARN);
    }
    return "";
}
std::map<std::thread::id, std::string> WebSocketServer::resultMap;
std::mutex WebSocketServer::resultMapMutex;