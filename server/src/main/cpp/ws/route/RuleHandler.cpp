//
// Created by Ankio on 2024/7/15.
//

#include "RuleHandler.h"
#include "RuleModel.h"
#include "db/Database.h"

Json::Value RuleHandler::handle(const std::string &function, Json::Value &data) {
    auto table = RuleModel::getTable();
    if (function == "list") {
        int page = data["page"].asInt();
        int size = data["size"].asInt();
        return Database::getInstance().page(table, page, size, "", {});
    } else if (function == "clear") {
        Database::getInstance().executeSQL("delete from " + table.name);
    } else if (function == "get") {
        return get(data["name"].asString());
    } else if (function == "add") {
        Database::getInstance().insert(table, data);
    } else if(function == "update"){
        Database::getInstance().update(table, data,data["id"].asInt());
    } else if(function == "del"){
        Database::getInstance().remove(table,data["id"].asInt());
    }
    Json::Value result;
    result["status"] = 0;
    result["message"] = "success";
    return result;
}


Json::Value RuleHandler::get(const std::string &name) {
    auto table = RuleModel::getTable();
    Json::Value result = Database::getInstance().selectConditional(table, "name=? ",
                                                                   {name});
    if (!result.empty()) {
        return result[0];
    }
    return {Json::ValueType::nullValue};
}