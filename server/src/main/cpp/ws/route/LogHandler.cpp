//
// Created by Ankio on 2024/7/15.
//

#include "LogHandler.h"
#include "db/Database.h"
#include "LogModel.h"

Json::Value LogHandler::handle(const std::string &function, Json::Value &data) {
    auto table = LogModel::getTable();
    if (function == "list") {
        int page = data["page"].asInt();
        int size = data["size"].asInt();
        return list(page, size);
    } else if (function == "clear") {
        Database::getInstance().executeSQL("delete from " + table.name);
    }  else if (function == "add") {
        Database::getInstance().insert(table, data);
    }
    return BaseHandler::handle(function, data);
}

Json::Value LogHandler::list(int page, int size) {

    return Database::getInstance().page(LogModel::getTable(), page, size, "", {});
}