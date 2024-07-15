//
// Created by Ankio on 2024/7/15.
//

#include "SettingHandler.h"
#include "db/Database.h"
#include "SettingsModel.h"

Json::Value SettingHandler::handle(const std::string &function, Json::Value &data) {
    auto table = SettingsModel::getTable();
    if (function == "get") {
        return get(data["app"].asString(), data["key"].asString());
    } else if (function == "del") {
        Database::getInstance().remove(table, data["id"].asInt());
    } else if (function == "set") {

        Json::Value res = get(data["app"].asString(), data["key"].asString());

        if (!res.empty()) {
            Database::getInstance().update(table, data, res["id"].asInt());
        } else {
            Database::getInstance().insert(table, data);
        }
    }
    Json::Value result;
    result["status"] = 0;
    result["message"] = "success";
    return result;
}

Json::Value SettingHandler::get(const std::string &app, const std::string &key) {
    auto table = SettingsModel::getTable();
    Json::Value result = Database::getInstance().selectConditional(table, "app=? and key=? ",
                                                                   {app, key});
    if (!result.empty()) {
        return result[0];
    }
    return {Json::ValueType::nullValue};
}