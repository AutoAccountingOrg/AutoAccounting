//
// Created by Ankio on 2024/5/14.
//

#include <cstdio>
#include "DbManager.h"

DbManager::DbManager() {
    int rc = sqlite3_open_v2("auto.db", &db,
                             SQLITE_OPEN_READWRITE | SQLITE_OPEN_CREATE | SQLITE_OPEN_FULLMUTEX,
                             nullptr);

    if (rc) {
        fprintf(stderr, "Can't open database: %s\n", sqlite3_errmsg(db));
        sqlite3_close(db);
    } else {
        initTable();
    }
}

DbManager::~DbManager() {
    sqlite3_close(db);
}

void DbManager::initTable() {
    //这里是建表数组
    const char *sqls[] = {
            "CREATE TABLE IF NOT EXISTS appData ("
            "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            "data TEXT,"
            "source TEXT,"
            "time INTEGER,"
            "match INTEGER,"
            "rule TEXT,"
            "issue INTEGER,"
            "type INTEGER"
            ");",
            "CREATE TABLE IF NOT EXISTS assets ("
            "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            "name TEXT,"
            "icon TEXT,"
            "sort INTEGER,"
            "type INTEGER,"
            "extras TEXT"
            ");",
            "CREATE TABLE IF NOT EXISTS assetsMap ("
            "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            "regex INTEGER,"
            "name TEXT,"
            "mapName TEXT"
            ");",

            "CREATE TABLE IF NOT EXISTS billInfo ("
            "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            "type INTEGER,"
            "currency TEXT,"
            "money INTEGER,"
            "fee INTEGER,"
            "timeStamp INTEGER,"
            "shopName TEXT,"
            "cateName TEXT,"
            "extendData TEXT,"
            "bookName TEXT,"
            "accountNameFrom TEXT,"
            "accountNameTo TEXT,"
            "fromApp TEXT,"
            "groupId INTEGER,"
            "channel TEXT,"
            "syncFromApp INTEGER,"
            "remark TEXT,"
            "fromType INTEGER"
            ");",
            "CREATE TABLE IF NOT EXISTS bookBill ("
            "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            "amount INTEGER,"
            "time INTEGER,"
            "remark TEXT,"
            "billId TEXT,"
            "type INTEGER,"
            "book TEXT,"
            "category TEXT,"
            "accountFrom TEXT,"
            "accountTo TEXT"
            ");",
            "CREATE TABLE IF NOT EXISTS bookName ("
            "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            "name TEXT,"
            "icon TEXT"
            ");",
            "CREATE TABLE IF NOT EXISTS category ("
            "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            "name TEXT,"
            "icon TEXT,"
            "remoteId TEXT,"
            "parent INTEGER,"
            "book INTEGER,"
            "sort INTEGER,"
            "type INTEGER"
            ");",

            "CREATE TABLE IF NOT EXISTS customRule ("
            "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            "use INTEGER,"
            "sort INTEGER,"
            "auto INTEGER,"
            "js TEXT,"
            "text TEXT,"
            "element TEXT"
            ");",
            "CREATE TABLE IF NOT EXISTS log ("
            "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            "date TEXT," //日志日期
            "app TEXT," //来源app
            "hook INTEGER,"//区分是否为hook
            "level INTEGER,"//区分是否为hook
            "thread TEXT,"//当前在哪个线程
            "line TEXT,"//在哪一行
            "log TEXT"//日志具体内容
            ");",
            "CREATE TABLE IF NOT EXISTS settings ("
            "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            "app TEXT,"//哪个app的设置
            "key TEXT,"//键名
            "val TEXT,"//键值
            "UNIQUE(app, key)"
            ");",
            "CREATE TABLE IF NOT EXISTS rule ("
            "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            "app TEXT,"//app名称，如果为空取所有type类型
            "type INTEGER,"//规则类型
            "js TEXT"//规则内容
            "version TEXT,"//规则版本
            "UNIQUE(app, type)"
            ");",
            "CREATE TABLE IF NOT EXISTS ruleSetting ("
            "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            "app TEXT,"//app名称，如果为空取所有type类型
            "type INTEGER,"//规则类型
            "channel TEXT"//规则名称
            "enable INTEGER,"//是否拉起自动记账
            "auto INTEGER,"//是否自动记账
            "UNIQUE(app, type,channel)"
            ");",
    };

    for (const char *sql: sqls) {
        char *zErrMsg = nullptr;
        int rc = sqlite3_exec(db, sql, nullptr, nullptr, &zErrMsg);
        if (rc != SQLITE_OK) {
            fprintf(stderr, "SQL error 0: %s, sql: %s\n", zErrMsg, sql);
            sqlite3_free(zErrMsg);
        }
    }
}

sqlite3_stmt *DbManager::getStmt(const std::string &sql) {
    sqlite3_stmt *stmt;
    int rc = sqlite3_prepare_v2(db, sql.c_str(), -1, &stmt, nullptr);
    if (rc != SQLITE_OK) {
        fprintf(stderr, "SQL error 6: %s\n", sqlite3_errmsg(db));
        return nullptr;
    }
    return stmt;
}


void DbManager::insertLog(const std::string &date, const std::string &app, int hook,
                          const std::string &thread, const std::string &line,
                          const std::string &log,int level) {
    char *zErrMsg = nullptr;
    //使用参数绑定
    sqlite3_stmt *stmt = getStmt(
            "INSERT INTO log (date, app, hook, thread, line, log,level) VALUES (?, ?, ?, ?, ?, ?,?);");

    sqlite3_bind_text(stmt, 1, date.c_str(), -1, SQLITE_STATIC);
    sqlite3_bind_text(stmt, 2, app.c_str(), -1, SQLITE_STATIC);
    sqlite3_bind_int(stmt, 3, hook);
    sqlite3_bind_text(stmt, 4, thread.c_str(), -1, SQLITE_STATIC);
    sqlite3_bind_text(stmt, 5, line.c_str(), -1, SQLITE_STATIC);
    sqlite3_bind_text(stmt, 6, log.c_str(), -1, SQLITE_STATIC);
    sqlite3_bind_int(stmt, 7, level);
    int rc = sqlite3_step(stmt);
    if (rc != SQLITE_DONE) {
        fprintf(stderr, "SQL error 2: %s\n", sqlite3_errmsg(db));
    }
    sqlite3_finalize(stmt);
    //只保留最近5000条日志
    sqlite3_exec(db,
                 "DELETE FROM log WHERE id NOT IN (SELECT id FROM log ORDER BY id DESC LIMIT 5000);",
                 nullptr, nullptr, &zErrMsg);
    if (zErrMsg) {
        fprintf(stderr, "SQL error 3: %s\n", zErrMsg);
        sqlite3_free(zErrMsg);
    }

}

