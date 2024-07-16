//
// Created by Ankio on 2024/7/12.
//

#include "TokenManager.h"
#include "common.h"
#include <string>
#include <filesystem>
#include <random>
#include <fstream>
#include <iostream>
#include <algorithm>
#include "Logger.h"
#include "db/Database.h"
#include "AuthModel.h"

std::string TokenManager::generateRandomString(int count) {
    std::string str = "0123456789";
    std::random_device rd;
    std::mt19937 generator(rd());
    std::shuffle(str.begin(), str.end(), generator);
    return str.substr(0, count);
}

void TokenManager::initToken() {

    std::ifstream appsFile("apps.txt");
    if (appsFile.is_open()) {
        std::string app;
        Table authTable = AuthModel::getTable();
        while (std::getline(appsFile, app)) {
            trim(app);
            if (app.empty()) {
                continue;
            }
            std::string condition = "app = ? limit 1";
            std::vector<Json::Value> params = { app };
            Json::Value queryResult = Database::getInstance().selectConditional(authTable, condition,params);
            std::string token;
            if (queryResult.empty()) {
                Json::Value auth;
                auth["app"] = app;
                auth["token"] = generateRandomString(32);
                token = auth["token"].asString();
                Database::getInstance().insert(authTable, auth);
            }else{
                token = queryResult[0]["token"].asString();
            }

            publishToken(app,token);
        }
    }
}




void TokenManager::publishToken(const std::string &app,const std::string &token) {
    std::string appPath = "/sdcard/Android/data/" + app;
    if (std::filesystem::exists(appPath)) {
        std::string path = appPath + "/token.txt";
        std::ofstream appFile(path);
        if (!appFile.is_open()) {
            Logger::log("open token file error: " + path, LOG_LEVEL_ERROR);
        } else {
            Logger::log("write token to " + path, LOG_LEVEL_INFO);
            appFile << token;
            appFile.close();
            std::filesystem::permissions(path, std::filesystem::perms::all);
        }
    }
}



bool TokenManager::checkToken(const std::string &app, const std::string &token) {
    Table authTable = AuthModel::getTable();
    std::string condition = "app = ?  limit 1";
    std::vector<Json::Value> params = { app };
    Json::Value queryResult = Database::getInstance().selectConditional(authTable, condition, params);
    if (queryResult.empty()) {
        Logger::log("token check failed: " + app + " " + token, LOG_LEVEL_ERROR);
        return false;
    }else{
        std::string dbToken = queryResult[0]["token"].asString();
        if (dbToken != token) {
            Logger::log("token check failed: " + app + " " + token, LOG_LEVEL_ERROR);
            publishToken(app, dbToken);
            return false;
        }
    }
    return true;
}