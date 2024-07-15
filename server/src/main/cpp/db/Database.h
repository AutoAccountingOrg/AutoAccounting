#ifndef DATABASE_H
#define DATABASE_H

#include <string>
#include <vector>
#include <sqlite/sqlite3.h>
#include <json/json.h>
#include <memory>

class Table {
public:
    std::string name;
    struct Field {
        std::string name;
        bool isPrimaryKey;
        bool isAutoIncrement;
    };
    std::vector<Field> fields;
};

class Database {
public:
    static Database& getInstance(const std::string& dbPath = "auto_v2.db");
    ~Database();

    bool executeSQL(const std::string& sql);
    Json::Value querySQL(const std::string& sql);

    bool insert(const Table& table, const Json::Value& json);
    bool update(const Table& table, const Json::Value& json, int id);
    bool remove(const Table& table, int id);
    Json::Value select(const Table& table, int id);
    Json::Value selectConditional(const Table& table, const std::string& condition, const std::vector<Json::Value>& parameters);
    Json::Value page(const Table &table, int page, int size, const std::string &condition, const std::vector<Json::Value> &parameters);

private:
    explicit Database(const std::string& dbPath);
    void initializeTables();

    bool bindValue(sqlite3_stmt* stmt, int index, const Json::Value& value);
    bool prepareStatement(sqlite3_stmt** stmt, const std::string& sql, const std::vector<Json::Value>& parameters);
    Json::Value queryWithParams(const std::string& sql, const std::vector<Json::Value>& parameters);

    static std::unique_ptr<Database> instance;
    sqlite3* db;
};

#endif // DATABASE_H
