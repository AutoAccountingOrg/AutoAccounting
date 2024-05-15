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
            "CREATE TABLE IF NOT EXISTS billInfo ("
            "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            "type INTEGER,"
            "currency INTEGER,"
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
            "version TEXT"//规则版本
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
                          const std::string &log) {
    char *zErrMsg = nullptr;
    //使用参数绑定
    sqlite3_stmt *stmt = getStmt(
            "INSERT INTO log (date, app, hook, thread, line, log) VALUES (?, ?, ?, ?, ?, ?);");

    sqlite3_bind_text(stmt, 1, date.c_str(), -1, SQLITE_STATIC);
    sqlite3_bind_text(stmt, 2, app.c_str(), -1, SQLITE_STATIC);
    sqlite3_bind_int(stmt, 3, hook);
    sqlite3_bind_text(stmt, 4, thread.c_str(), -1, SQLITE_STATIC);
    sqlite3_bind_text(stmt, 5, line.c_str(), -1, SQLITE_STATIC);
    sqlite3_bind_text(stmt, 6, log.c_str(), -1, SQLITE_STATIC);
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
        log["thread"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 4));
        log["line"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 5));
        log["log"] = reinterpret_cast<const char *>(sqlite3_column_text(stmt, 6));
        ret.append(log);
    }
    if (rc != SQLITE_DONE) {
        fprintf(stderr, "SQL error 5: %s\n", sqlite3_errmsg(db));
    }
    sqlite3_finalize(stmt);
    return ret;
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

int DbManager::insertBill(int id, int type, int currency, int money, int fee, int timeStamp,
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
    sqlite3_bind_int(stmt, count + 3, currency);
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
        bill["currency"] = sqlite3_column_int(stmt, 2);
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
        Json::Value bill;
        bill["id"] = sqlite3_column_int(stmt, 0);
        bill["type"] = sqlite3_column_int(stmt, 1);
        bill["currency"] = sqlite3_column_int(stmt, 2);
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

Json::Value DbManager::getBillByGroupId(int groupId) {
    Json::Value ret;
    char *zErrMsg = nullptr;
    sqlite3_stmt *stmt = getStmt("SELECT * FROM billInfo WHERE groupId = ?;");
    sqlite3_bind_int(stmt, 1, groupId);
    int rc = 0;
    while ((rc = sqlite3_step(stmt)) == SQLITE_ROW) {
        Json::Value bill;
        bill["id"] = sqlite3_column_int(stmt, 0);
        bill["type"] = sqlite3_column_int(stmt, 1);
        bill["currency"] = sqlite3_column_int(stmt, 2);
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