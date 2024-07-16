//
// Created by Ankio on 2024/7/13.
//

#include "AppDataHandler.h"
#include "db/Database.h"
#include "db/models/AppDataModel.h"

Json::Value AppDataHandler::handle(const std::string &function, Json::Value &data) {
    auto table = AppDataModel::getTable();
    if (function == "list") {
        return list(data["page"].asInt(), data["size"].asInt(), data["data"].asString(),
                    data["match"].asInt());
    } else if(function == "update"){
        Database::getInstance().update(table, data,data["id"].asInt());
    } else if(function == "del"){
        Database::getInstance().remove(table,data["id"].asInt());
    } else if(function == "add"){
        add(data);
            } else if(function == "clear"){
        Database::getInstance().executeSQL("delete from " + table.name);
    }
    Json::Value result;
    result["status"] = 0;
    result["message"] = "success";
    return result;
}

 int AppDataHandler::add(Json::Value &data){
     auto table = AppDataModel::getTable();
    int id = Database::getInstance().insert(table,data);
    //只保留最新的500条数据
    Database::getInstance().executeSQL("delete from " + table.name + " where id not in (select id from " + table.name + " order by id desc limit 500)");

    return id;
}

Json::Value AppDataHandler::list(int page, int size, const std::string& data, int match) {
    std::string condition = " 1=1  ";
    if (match == 0) {
        condition += " and match=0";
    } else if (match == 1) {
        condition += " and match=1";
    }
    if (!data.empty()) {
        condition += " and data like '%" + data + "%'";
    }


    return Database::getInstance().page(AppDataModel::getTable(), page, size, condition, {});
}