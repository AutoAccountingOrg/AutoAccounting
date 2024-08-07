//
// Created by Ankio on 2024/7/15.
//

#include "AssetsMapHandler.h"
#include "AssetsMapModel.h"
#include "db/Database.h"

Json::Value AssetsMapHandler::handle(const std::string &function, Json::Value &data) {
    auto table = AssetsMapModel::getTable();
    if (function == "list") {
        int page = data["page"].asInt();
        int size = data["size"].asInt();
        return list(page, size);
    } else if (function == "clear") {
        Database::getInstance().executeSQL("delete from " + table.name);
    }  else if (function == "add") {
        Database::getInstance().insert(table, data);
    } else if (function == "del") {
        Database::getInstance().remove(table, data["id"].asInt());
    } else if (function == "update"){
        Database::getInstance().update(table, data, data["id"].asInt());
    }
    Json::Value result;
    result["status"] = 0;
    result["message"] = "success";
    return result;
}


Json::Value AssetsMapHandler::list(int page, int size) {
    return Database::getInstance().page(AssetsMapModel::getTable(), page, size, "", {});
}



