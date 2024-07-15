#include "Database.h"
#include "db/orm/Utils.h"
#include "AppDataModel.h"
#include "AssetsMapModel.h"
#include "AssetsModel.h"
#include "AuthModel.h"
#include "BillInfoModel.h"
#include "RuleSettingModel.h"
#include "LogModel.h"
#include "RuleModel.h"
#include "CategoryModel.h"
#include "BookBillModel.h"
#include "CustomRuleModel.h"
#include "SettingsModel.h"
#include <iostream>
#include <memory>

std::unique_ptr<Database> Database::instance = nullptr;

Database::Database(const std::string& dbPath) {
    if (sqlite3_open(dbPath.c_str(), &db) != SQLITE_OK) {
        std::cerr << "Can't open database: " << sqlite3_errmsg(db) << std::endl;
        db = nullptr;
    } else {
        initializeTables();
    }
}

Database::~Database() {
    if (db) {
        sqlite3_close(db);
    }
}

Database& Database::getInstance(const std::string& dbPath) {
    if (!instance) {
        instance = std::unique_ptr<Database>(new Database(dbPath));
    }
    return *instance;
}

void Database::initializeTables() {
    std::vector<Table> tables = {
            AppDataModel::getTable(),
            AssetsMapModel::getTable(),
            AssetsModel::getTable(),
            AuthModel::getTable(),
            BillInfoModel::getTable(),
            BookBillModel::getTable(),
            CategoryModel::getTable(),
            CustomRuleModel::getTable(),
            LogModel::getTable(),
            RuleModel::getTable(),
            RuleSettingModel::getTable(),
            SettingsModel::getTable()
    };

    for (const auto& table : tables) {
        std::string createSQL = generateCreateTableSQL(table);
        executeSQL(createSQL);
    }
}

Json::Value Database::executeSQL(const std::string &sql, const std::vector<Json::Value> &parameters,
                                 bool readonly) {
    Json::Value result(Json::arrayValue);
    sqlite3_stmt *stmt;

    if (!prepareStatement(&stmt, sql, parameters)) {
        return result;
    }

    if (readonly) {

        int cols = sqlite3_column_count(stmt);

        while (sqlite3_step(stmt) == SQLITE_ROW) {
            Json::Value row(Json::objectValue);
            for (int i = 0; i < cols; i++) {
                std::string colName = sqlite3_column_name(stmt, i);
                switch (sqlite3_column_type(stmt, i)) {
                    case SQLITE_INTEGER:
                        row[colName] = sqlite3_column_int(stmt, i);
                        break;
                    case SQLITE_FLOAT:
                        row[colName] = sqlite3_column_double(stmt, i);
                        break;
                    case SQLITE_TEXT:
                        row[colName] = (const char *) sqlite3_column_text(stmt, i);
                        break;
                    case SQLITE_NULL:
                        row[colName] = Json::nullValue;
                        break;
                    default:
                        break;
                }
            }
            result.append(row);
        }
    } else {
        bool success = sqlite3_step(stmt) == SQLITE_DONE;
        if (!success) {
            std::cerr << "SQL error: " << sqlite3_errmsg(db) << std::endl;
        }
    }
    sqlite3_finalize(stmt);
    return result;
}


bool Database::bindValue(sqlite3_stmt* stmt, int index, const Json::Value& value) {
    if (value.isNull()) {
        return sqlite3_bind_null(stmt, index) == SQLITE_OK;
    } else if (value.isInt()) {
        return sqlite3_bind_int(stmt, index, value.asInt()) == SQLITE_OK;
    } else if (value.isUInt()) {
        return sqlite3_bind_int64(stmt, index, value.asUInt()) == SQLITE_OK;
    } else if (value.isDouble()) {
        return sqlite3_bind_double(stmt, index, value.asDouble()) == SQLITE_OK;
    } else if (value.isString()) {
        return sqlite3_bind_text(stmt, index, value.asCString(), -1, SQLITE_TRANSIENT) == SQLITE_OK;
    }
    return false;
}

