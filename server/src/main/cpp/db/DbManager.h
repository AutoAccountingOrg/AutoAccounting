//
// Created by Ankio on 2024/5/14.
//

#ifndef AUTOACCOUNTING_DBMANAGER_H
#define AUTOACCOUNTING_DBMANAGER_H


#include "../sqlite/sqlite3.h"
#include "../jsoncpp/include/json/value.h"
#include <string>
class DbManager {
private:
    sqlite3 *db{};

    DbManager();


    void initTable();

public:
    static DbManager &getInstance() {
        static DbManager instance;
        return instance;
    }

    DbManager(DbManager const &) = delete;

    void operator=(DbManager const &) = delete;
    ~DbManager();

    sqlite3_stmt * getStmt(const std::string &sql);
    //插入日志
   void insertLog(const std::string& date, const std::string& app, int hook, const std::string& thread, const std::string& line, const std::string& log);

   //获取日志
   Json::Value getLog(int limit);

   //设置项
    void setSetting(const std::string& app, const std::string& key, const std::string& value);
    std::string getSetting(const std::string& app, const std::string& key);


    //账单
    int insertBill(int id, int type, int currency,int money, int fee, int timeStamp, const std::string& shopName, const std::string& cateName, const std::string& extendData, const std::string& bookName, const std::string& accountNameFrom, const std::string& accountNameTo, const std::string& fromApp, int groupId, const std::string& channel, int syncFromApp, const std::string& remark, int fromType);
    //获取需要同步的账单
    Json::Value getWaitSyncBills();
    //更新账单同步状态
    void updateBillSyncStatus(int id, int status);
    Json::Value getBillListGroup(int limit);
    Json::Value getBillByIds(const std::string& ids);
    Json::Value getBillByGroupId(int groupId);
    //
};


#endif //AUTOACCOUNTING_DBMANAGER_H
