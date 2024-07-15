//
// Created by Ankio on 2024/7/15.
//

#include "AssetHandler.h"
#include "db/Database.h"
#include "AssetsModel.h"

Json::Value AssetHandler::handle(const std::string &function, Json::Value &data) {
    auto table = AssetsModel::getTable();
    if (function == "list") {
        int page = data["page"].asInt();
        int size = data["size"].asInt();//9999
        return list(page, size);
    } else if (function == "clear") {
        Database::getInstance().executeSQL("delete from " + table.name);
    }  else if (function == "add") {
        Database::getInstance().insert(table, data);
    }
    return BaseHandler::handle(function, data);
}


Json::Value AssetHandler::list(int page, int size) {
    return Database::getInstance().page(AssetsModel::getTable(), page, size, "", {});
}