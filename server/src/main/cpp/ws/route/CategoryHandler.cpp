//
// Created by Ankio on 2024/7/15.
//

#include "CategoryHandler.h"
#include "CategoryModel.h"
#include "db/Database.h"
Json::Value CategoryHandler::handle(const std::string &function, Json::Value &data) {
    auto table = CategoryModel::getTable();
    if (function == "list") {
        int page = data["page"].asInt();
        int size = data["size"].asInt();
        return Database::getInstance().page(table, page, size, "book=? and type=? and parent=?", {data["book"].asInt(),data["type"].asInt(),data["parent"].asInt()});
    } else if (function == "clear") {
        Database::getInstance().executeSQL("delete from " + table.name);
    }  else if (function == "add") {
        Database::getInstance().insert(table, data);
    } else if (function == "get"){
        return Database::getInstance().selectConditional(table, "name=? and book=? and type=?", {data["id"],data["book"].asInt(),data["type"].asInt()});
    }
    Json::Value result;
    result["status"] = 0;
    result["message"] = "success";
    return result;
}