Json::Value DbManager::getLog(int limit) {
    Json::Value ret;
    char *zErrMsg = nullptr;
    sqlite3_stmt *stmt = getStmt("SELECT * FROM log ORDER BY id DESC LIMIT ?;");
    sqlite3_bind_int(stmt, 1, limit);
    int rc = 0;
    while ((rc = sqlite3_step(stmt)) == SQLITE_ROW) {
        Json::Value log;
        log["id"] = sqlite3_column_int(stmt, 0);
        log["date"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 1));
        log["app"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 2));
        log["hook"] = sqlite3_column_int(stmt, 3);
        log["level"] = sqlite3_column_int(stmt, 4);
        log["thread"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 5));
        log["line"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 6));
        log["log"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 7));
        ret.append(log);
    }
    if (rc != SQLITE_DONE) {
        fprintf(stderr, "SQL error 5: %s\n", sqlite3_errmsg(db));
    }
    sqlite3_finalize(stmt);
    return ret;
}
void DbManager::deleteAllLog(){
    char *zErrMsg = nullptr;
    sqlite3_exec(db,
                 "DELETE FROM log;",
                 nullptr, nullptr, &zErrMsg);
    if (zErrMsg) {
        fprintf(stderr, "SQL error 3: %s\n", zErrMsg);
        sqlite3_free(zErrMsg);
    }
}
void
DbManager::setSetting(const std::string &app, const std::string &key, const std::string &value) {
    char *zErrMsg = nullptr;
    sqlite3_stmt *stmt = getStmt(
            "INSERT OR REPLACE INTO settings (app, key, val) VALUES (?, ?, ?);");
    sqlite3_bind_text(stmt, 1, app.c_str(), -1, SQLITE_STATIC);
    sqlite3_bind_text(stmt, 2, key.c_str(), -1, SQLITE_STATIC);
    sqlite3_bind_text(stmt, 3, value.c_str(), -1, SQLITE_STATIC);
    int rc = sqlite3_step(stmt);
    if (rc != SQLITE_DONE) {
        fprintf(stderr, "SQL error 4: %s\n", sqlite3_errmsg(db));
    }
    sqlite3_finalize(stmt);
}


std::string DbManager::getSetting(const std::string &app, const std::string &key) {
    char *zErrMsg = nullptr;
    sqlite3_stmt *stmt = getStmt("SELECT val FROM settings WHERE app = ? AND key = ?;");
    sqlite3_bind_text(stmt, 1, app.c_str(), -1, SQLITE_STATIC);
    sqlite3_bind_text(stmt, 2, key.c_str(), -1, SQLITE_STATIC);
    int rc = sqlite3_step(stmt);
    std::string ret;
    if (rc == SQLITE_ROW) {
        ret = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 0));
    } else if (rc != SQLITE_DONE) {
        fprintf(stderr, "SQL error 7: %s\n", sqlite3_errmsg(db));
    }
    sqlite3_finalize(stmt);
    return ret;
}

int DbManager::insertBill(int id, int type, const std::string &currency, int money, int fee, int timeStamp,
                           const std::string &shopName, const std::string &cateName,
                           const std::string &extendData, const std::string &bookName,
                           const std::string &accountNameFrom, const std::string &accountNameTo,
                           const std::string &fromApp, int groupId, const std::string &channel,
                           int syncFromApp, const std::string &remark, int fromType) {
    char *zErrMsg = nullptr;

    int count = -1;
    sqlite3_stmt *stmt;
    if (id == 0) {
        //id=0表示插入，反之表示更新
        stmt = getStmt(
                "INSERT INTO billInfo ( type, currency, money, fee, timeStamp, shopName, cateName, extendData, bookName, accountNameFrom, accountNameTo, fromApp, groupId, channel, syncFromApp, remark, fromType) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);");
    } else {
        stmt = getStmt(
                "INSERT OR REPLACE INTO billInfo (id, type, currency, money, fee, timeStamp, shopName, cateName, extendData, bookName, accountNameFrom, accountNameTo, fromApp, groupId, channel, syncFromApp, remark, fromType) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);");
        count = 0;
    }
    if (count == 0) {
        sqlite3_bind_int(stmt, count + 1, id);
    }

    sqlite3_bind_int(stmt, count + 2, type);
    sqlite3_bind_text(stmt, count + 3, currency.c_str(), -1, SQLITE_STATIC);
    sqlite3_bind_int(stmt, count + 4, money);
    sqlite3_bind_int(stmt, count + 5, fee);
    sqlite3_bind_int(stmt, count + 6, timeStamp);
    sqlite3_bind_text(stmt, count + 7, shopName.c_str(), -1, SQLITE_STATIC);
    sqlite3_bind_text(stmt, count + 8, cateName.c_str(), -1, SQLITE_STATIC);
    sqlite3_bind_text(stmt, count + 9, extendData.c_str(), -1, SQLITE_STATIC);
    sqlite3_bind_text(stmt, count + 10, bookName.c_str(), -1, SQLITE_STATIC);
    sqlite3_bind_text(stmt, count + 11, accountNameFrom.c_str(), -1, SQLITE_STATIC);
    sqlite3_bind_text(stmt, count + 12, accountNameTo.c_str(), -1, SQLITE_STATIC);
    sqlite3_bind_text(stmt, count + 13, fromApp.c_str(), -1, SQLITE_STATIC);
    sqlite3_bind_int(stmt, count + 14, groupId);
    sqlite3_bind_text(stmt, count + 15, channel.c_str(), -1, SQLITE_STATIC);
    sqlite3_bind_int(stmt, count + 16, syncFromApp);
    sqlite3_bind_text(stmt, count + 17, remark.c_str(), -1, SQLITE_STATIC);
    sqlite3_bind_int(stmt, count + 18, fromType);


    int rc = sqlite3_step(stmt);
    if (rc != SQLITE_DONE) {
        fprintf(stderr, "SQL error 8: %s\n", sqlite3_errmsg(db));
    }
    sqlite3_finalize(stmt);

    //DELETE FROM BillInfo WHERE id NOT IN (SELECT id FROM BillInfo ORDER BY timeStamp DESC LIMIT 500)

    //本地保留1000条数据

/*    sqlite3_exec(db,
                 "DELETE FROM billInfo WHERE id NOT IN (SELECT id FROM billInfo ORDER BY timeStamp DESC LIMIT 1000);",
                 nullptr, nullptr, &zErrMsg);
    if (zErrMsg) {
        fprintf(stderr, "SQL error 3: %s\n", zErrMsg);
        sqlite3_free(zErrMsg);
    }
    */

    //删除syncFromApp=1并且排名在1000名之后的数据
    sqlite3_exec(db,
                 "DELETE FROM billInfo WHERE syncFromApp=1 AND id NOT IN (SELECT id FROM billInfo WHERE syncFromApp=1 ORDER BY timeStamp DESC LIMIT 1000);",
                 nullptr, nullptr, &zErrMsg);
    if (zErrMsg) {
        fprintf(stderr, "SQL error 3: %s\n", zErrMsg);
        sqlite3_free(zErrMsg);
    }

    //删除groupId!=0并且以groupId为id的数据不在表里的数据
    sqlite3_exec(db,
                 "DELETE FROM billInfo WHERE groupId!=0 AND groupId NOT IN (SELECT id FROM billInfo WHERE groupId=0);",
                 nullptr, nullptr, &zErrMsg);
    if (zErrMsg) {
        fprintf(stderr, "SQL error 3: %s\n", zErrMsg);
        sqlite3_free(zErrMsg);
    }

    //查询需要同步的账单数量
    sqlite3_stmt *stmt2 = getStmt("SELECT COUNT(*) FROM billInfo WHERE syncFromApp=0;");
    int ret = 0;
    if (sqlite3_step(stmt2) == SQLITE_ROW) {
        ret = sqlite3_column_int(stmt2, 0);
    }
    sqlite3_finalize(stmt2);
    return ret;
}