bool Database::prepareStatement(sqlite3_stmt** stmt, const std::string& sql, const std::vector<Json::Value>& parameters) {
    if (sqlite3_prepare_v2(db, sql.c_str(), -1, stmt, nullptr) != SQLITE_OK) {
        std::cerr << "Failed to prepare statement: " << sqlite3_errmsg(db) << std::endl;
        return false;
    }
    for (int i = 0; i < parameters.size(); ++i) {
        if (!bindValue(*stmt, i + 1, parameters[i])) {
            std::cerr << "Failed to bind parameter at index " << i + 1 << std::endl;
            return false;
        }
    }
    return true;
}

bool Database::insert(const Table& table, const Json::Value& json) {
    std::string sql = "INSERT INTO " + table.name + " (";
    std::string values = "VALUES (";
    std::vector<Json::Value> parameters;

    bool first = true;
    for (const auto& field : table.fields) {
        if (field.isPrimaryKey && field.isAutoIncrement) {
            continue;
        }
        if (!first) {
            sql += ", ";
            values += ", ";
        }
        first = false;
        sql += field.name;
        values += "?";
        parameters.push_back(json.get(field.name, Json::nullValue));
    }

    sql += ") " + values + ");";

    sqlite3_stmt* stmt;
    if (!prepareStatement(&stmt, sql, parameters)) {
        return false;
    }

    bool success = sqlite3_step(stmt) == SQLITE_DONE;
    if (!success) {
        std::cerr << "Insert failed: " << sqlite3_errmsg(db) << std::endl;
    }
    sqlite3_finalize(stmt);
    return success;
}

bool Database::update(const Table& table, const Json::Value& json, int id) {
    std::string sql = "UPDATE " + table.name + " SET ";
    std::vector<Json::Value> parameters;

    bool first = true;
    for (const auto& field : table.fields) {
        if (field.isPrimaryKey) {
            continue;
        }
        if (!first) {
            sql += ", ";
        }
        first = false;
        sql += field.name + " = ?";
        parameters.push_back(json.get(field.name, Json::nullValue));
    }
    sql += " WHERE id = ?;";
    parameters.emplace_back(id);

    sqlite3_stmt* stmt;
    if (!prepareStatement(&stmt, sql, parameters)) {
        return false;
    }

    bool success = sqlite3_step(stmt) == SQLITE_DONE;
    if (!success) {
        std::cerr << "Update failed: " << sqlite3_errmsg(db) << std::endl;
    }
    sqlite3_finalize(stmt);
    return success;
}

bool Database::remove(const Table& table, int id) {
    std::string sql = "DELETE FROM " + table.name + " WHERE id = ?;";
    sqlite3_stmt* stmt;

    if (!prepareStatement(&stmt, sql, {Json::Value(id)})) {
        return false;
    }

    bool success = sqlite3_step(stmt) == SQLITE_DONE;
    if (!success) {
        std::cerr << "Delete failed: " << sqlite3_errmsg(db) << std::endl;
    }
    sqlite3_finalize(stmt);
    return success;
}

Json::Value Database::select(const Table& table, int id) {
    std::string sql = "SELECT * FROM " + table.name + " WHERE id = ?;";
    return executeSQL(sql, {Json::Value(id)}, true);
}

Json::Value Database::selectConditional(const Table& table, const std::string& condition, const std::vector<Json::Value>& parameters) {
    std::string sql = "SELECT * FROM " + table.name;
    if (!condition.empty()) {
        sql += " WHERE " + condition;
    }
    sql += ";";
    return executeSQL(sql, parameters,true);
}


Json::Value Database::page(const Table &table, int page, int size, const std::string &condition,
                           const std::vector<Json::Value> &parameters, const std::string &orderBy) {
    std::string sql = "SELECT * FROM " + table.name;
    if (!condition.empty()) {
        sql += " WHERE " + condition;
    }
    sql += " ORDER BY " + orderBy;
    if(size > 0){
        sql += " LIMIT " + std::to_string((page - 1) * size) + ", " + std::to_string(size) ;
    }
    sql += ";";
    return executeSQL(sql, parameters, true);
}
