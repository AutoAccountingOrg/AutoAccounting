#include "LoginHandler.h"
#include "ws/TokenManager.h"
#include "ws/VersionManager.h"

// login/login
Json::Value LoginHandler::handle(const std::string &function, Json::Value &data) {
    if(function == "login") {
        std::string token = data["token"].asString();
        std::string app = data["app"].asString();


        if(!VersionManager::checkVersion()){
            Json::Value ret;
            ret["status"] = 2;
            ret["msg"] = "version is too low";
            return ret;
        }

        if (TokenManager::checkToken(app, token)) {
            Json::Value ret;
            ret["status"] = 0;
            ret["msg"] = "login success";
            //这里返回登录状态但是不直接结束链接，客户端可以复用链接重新登录。
            return ret;
        }
    }
    Json::Value ret;
    ret["status"] = 1;
    ret["msg"] = "login failed";
    return ret;
}

