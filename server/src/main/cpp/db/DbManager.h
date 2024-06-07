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
   void insertLog(const std::string& date, const std::string& app, int hook, const std::string& thread, const std::string& line, const std::string& log,int level);

   //获取日志
   Json::Value getLog(int limit);

   void deleteAllLog();

   //设置项
    void setSetting(const std::string& app, const std::string& key, const std::string& value);
    std::string getSetting(const std::string& app, const std::string& key);


    //账单
    int insertBill(int id, int type, const std::string &currency,int money, int fee, int timeStamp, const std::string& shopName, const std::string& cateName, const std::string& extendData, const std::string& bookName, const std::string& accountNameFrom, const std::string& accountNameTo, const std::string& fromApp, int groupId, const std::string& channel, int syncFromApp, const std::string& remark, int fromType);
    //获取需要同步的账单
    Json::Value getWaitSyncBills();
    //更新账单同步状态
    void updateBillSyncStatus(int id, int status);
    //获取账单列表
    Json::Value getBillListGroup(int limit);
    //获取账单
    Json::Value getBillByIds(const std::string& ids);
    //获取账单
    Json::Value getBillByGroupId(int groupId);

    //App数据
    void insertAppData(int id,const std::string& data, int type, const std::string& source,const std::string &rule,int time,int match,int issue);
    Json::Value getAppData(int limit);

    //资产
    void insertAsset(int id, const std::string& name, int type, int sort, const std::string&  icon, const std::string&  extra);
    Json::Value getAsset(int limit);
    Json::Value getAssetByName(const std::string& name);
    void removeAsset(std::string& name);

    //资产映射
    void insertAssetMap(int id, const std::string& name, const std::string&  mapName, int regex);
    Json::Value getAssetMap();
    void removeAssetMap(int id);

    //BookName
    void insertBookName(int id, const std::string& name, const std::string&  icon);
    Json::Value getOneBookName();
    Json::Value getBookName(const std::string& name);
    Json::Value getBookName();
    void removeBookName(const std::string& name);

    //分类
    int insertCate(int id, const std::string& name, const std::string&  icon, const std::string&  remoteId,int parent,int book,int sort,int type);
    Json::Value getAllCate(int book, int type = 0,int parent = 0);
    Json::Value getCate(int book,const std::string& cateName,int type);
    Json::Value getCateByRemote(int book,const std::string& remoteId);
    void removeCate(int id);

    //自定义分类规则
    void insertCustomRule(int id, const std::string& js, const std::string&  text, const std::string&  element, int use,int sort,int _auto);
    Json::Value loadCustomRules(int limit);
    void removeCustomRule(int id);
    Json::Value getCustomRule(int id);

    //规则
    void insertRule( const std::string& app, const std::string&  js, const std::string&  version,  int type);
    Json::Value getRule(const std::string& app, int type);

    Json::Value buildBill(sqlite3_stmt *stmt);

    Json::Value getBillAllParents();

    //检查规则,应该返回两个值
    void ruleSetting( int id,int autoAccounting,int enable);
    std::pair<bool,bool> checkRule(const std::string& app, int  type,const std::string&  channel);
    void removeRule(int id);
    Json::Value getRule(int limit);

    void addBxBills(const Json::Value& billArray,std::string md5);
    Json::Value getBxBills(int limit,int t);


    void syncBook(const Json::Value& bookArray,std::string md5);
    void syncAssets(const Json::Value& assetArray,std::string md5);
};


#endif //AUTOACCOUNTING_DBMANAGER_H