Json::Value DbManager::getWaitSyncBills() {
    Json::Value ret;
    char *zErrMsg = nullptr;
    sqlite3_stmt *stmt = getStmt("SELECT * FROM billInfo WHERE syncFromApp=0 AND groupId=0;");
    int rc = 0;
    while ((rc = sqlite3_step(stmt)) == SQLITE_ROW) {
        Json::Value bill;
        bill["id"] = sqlite3_column_int(stmt, 0);
        bill["type"] = sqlite3_column_int(stmt, 1);
        bill["currency"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 2));
        bill["money"] = sqlite3_column_int(stmt, 3);
        bill["fee"] = sqlite3_column_int(stmt, 4);
        bill["timeStamp"] = sqlite3_column_int(stmt, 5);
        bill["shopName"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 6));
        bill["cateName"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 7));
        bill["extendData"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 8));
        bill["bookName"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 9));
        bill["accountNameFrom"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 10));
        bill["accountNameTo"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 11));
        bill["fromApp"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 12));
        bill["groupId"] = sqlite3_column_int(stmt, 13);
        bill["channel"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 14));
        bill["syncFromApp"] = sqlite3_column_int(stmt, 15);
        bill["remark"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 16));
        bill["fromType"] = sqlite3_column_int(stmt, 17);
        ret.append(bill);
    }
    if (rc != SQLITE_DONE) {
        fprintf(stderr, "SQL error 5: %s\n", sqlite3_errmsg(db));
    }
    sqlite3_finalize(stmt);
    return ret;
}

void DbManager::updateBillSyncStatus(int id, int status) {
    char *zErrMsg = nullptr;
    sqlite3_stmt *stmt = getStmt("UPDATE billInfo SET syncFromApp=? WHERE id=?;");
    sqlite3_bind_int(stmt, 1, status);
    sqlite3_bind_int(stmt, 2, id);
    int rc = sqlite3_step(stmt);
    if (rc != SQLITE_DONE) {
        fprintf(stderr, "SQL error 9: %s\n", sqlite3_errmsg(db));
    }
    sqlite3_finalize(stmt);
}

Json::Value DbManager::getBillListGroup(int limit) {
    //SELECT  strftime('%Y-%m-%d', timeStamp / 1000, 'unixepoch') as date,group_concat(id) as ids FROM BillInfo where  groupId == 0 group by date order by date desc
    Json::Value ret;
    char *zErrMsg = nullptr;
    sqlite3_stmt *stmt = getStmt(
            "SELECT strftime('%Y-%m-%d', timeStamp / 1000, 'unixepoch') as date, group_concat(id) as ids FROM billInfo WHERE groupId = 0 GROUP BY date ORDER BY date DESC LIMIT ?;"
    );
    sqlite3_bind_int(stmt, 1, limit);
    int rc = 0;
    while ((rc = sqlite3_step(stmt)) == SQLITE_ROW) {
        Json::Value bill;
        bill["date"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 0));
        bill["ids"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 1));
        ret.append(bill);
    }
    if (rc != SQLITE_DONE) {
        fprintf(stderr, "SQL error 5: %s\n", sqlite3_errmsg(db));
    }
    sqlite3_finalize(stmt);
    return ret;
}

Json::Value DbManager::getBillByIds(const std::string& ids) {
    Json::Value ret;
    char *zErrMsg = nullptr;
    sqlite3_stmt *stmt = getStmt("SELECT * FROM billInfo WHERE id IN (" + ids + ");");
    int rc = 0;
    while ((rc = sqlite3_step(stmt)) == SQLITE_ROW) {
        ret.append(buildBill(stmt));
    }
    if (rc != SQLITE_DONE) {
        fprintf(stderr, "SQL error 5: %s\n", sqlite3_errmsg(db));
    }
    sqlite3_finalize(stmt);
    return ret;
}

Json::Value DbManager::getBillAllParents() {
    Json::Value ret;
    char *zErrMsg = nullptr;
    sqlite3_stmt *stmt = getStmt("SELECT * FROM BillInfo where  groupId = 0  and syncFromApp = 0;");
    int rc = 0;
    while ((rc = sqlite3_step(stmt)) == SQLITE_ROW) {
        ret.append(buildBill(stmt));
    }
    if (rc != SQLITE_DONE) {
        fprintf(stderr, "SQL error 5: %s\n", sqlite3_errmsg(db));
    }
    sqlite3_finalize(stmt);
    return ret;
}

Json::Value DbManager::getBillByGroupId(int groupId) {
    Json::Value ret;
    char *zErrMsg = nullptr;
    sqlite3_stmt *stmt = getStmt("SELECT * FROM billInfo WHERE groupId = ?;");
    sqlite3_bind_int(stmt, 1, groupId);
    int rc = 0;
    while ((rc = sqlite3_step(stmt)) == SQLITE_ROW) {
        ret.append(buildBill(stmt));
    }
    if (rc != SQLITE_DONE) {
        fprintf(stderr, "SQL error 5: %s\n", sqlite3_errmsg(db));
    }
    sqlite3_finalize(stmt);
    return ret;
}

Json::Value DbManager::buildBill(sqlite3_stmt *stmt){
    Json::Value bill;
    bill["id"] = sqlite3_column_int(stmt, 0);
    bill["type"] = sqlite3_column_int(stmt, 1);
    bill["currency"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 2));
    bill["money"] = sqlite3_column_int(stmt, 3);
    bill["fee"] = sqlite3_column_int(stmt, 4);
    bill["timeStamp"] = sqlite3_column_int(stmt, 5);
    bill["shopName"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 6));
    bill["cateName"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 7));
    bill["extendData"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 8));
    bill["bookName"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 9));
    bill["accountNameFrom"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 10));
    bill["accountNameTo"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 11));
    bill["fromApp"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 12));
    bill["groupId"] = sqlite3_column_int(stmt, 13);
    bill["channel"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 14));
    bill["syncFromApp"] = sqlite3_column_int(stmt, 15);
    bill["remark"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 16));
    bill["fromType"] = sqlite3_column_int(stmt, 17);
    return bill;
}

