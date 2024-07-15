//
// Created by Ankio on 2024/7/15.
//

#include "BookNameHandler.h"
#include "BookNameModel.h"
#include "db/Database.h"

Json::Value BookNameHandler::handle(const std::string &function, Json::Value &data) {
    auto table = BookNameModel::getTable();
    if (function == "list") {
        int page = data["page"].asInt();
        int size = data["size"].asInt();
        return Database::getInstance().page(table, page, size, "", {});
    } else if (function == "clear") {
        Database::getInstance().executeSQL("delete from " + table.name);
    }  else if (function == "add") {
        Database::getInstance().insert(table, data);
    }
    Json::Value result;
    result["status"] = 0;
    result["message"] = "success";
    return result;
}


