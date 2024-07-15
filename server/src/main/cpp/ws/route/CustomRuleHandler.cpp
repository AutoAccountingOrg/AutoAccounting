//
// Created by Ankio on 2024/7/15.
//

#include "CustomRuleHandler.h"
#include "CustomRuleModel.h"
#include "db/Database.h"

Json::Value CustomRuleHandler::handle(const std::string &function, Json::Value &data) {
    auto table = CustomRuleModel::getTable();
    if (function == "list") {
        int page = data["page"].asInt();
        int size = data["size"].asInt();
        return Database::getInstance().page(table, page, size, "book=?", {data["book"].asInt()});
    } else if (function == "clear") {
        Database::getInstance().executeSQL("delete from " + table.name);
    }  else if (function == "add") {
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