void DbManager::insertAppData(int id, const std::string &data, int type, const std::string &source,const std::string &rule,
                              int time, int match, int issue) {
    char *zErrMsg = nullptr;
    sqlite3_stmt *stmt ;
    int count = -1;
    if(id == 0){
        stmt = getStmt(
                "INSERT INTO appData ( data, type, source, time, match, issue,rule) VALUES (?,?,?,?,?,?,?);");
    }else{
        count = 0;
        stmt = getStmt(
                "INSERT OR REPLACE INTO appData (id, data, type, source, time, match, issue,rule) VALUES (?,?,?,?,?,?,?,?);");
    }
    if(count == 0){
        sqlite3_bind_int(stmt, count + 1, id);
    }
    sqlite3_bind_text(stmt, count + 2, data.c_str(), -1, SQLITE_STATIC);
    sqlite3_bind_int(stmt, count + 3, type);
    sqlite3_bind_text(stmt,  count +4, source.c_str(), -1, SQLITE_STATIC);
    sqlite3_bind_int(stmt,  count +5, time);
    sqlite3_bind_int(stmt,  count +6, match);
    sqlite3_bind_int(stmt, count + 7, issue);
    sqlite3_bind_text(stmt, count + 8, rule.c_str(), -1, SQLITE_STATIC);
    int rc = sqlite3_step(stmt);
    if (rc != SQLITE_DONE) {
        fprintf(stderr, "SQL error 8: %s\n", sqlite3_errmsg(db));
    }
    sqlite3_finalize(stmt);
}

Json::Value DbManager::getAppData(int limit) {
    Json::Value ret;
    char *zErrMsg = nullptr;
    sqlite3_stmt *stmt = getStmt("SELECT * FROM appData ORDER BY id DESC LIMIT ?;");
    sqlite3_bind_int(stmt, 1, limit);
    int rc = 0;
    while ((rc = sqlite3_step(stmt)) == SQLITE_ROW) {
        Json::Value appData;
        appData["id"] = sqlite3_column_int(stmt, 0);
        appData["data"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 1));
        appData["type"] = sqlite3_column_int(stmt, 2);
        appData["source"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 3));
        appData["time"] = sqlite3_column_int(stmt, 4);
        appData["match"] = sqlite3_column_int(stmt, 5);
        appData["issue"] = sqlite3_column_int(stmt, 6);
        ret.append(appData);
    }
    if (rc != SQLITE_DONE) {
        fprintf(stderr, "SQL error 5: %s\n", sqlite3_errmsg(db));
    }
    sqlite3_finalize(stmt);
    return ret;
}

void DbManager::insertAsset(int id, const std::string &name, int type, int sort, const std::string &icon,
                            const std::string &extra) {
    char *zErrMsg = nullptr;
    sqlite3_stmt *stmt;
    int count = -1;
    if(id == 0){
        stmt = getStmt(
                "INSERT INTO assets ( name, type, sort, icon, extras) VALUES (?,?,?,?,?);");
    }else{
        count = 0;
        stmt = getStmt(
                "INSERT OR REPLACE INTO assets (id, name, type, sort, icon, extras) VALUES (?,?,?,?,?,?,?);");
    }
    if(count == 0){
        sqlite3_bind_int(stmt, count + 1, id);
    }
    sqlite3_bind_text(stmt, count + 2, name.c_str(), -1, SQLITE_STATIC);
    sqlite3_bind_int(stmt, count + 3, type);
    sqlite3_bind_int(stmt, count + 4, sort);
    sqlite3_bind_text(stmt, count + 5, icon.c_str(), -1, SQLITE_STATIC);
    sqlite3_bind_text(stmt, count + 6, extra.c_str(), -1, SQLITE_STATIC);
    int rc = sqlite3_step(stmt);
    if (rc != SQLITE_DONE) {
        fprintf(stderr, "SQL error 8: %s\n", sqlite3_errmsg(db));
    }
    sqlite3_finalize(stmt);
}

Json::Value DbManager::getAsset(int limit) {
    Json::Value ret;
    char *zErrMsg = nullptr;
    sqlite3_stmt *stmt = getStmt("SELECT * FROM assets ORDER BY id DESC LIMIT ?;");
    sqlite3_bind_int(stmt, 1, limit);
    int rc = 0;
    while ((rc = sqlite3_step(stmt)) == SQLITE_ROW) {
        Json::Value asset;
        asset["id"] = sqlite3_column_int(stmt, 0);
        asset["name"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 1));
        asset["type"] = sqlite3_column_int(stmt, 2);
        asset["sort"] = sqlite3_column_int(stmt, 3);
        asset["icon"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 4));
        asset["extras"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 5));
        ret.append(asset);
    }
    if (rc != SQLITE_DONE) {
        fprintf(stderr, "SQL error 5: %s\n", sqlite3_errmsg(db));
    }
    sqlite3_finalize(stmt);
    return ret;
}

Json::Value DbManager::getAssetByName(const std::string &name) {
    Json::Value asset;
    char *zErrMsg = nullptr;
    sqlite3_stmt *stmt = getStmt("SELECT * FROM assets WHERE name = ? LIMIT 1;");
    sqlite3_bind_text(stmt, 1, name.c_str(), -1, SQLITE_STATIC);
    int rc = 0;
    while ((rc = sqlite3_step(stmt)) == SQLITE_ROW) {
        asset["id"] = sqlite3_column_int(stmt, 0);
        asset["name"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 1));
        asset["type"] = sqlite3_column_int(stmt, 2);
        asset["sort"] = sqlite3_column_int(stmt, 3);
        asset["icon"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 4));
        asset["extras"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 5));
    }
    if (rc != SQLITE_DONE) {
        fprintf(stderr, "SQL error 5: %s\n", sqlite3_errmsg(db));
    }
    sqlite3_finalize(stmt);
    return asset;
}

void DbManager::removeAsset(std::string &name) {
    char *zErrMsg = nullptr;
    sqlite3_stmt *stmt = getStmt("DELETE FROM assets WHERE name = ?;");
    sqlite3_bind_text(stmt, 1, name.c_str(), -1, SQLITE_STATIC);
    int rc = sqlite3_step(stmt);
    if (rc != SQLITE_DONE) {
        fprintf(stderr, "SQL error 9: %s\n", sqlite3_errmsg(db));
    }
    sqlite3_finalize(stmt);
}

void DbManager::insertAssetMap(int id, const std::string &name, const std::string &mapName, int regex) {
    char *zErrMsg = nullptr;
    sqlite3_stmt *stmt;
    int count = -1;
    if(id == 0){
        stmt = getStmt(
                "INSERT INTO assetsMap ( name, mapName, regex) VALUES (?,?,?);");
    }else{
        count = 0;
        stmt = getStmt(
                "INSERT OR REPLACE INTO assetsMap (id, name, mapName, regex) VALUES (?,?,?,?);");
    }
    if(count == 0){
        sqlite3_bind_int(stmt, count + 1, id);
    }
    sqlite3_bind_text(stmt, count + 2, name.c_str(), -1, SQLITE_STATIC);
    sqlite3_bind_text(stmt, count + 3, mapName.c_str(), -1, SQLITE_STATIC);
    sqlite3_bind_int(stmt, count + 4, regex);
    int rc = sqlite3_step(stmt);
    if (rc != SQLITE_DONE) {
        fprintf(stderr, "SQL error 8: %s\n", sqlite3_errmsg(db));
    }
    sqlite3_finalize(stmt);
}

