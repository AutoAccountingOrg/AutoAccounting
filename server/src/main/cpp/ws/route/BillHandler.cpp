//
// Created by Ankio on 2024/7/15.
//

#include "BillHandler.h"
#include "db/Database.h"
#include "BillInfoModel.h"

Json::Value BillHandler::handle(const std::string &function, Json::Value &data) {
    auto table = BillInfoModel::getTable();
    if (function == "list") {
        int page = data["page"].asInt();
        int size = data["size"].asInt();
        return list(page, size);
    } else if (function == "clear") {
        Database::getInstance().executeSQL("delete from " + table.name);
    }  else if (function == "add") {
        return {Json::Value(add(data))};
    } else if (function == "update") {
        Database::getInstance().update(table, data, data["id"].asInt());
    } else if (function == "del") {
        Database::getInstance().remove(table, data["id"].asInt());
    } else if(function == "group"){
        //根据分组id查找child
        auto group = data["group"].asInt();
        return Database::getInstance().selectConditional(table,"groupId=?", {group});
    } else if(function == "sync/list"){
        //获取需要同步的账单
        return Database::getInstance().selectConditional(table,"sync=0 and groupId=0", {});
    } else if(function == "sync/status"){
        //更新同步状态
        Database::getInstance().executeSQL("update " + table.name + " set sync=? where id=?", {data["sync"].asInt(),data["id"].asInt()});
    }
    Json::Value result;
    result["status"] = 0;
    result["message"] = "success";
    return result;
}


Json::Value BillHandler::list(int page, int size) {
    // list列表的时候，是列全局的表只允许没有分组的账单
    return Database::getInstance().page(BillInfoModel::getTable(), page, size, "groupId=0", {},"time desc");
}

int BillHandler::add(const Json::Value &data) {
    auto table = BillInfoModel::getTable();
    int id = Database::getInstance().insert(table, data);
    return id;
}