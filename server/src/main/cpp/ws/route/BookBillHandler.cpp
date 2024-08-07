//
// Created by Ankio on 2024/7/15.
//

#include "BookBillHandler.h"
#include "db/Database.h"
#include "BookBillModel.h"

Json::Value BookBillHandler::handle(const std::string &function, Json::Value &data) {
    auto table = BookBillModel::getTable();
    if (function == "list") {
        int type = data["type"].asInt();
        int page = data["page"].asInt();
        int size = data["size"].asInt();
        return Database::getInstance().page(table, page, size, "book=? and type=?", {data["book"].asInt(),type});
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