Json::Value DbManager::getAssetMap() {
    Json::Value ret;
    char *zErrMsg = nullptr;
    sqlite3_stmt *stmt = getStmt("SELECT * FROM assetsMap ORDER BY id DESC;");
    int rc = 0;
    while ((rc = sqlite3_step(stmt)) == SQLITE_ROW) {
        Json::Value assetMap;
        assetMap["id"] = sqlite3_column_int(stmt, 0);
        assetMap["name"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 1));
        assetMap["mapName"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 2));
        assetMap["regex"] = sqlite3_column_int(stmt, 3);
        ret.append(assetMap);
    }
    if (rc != SQLITE_DONE) {
        fprintf(stderr, "SQL error 5: %s\n", sqlite3_errmsg(db));
    }
    sqlite3_finalize(stmt);
    return ret;
}

void DbManager::removeAssetMap(int id) {
    char *zErrMsg = nullptr;
    sqlite3_stmt *stmt = getStmt("DELETE FROM assetsMap WHERE id = ?;");
    sqlite3_bind_int(stmt, 1, id);
    int rc = sqlite3_step(stmt);
    if (rc != SQLITE_DONE) {
        fprintf(stderr, "SQL error 9: %s\n", sqlite3_errmsg(db));
    }
    sqlite3_finalize(stmt);
}


void DbManager::insertBookName(int id, const std::string &name, const std::string &icon) {
    char *zErrMsg = nullptr;
    sqlite3_stmt *stmt;
    int count = -1;
    if(id == 0){
        stmt = getStmt(
                "INSERT INTO bookName ( name, icon) VALUES (?,?);");
    }else{
        count = 0;
        stmt = getStmt(
                "INSERT OR REPLACE INTO bookName (id, name, icon) VALUES (?,?,?);");
    }
    if(count == 0){
        sqlite3_bind_int(stmt, count + 1, id);
    }
    sqlite3_bind_text(stmt, count + 2, name.c_str(), -1, SQLITE_STATIC);
    sqlite3_bind_text(stmt, count + 3, icon.c_str(), -1, SQLITE_STATIC);
    int rc = sqlite3_step(stmt);
    if (rc != SQLITE_DONE) {
        fprintf(stderr, "SQL error 8: %s\n", sqlite3_errmsg(db));
    }
    sqlite3_finalize(stmt);
}

Json::Value DbManager::getBookName() {
    Json::Value ret;
    char *zErrMsg = nullptr;
    sqlite3_stmt *stmt = getStmt("SELECT * FROM bookName ORDER BY id DESC;");
    int rc = 0;
    while ((rc = sqlite3_step(stmt)) == SQLITE_ROW) {
        Json::Value bookName;
        bookName["id"] = sqlite3_column_int(stmt, 0);
        bookName["name"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 1));
        bookName["icon"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 2));
        ret.append(bookName);
    }
    if (rc != SQLITE_DONE) {
        fprintf(stderr, "SQL error 5: %s\n", sqlite3_errmsg(db));
    }
    sqlite3_finalize(stmt);
    return ret;
}

void DbManager::removeBookName(const std::string& name){
    char *zErrMsg = nullptr;
    sqlite3_stmt *stmt = getStmt("DELETE FROM bookName WHERE name = ?;");
    sqlite3_bind_text(stmt, 1, name.c_str(), -1, SQLITE_STATIC);
    int rc = sqlite3_step(stmt);
    if (rc != SQLITE_DONE) {
        fprintf(stderr, "SQL error 9: %s\n", sqlite3_errmsg(db));
    }
    sqlite3_finalize(stmt);
}

Json::Value DbManager::getBookName(const std::string& name){
    Json::Value bookName;
    char *zErrMsg = nullptr;
    sqlite3_stmt *stmt = getStmt("SELECT * FROM bookName WHERE name = ?;");
    sqlite3_bind_text(stmt, 1, name.c_str(), -1, SQLITE_STATIC);
    int rc = 0;
    while ((rc = sqlite3_step(stmt)) == SQLITE_ROW) {
        bookName["id"] = sqlite3_column_int(stmt, 0);
        bookName["name"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 1));
        bookName["icon"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 2));
    }
    if (rc != SQLITE_DONE) {
        fprintf(stderr, "SQL error 5: %s\n", sqlite3_errmsg(db));
    }
    sqlite3_finalize(stmt);
    return bookName;
}

Json::Value DbManager::getOneBookName() {
    Json::Value ret;
    char *zErrMsg = nullptr;
    sqlite3_stmt *stmt = getStmt("SELECT * FROM bookName ORDER BY id DESC LIMIT 1;");
    int rc = 0;
    while ((rc = sqlite3_step(stmt)) == SQLITE_ROW) {
        Json::Value bookName;
        bookName["id"] = sqlite3_column_int(stmt, 0);
        bookName["name"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 1));
        bookName["icon"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 2));
        ret.append(bookName);
    }
    if (rc != SQLITE_DONE) {
        fprintf(stderr, "SQL error 5: %s\n", sqlite3_errmsg(db));
    }
    sqlite3_finalize(stmt);
    return ret;
}


int DbManager::insertCate(int id, const std::string &name, const std::string &icon,
                          const std::string &remoteId, int parent, int book, int sort, int type) {
    char *zErrMsg = nullptr;
    sqlite3_stmt *stmt;
    int count = -1;
    if (id == 0) {
        stmt = getStmt(
                "INSERT INTO category ( name, icon, remoteId, parent, book, sort, type) VALUES (?,?,?,?,?,?,?);");
    } else {
        count = 0;
        stmt = getStmt(
                "INSERT OR REPLACE INTO category (id, name, icon, remoteId, parent, book, sort, type) VALUES (?,?,?,?,?,?,?,?);");

    }
    if (count == 0) {
        sqlite3_bind_int(stmt, count + 1, id);
    }
    sqlite3_bind_text(stmt, count + 2, name.c_str(), -1, SQLITE_STATIC);
    sqlite3_bind_text(stmt, count + 3, icon.c_str(), -1, SQLITE_STATIC);
    sqlite3_bind_text(stmt, count + 4, remoteId.c_str(), -1, SQLITE_STATIC);
    sqlite3_bind_int(stmt, count + 5, parent);
    sqlite3_bind_int(stmt, count + 6, book);
    sqlite3_bind_int(stmt, count + 7, sort);
    sqlite3_bind_int(stmt, count + 8, type);
    int rc = sqlite3_step(stmt);
    if (rc != SQLITE_DONE) {
        fprintf(stderr, "SQL error 8: %s\n", sqlite3_errmsg(db));
    }
    sqlite3_finalize(stmt);
    return sqlite3_last_insert_rowid(db);
}

Json::Value DbManager::getAllCate(int parent, int book, int type) {
    Json::Value ret;
    char *zErrMsg = nullptr;
    sqlite3_stmt *stmt = getStmt(
            "SELECT * FROM category WHERE parent = ? AND book = ? AND type = ? ORDER BY sort;");
    sqlite3_bind_int(stmt, 1, parent);
    sqlite3_bind_int(stmt, 2, book);
    sqlite3_bind_int(stmt, 3, type);
    int rc = 0;
    while ((rc = sqlite3_step(stmt)) == SQLITE_ROW) {
        Json::Value cate;
        cate["id"] = sqlite3_column_int(stmt, 0);
        cate["name"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 1));
        cate["icon"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 2));
        cate["remoteId"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 3));
        cate["parent"] = sqlite3_column_int(stmt, 4);
        cate["book"] = sqlite3_column_int(stmt, 5);
        cate["sort"] = sqlite3_column_int(stmt, 6);
        cate["type"] = sqlite3_column_int(stmt, 7);
       // cate["child"] = getAllCate(db, sqlite3_column_int(stmt, 0), book, type);
        ret.append(cate);
    }
    if (rc != SQLITE_DONE) {
        fprintf(stderr, "SQL error 5: %s\n", sqlite3_errmsg(db));
    }
    sqlite3_finalize(stmt);
    return ret;
}

Json::Value DbManager::getCate(int book, const std::string &cateName,int type) {
    Json::Value ret;
    char *zErrMsg = nullptr;
    sqlite3_stmt *stmt = getStmt(
            "SELECT * FROM category WHERE book = ? AND name = ? AND type = ?;");
    sqlite3_bind_int(stmt, 1, book);
    sqlite3_bind_text(stmt, 2, cateName.c_str(), -1, SQLITE_STATIC);
    sqlite3_bind_int(stmt, 3, type);
    int rc = 0;
    while ((rc = sqlite3_step(stmt)) == SQLITE_ROW) {
        Json::Value cate;
        cate["id"] = sqlite3_column_int(stmt, 0);
        cate["name"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 1));
        cate["icon"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 2));
        cate["remoteId"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 3));
        cate["parent"] = sqlite3_column_int(stmt, 4);
        cate["book"] = sqlite3_column_int(stmt, 5);
        cate["sort"] = sqlite3_column_int(stmt, 6);
        cate["type"] = sqlite3_column_int(stmt, 7);
        ret.append(cate);
    }
    if (rc != SQLITE_DONE) {
        fprintf(stderr, "SQL error 5: %s\n", sqlite3_errmsg(db));
    }
    sqlite3_finalize(stmt);
    return ret;
}

Json::Value DbManager::getCateByRemote(int book, const std::string &remoteId) {
    Json::Value ret;
    char *zErrMsg = nullptr;
    sqlite3_stmt *stmt = getStmt(
            "SELECT * FROM category WHERE book = ? AND remoteId = ?  limit 1;");
    sqlite3_bind_int(stmt, 1, book);
    sqlite3_bind_text(stmt, 2, remoteId.c_str(), -1, SQLITE_STATIC);
    int rc = 0;
    while ((rc = sqlite3_step(stmt)) == SQLITE_ROW) {
        ret["id"] = sqlite3_column_int(stmt, 0);
        ret["name"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 1));
        ret["icon"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 2));
        ret["remoteId"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 3));
        ret["parent"] = sqlite3_column_int(stmt, 4);
        ret["book"] = sqlite3_column_int(stmt, 5);
        ret["sort"] = sqlite3_column_int(stmt, 6);
        ret["type"] = sqlite3_column_int(stmt, 7);
    }
    if (rc != SQLITE_DONE) {
        fprintf(stderr, "SQL error 5: %s\n", sqlite3_errmsg(db));
    }
    sqlite3_finalize(stmt);
    return ret;
}

void DbManager::removeCate(int id) {
    char *zErrMsg = nullptr;
    sqlite3_stmt *stmt = getStmt("DELETE FROM category WHERE id = ?;");
    sqlite3_bind_int(stmt, 1, id);
    int rc = sqlite3_step(stmt);
    if (rc != SQLITE_DONE) {
        fprintf(stderr, "SQL error 9: %s\n", sqlite3_errmsg(db));
    }
    sqlite3_finalize(stmt);
}

void DbManager::insertRule(const std::string &app, const std::string &js,
                           const std::string &version, int type) {
    char *zErrMsg = nullptr;
    sqlite3_stmt *stmt = getStmt(
            "INSERT OR REPLACE INTO rule ( app, js, version, type) VALUES (?,?,?,?);");
    sqlite3_bind_text(stmt, 1, app.c_str(), -1, SQLITE_STATIC);
    sqlite3_bind_text(stmt, 2, js.c_str(), -1, SQLITE_STATIC);
    sqlite3_bind_text(stmt, 3, version.c_str(), -1, SQLITE_STATIC);
    sqlite3_bind_int(stmt, 4, type);
    int rc = sqlite3_step(stmt);
    if (rc != SQLITE_DONE) {
        fprintf(stderr, "SQL error 8: %s\n", sqlite3_errmsg(db));
    }
    sqlite3_finalize(stmt);

}

Json::Value DbManager::getRule(const std::string &app, int type) {
    Json::Value ret;
    char *zErrMsg = nullptr;
    sqlite3_stmt *stmt = getStmt("SELECT * FROM rule WHERE app = ? AND type = ?;");
    sqlite3_bind_text(stmt, 1, app.c_str(), -1, SQLITE_STATIC);
    sqlite3_bind_int(stmt, 2, type);
    int rc = 0;
    while ((rc = sqlite3_step(stmt)) == SQLITE_ROW) {
        Json::Value rule;
        rule["app"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 1));
        rule["js"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 2));
        rule["version"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 3));
        rule["type"] = sqlite3_column_int(stmt, 4);
        ret.append(rule);
    }
    if (rc != SQLITE_DONE) {
        fprintf(stderr, "SQL error 5: %s\n", sqlite3_errmsg(db));
    }
    sqlite3_finalize(stmt);
    return ret;
}

void DbManager::insertCustomRule(int id, const std::string &js, const std::string &text,
                                 const std::string &element, int use, int sort, int _auto) {
    char *zErrMsg = nullptr;
    sqlite3_stmt *stmt;
    int count = -1;
    if(id == 0){
        stmt = getStmt(
                "INSERT INTO customRule ( js, text, element, use, sort, auto) VALUES (?,?,?,?,?,?);");
    }else{
        count = 0;
        stmt = getStmt(
                "INSERT OR REPLACE INTO customRule (id, js, text, element, use, sort, auto) VALUES (?,?,?,?,?,?,?,?);");

    }
    if(count == 0){
        sqlite3_bind_int(stmt, count + 1, id);
    }
    sqlite3_bind_text(stmt, count + 2, js.c_str(), -1, SQLITE_STATIC);
    sqlite3_bind_text(stmt, count + 3, text.c_str(), -1, SQLITE_STATIC);
    sqlite3_bind_text(stmt, count + 4, element.c_str(), -1, SQLITE_STATIC);
    sqlite3_bind_int(stmt, count + 5, use);
    sqlite3_bind_int(stmt, count + 6, sort);
    sqlite3_bind_int(stmt, count + 7, _auto);
    int rc = sqlite3_step(stmt);
    if (rc != SQLITE_DONE) {
        fprintf(stderr, "SQL error 8: %s\n", sqlite3_errmsg(db));
    }
    sqlite3_finalize(stmt);
}

Json::Value DbManager::loadCustomRules(int limit) {
    Json::Value ret;
    char *zErrMsg = nullptr;
    sqlite3_stmt *stmt = getStmt("SELECT * FROM customRule ORDER BY sort LIMIT ?;");
    sqlite3_bind_int(stmt, 1, limit);
    int rc = 0;
    while ((rc = sqlite3_step(stmt)) == SQLITE_ROW) {
        Json::Value rule;
        rule["id"] = sqlite3_column_int(stmt, 0);
        rule["js"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 4));
        rule["text"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 5));
        rule["element"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 6));
        rule["use"] = sqlite3_column_int(stmt, 1) == 1;
        rule["sort"] = sqlite3_column_int(stmt, 2);
        rule["auto"] = sqlite3_column_int(stmt, 3) == 1;
        ret.append(rule);
    }
    if (rc != SQLITE_DONE) {
        fprintf(stderr, "SQL error 5: %s\n", sqlite3_errmsg(db));
    }
    sqlite3_finalize(stmt);
    return ret;
}

void DbManager::removeCustomRule(int id) {
    char *zErrMsg = nullptr;
    sqlite3_stmt *stmt = getStmt("DELETE FROM customRule WHERE id = ?;");
    sqlite3_bind_int(stmt, 1, id);
    int rc = sqlite3_step(stmt);
    if (rc != SQLITE_DONE) {
        fprintf(stderr, "SQL error 9: %s\n", sqlite3_errmsg(db));
    }
    sqlite3_finalize(stmt);
}

Json::Value DbManager::getCustomRule(int id) {
    Json::Value ret;
    char *zErrMsg = nullptr;
    sqlite3_stmt *stmt = getStmt("SELECT * FROM customRule WHERE id = ?;");
    sqlite3_bind_int(stmt, 1, id);
    int rc = 0;
    while ((rc = sqlite3_step(stmt)) == SQLITE_ROW) {
        Json::Value rule;
        rule["id"] = sqlite3_column_int(stmt, 0);
        rule["js"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 4));
        rule["text"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 5));
        rule["element"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 6));
        rule["use"] = sqlite3_column_int(stmt, 1);
        rule["sort"] = sqlite3_column_int(stmt, 2);
        rule["auto"] = sqlite3_column_int(stmt, 3);
        ret.append(rule);
    }
    if (rc != SQLITE_DONE) {
        fprintf(stderr, "SQL error 5: %s\n", sqlite3_errmsg(db));
    }
    sqlite3_finalize(stmt);
    return ret;
}
//ruleSetting

std::pair<bool,bool>  DbManager::checkRule(const std::string& app, int  type,const std::string&  channel){
    char *zErrMsg = nullptr;
    sqlite3_stmt *stmt = getStmt("SELECT * FROM ruleSetting WHERE app = ? AND type = ? AND channel = ?;");
    sqlite3_bind_text(stmt, 1, app.c_str(), -1, SQLITE_STATIC);
    sqlite3_bind_int(stmt, 2, type);
    sqlite3_bind_text(stmt, 3, channel.c_str(), -1, SQLITE_STATIC);
    int rc = 0;
    bool ret = true;
    bool ret2 = true;
    //如果没有查到数据就自己添加数据
    if ((rc = sqlite3_step(stmt)) == SQLITE_DONE) {
        sqlite3_stmt *stmt2 = getStmt("INSERT INTO ruleSetting ( app, type, channel, enable, auto) VALUES (?,?,?,?,?);");
        sqlite3_bind_text(stmt2, 1, app.c_str(), -1, SQLITE_STATIC);
        sqlite3_bind_int(stmt2, 2, type);
        sqlite3_bind_text(stmt2, 3, channel.c_str(), -1, SQLITE_STATIC);
        sqlite3_bind_int(stmt2, 4, 1);
        sqlite3_bind_int(stmt2, 5, 1);
        int rc2 = sqlite3_step(stmt2);
        if (rc2 != SQLITE_DONE) {
            fprintf(stderr, "SQL error 8: %s\n", sqlite3_errmsg(db));
        }
        sqlite3_finalize(stmt2);
    }else{
        ret = sqlite3_column_int(stmt, 3);
        ret2 = sqlite3_column_int(stmt, 4);
    }
    if (rc != SQLITE_DONE) {
        fprintf(stderr, "SQL error 5: %s\n", sqlite3_errmsg(db));
    }
    sqlite3_finalize(stmt);
    return std::make_pair(ret,ret2);
}

void DbManager::ruleSetting(int id,int autoAccounting,int enable){
    char *zErrMsg = nullptr;
    sqlite3_stmt *stmt = getStmt("UPDATE ruleSetting SET auto = ?, enable = ? WHERE id = ?;");
    sqlite3_bind_int(stmt, 1, autoAccounting);
    sqlite3_bind_int(stmt, 2, enable);
    int rc = sqlite3_step(stmt);
    if (rc != SQLITE_DONE) {
        fprintf(stderr, "SQL error 8: %s\n", sqlite3_errmsg(db));
    }
    sqlite3_finalize(stmt);
}

Json::Value DbManager::getRule(int limit){
    Json::Value ret;
    char *zErrMsg = nullptr;
    sqlite3_stmt *stmt = getStmt("SELECT * FROM ruleSetting ORDER BY id DESC LIMIT ?;");
    sqlite3_bind_int(stmt, 1, limit);
    int rc = 0;
    while ((rc = sqlite3_step(stmt)) == SQLITE_ROW) {
        Json::Value rule;
        rule["id"] = sqlite3_column_int(stmt, 0);
        rule["app"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 1));
        rule["type"] = sqlite3_column_int(stmt, 2);
        rule["channel"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 3));
        rule["enable"] = sqlite3_column_int(stmt, 4);
        rule["auto"] = sqlite3_column_int(stmt, 5);
        ret.append(rule);
    }
    if (rc != SQLITE_DONE) {
        fprintf(stderr, "SQL error 5: %s\n", sqlite3_errmsg(db));
    }
    sqlite3_finalize(stmt);
    return ret;
}

void DbManager::removeRule(int id) {
    char *zErrMsg = nullptr;
    sqlite3_stmt *stmt = getStmt("DELETE FROM ruleSetting WHERE id = ?;");
    sqlite3_bind_int(stmt, 1, id);
    int rc = sqlite3_step(stmt);
    if (rc != SQLITE_DONE) {
        fprintf(stderr, "SQL error 9: %s\n", sqlite3_errmsg(db));
    }
    sqlite3_finalize(stmt);
}

void DbManager::addBxBills(const Json::Value& billArray){
    //这里使用事物操作
    sqlite3_exec(db, "BEGIN;", nullptr, nullptr, nullptr);
    try {
        //先清空bookBill表
        sqlite3_stmt *stmt = getStmt("DELETE FROM bookBill;");
        int rc = sqlite3_step(stmt);
        if (rc != SQLITE_DONE) {
            fprintf(stderr, "SQL error 9: %s\n", sqlite3_errmsg(db));
        }
        sqlite3_finalize(stmt);
        //插入数据
        for (auto bill : billArray) {
            std::string billId = bill["billId"].asString();
            int amount = bill["amount"].asInt();
            int time = bill["time"].asInt();
            std::string remark = bill["remark"].asString();
            int type = bill["type"].asInt();
            std::string book = bill["book"].asString();
            std::string category = bill["category"].asString();
            std::string accountFrom = bill["accountFrom"].asString();
            std::string accountTo = bill["accountTo"].asString();

            sqlite3_stmt *stmt2 = getStmt(
                    "INSERT INTO bookBill ( billId, amount, time, remark, type, book, category, accountFrom, accountTo) VALUES (?,?,?,?,?,?,?,?,?);");

            sqlite3_bind_text(stmt2, 1, billId.c_str(), -1, SQLITE_STATIC);
            sqlite3_bind_int(stmt2, 2, amount);
            sqlite3_bind_int(stmt2, 3, time);
            sqlite3_bind_text(stmt2, 4, remark.c_str(), -1, SQLITE_STATIC);
            sqlite3_bind_int(stmt2, 5, type);
            sqlite3_bind_text(stmt2, 6, book.c_str(), -1, SQLITE_STATIC);
            sqlite3_bind_text(stmt2, 7, category.c_str(), -1, SQLITE_STATIC);
            sqlite3_bind_text(stmt2, 8, accountFrom.c_str(), -1, SQLITE_STATIC);
            sqlite3_bind_text(stmt2, 9, accountTo.c_str(), -1, SQLITE_STATIC);
            int rc2 = sqlite3_step(stmt2);
            if (rc2 != SQLITE_DONE) {
                fprintf(stderr, "SQL error 8: %s\n", sqlite3_errmsg(db));
            }
            sqlite3_finalize(stmt2);

        }
        sqlite3_exec(db, "COMMIT;", nullptr, nullptr, nullptr);

    } catch (...) {
        sqlite3_exec(db, "ROLLBACK;", nullptr, nullptr, nullptr);
    }
}

Json::Value DbManager::getBxBills(int limit,int t) {
    Json::Value ret;
    char *zErrMsg = nullptr;
    sqlite3_stmt *stmt = getStmt("SELECT * FROM bookBill WHERE type=? ORDER BY time DESC LIMIT ?;");
    sqlite3_bind_int(stmt, 1, t);
    sqlite3_bind_int(stmt, 2, limit);
    int rc = 0;
    while ((rc = sqlite3_step(stmt)) == SQLITE_ROW) {
        Json::Value bill;
        bill["billId"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 0));
        bill["amount"] = sqlite3_column_int(stmt, 1);
        bill["time"] = sqlite3_column_int(stmt, 2);
        bill["remark"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 3));
        bill["type"] = sqlite3_column_int(stmt, 4);
        bill["book"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 5));
        bill["category"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 6));
        bill["accountFrom"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 7));
        bill["accountTo"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 8));
        ret.append(bill);
    }
    if (rc != SQLITE_DONE) {
        fprintf(stderr, "SQL error 5: %s\n", sqlite3_errmsg(db));
    }
    sqlite3_finalize(stmt);
    return ret;
}

void DbManager::syncBook(const Json::Value &bookArray) {
    //这里使用事物操作
    sqlite3_exec(db, "BEGIN;", nullptr, nullptr, nullptr);
    try {
        //先清空book表
        sqlite3_stmt *stmt = getStmt("DELETE FROM bookName;");
        int rc = sqlite3_step(stmt);
        if (rc != SQLITE_DONE) {
            fprintf(stderr, "SQL error 9: %s\n", sqlite3_errmsg(db));
        }
        sqlite3_finalize(stmt);

        //清空category表
        sqlite3_stmt *stmt1 = getStmt("DELETE FROM category;");
        int rc1 = sqlite3_step(stmt1);
        if (rc1 != SQLITE_DONE) {
            fprintf(stderr, "SQL error 9: %s\n", sqlite3_errmsg(db));
        }
        sqlite3_finalize(stmt1);

        //插入数据
        for (auto book : bookArray) {
            Json::Value category = book["category"];
            std::string name = book["name"].asString();
            std::string icon = book["icon"].asString();
            sqlite3_stmt *stmt2 = getStmt(
                    "INSERT INTO bookName ( name, icon) VALUES (?,?);");

            sqlite3_bind_text(stmt2, 1, name.c_str(), -1, SQLITE_STATIC);
            sqlite3_bind_text(stmt2, 2, icon.c_str(), -1, SQLITE_STATIC);
            int rc2 = sqlite3_step(stmt2);
            if (rc2 != SQLITE_DONE) {
                fprintf(stderr, "SQL error 8: %s\n", sqlite3_errmsg(db));
            }
            sqlite3_finalize(stmt2);

            //获取插入的id
            long long bookId = sqlite3_last_insert_rowid(db);

            std::vector<Json::Value> sortedModel;
            for (const auto& it : category) {
                sortedModel.push_back(it);
            }
            std::sort(sortedModel.begin(), sortedModel.end(), [](const Json::Value& a, const Json::Value& b) {
                if ((a["parent"].asString() != "-1") != (b["parent"].asString() != "-1")) {
                    return a["parent"].asString() != "-1";
                }
                return a["sort"].asInt() < b["sort"].asInt();
            });

            // 处理排序后的数据
            for (const auto& it : sortedModel) {
                std::string cateName = it["name"].asString();
                std::string cateIcon = it["icon"].asString();
                std::string cateRemoteId = it["id"].asString();
                std::string cateParent = it["parent"].asString();
                int cateSort = it["sort"].asInt();
                int cateType = it["type"].asInt();

                if (cateParent != "-1") {

                    //查找remoteId
                    sqlite3_stmt *stmt3 = getStmt("SELECT * FROM category WHERE book = ? AND remoteId = ?;");
                    sqlite3_bind_int64(stmt3, 1, bookId);
                    sqlite3_bind_text(stmt3, 2, cateParent.c_str(), -1, SQLITE_STATIC);
                    int rc3 = sqlite3_step(stmt3);
                    if (rc3 == SQLITE_ROW) {
                        cateParent = std::to_string(sqlite3_column_int(stmt3, 0));
                    }
                    sqlite3_finalize(stmt3);
                }

                //插入数据库
                sqlite3_stmt *stmt3 = getStmt(
                        "INSERT INTO category ( name, icon, remoteId, parent, book, sort, type) VALUES (?,?,?,?,?,?,?,?);");
                sqlite3_bind_text(stmt3, 1, cateName.c_str(), -1, SQLITE_STATIC);
                sqlite3_bind_text(stmt3, 2, cateIcon.c_str(), -1, SQLITE_STATIC);
                sqlite3_bind_text(stmt3, 3, cateRemoteId.c_str(), -1, SQLITE_STATIC);
                sqlite3_bind_int(stmt3, 4, cateParent == "-1" ? 0 : std::stoi(cateParent));
                sqlite3_bind_int64(stmt3, 5, bookId);
                sqlite3_bind_int(stmt3, 6, cateSort);
                sqlite3_bind_int(stmt3, 7, cateType);
                int rc3 = sqlite3_step(stmt3);
                if (rc3 != SQLITE_DONE) {
                    fprintf(stderr, "SQL error 8: %s\n", sqlite3_errmsg(db));
                }
                sqlite3_finalize(stmt3);

            }


        }
        sqlite3_exec(db, "COMMIT;", nullptr, nullptr, nullptr);

    } catch (...) {
        sqlite3_exec(db, "ROLLBACK;", nullptr, nullptr, nullptr);
    }
}