#include "Utils.h"

template <>
FieldType GetFieldType<int>() { return INTEGER; }

template <>
FieldType GetFieldType<long>() { return LONG; }

template <>
FieldType GetFieldType<double>() { return REAL; }

template <>
FieldType GetFieldType<std::string>() { return TEXT; }

std::string generateCreateTableSQL(const Table& table) {
    std::stringstream sql;
    sql << "CREATE TABLE IF NOT EXISTS " << table.name << " (";

    bool first = true;
    for (const auto& field : table.fields) {
        if (!first) {
            sql << ", ";
        }
        first = false;

        sql << field.name << " ";
        switch (field.type) {
            case INTEGER:
                sql << "INTEGER";
                break;
            case LONG:
                sql << "BIGINT";
                break;
            case REAL:
                sql << "REAL";
                break;
            case TEXT:
                sql << "TEXT";
                break;
        }

        if (field.isPrimaryKey) {
            sql << " PRIMARY KEY";
        }
        if (field.isAutoIncrement) {
            sql << " AUTOINCREMENT";
        }
    }

    sql << ");";
    return sql.str();
}

std::string generateInsertSQL(const Table& table, const Json::Value& json) {
    std::stringstream sql;
    sql << "INSERT INTO " << table.name << " (";
    bool first = true;
    for (const auto& field : table.fields) {
        if (field.isPrimaryKey && field.isAutoIncrement) {
            continue;
        }
        if (!first) {
            sql << ", ";
        }
        first = false;
        sql << field.name;
    }
    sql << ") VALUES (";
    first = true;
    for (const auto& field : table.fields) {
        if (field.isPrimaryKey && field.isAutoIncrement) {
            continue;
        }
        if (!first) {
            sql << ", ";
        }
        first = false;
        if (field.type == TEXT) {
            sql << "'" << json[field.name].asString() << "'";
        } else {
            sql << json[field.name];
        }
    }
    sql << ");";
    return sql.str();
}

std::string generateUpdateSQL(const Table& table, const Json::Value& json, int id) {
    std::stringstream sql;
    sql << "UPDATE " << table.name << " SET ";
    bool first = true;
    for (const auto& field : table.fields) {
        if (field.isPrimaryKey) {
            continue;
        }
        if (!first) {
            sql << ", ";
        }
        first = false;
        sql << field.name << " = ";
        if (field.type == TEXT) {
            sql << "'" << json[field.name].asString() << "'";
        } else {
            sql << json[field.name];
        }
    }
    sql << " WHERE id = " << id << ";";
    return sql.str();
}

std::string generateDeleteSQL(const Table& table, int id) {
    std::stringstream sql;
    sql << "DELETE FROM " << table.name << " WHERE id = " << id << ";";
    return sql.str();
}

std::string generateSelectSQL(const Table& table, int id) {
    std::stringstream sql;
    sql << "SELECT * FROM " << table.name << " WHERE id = " << id << ";";
    return sql.str();
}

std::string generateConditionalSelectSQL(const Table& table, const std::string& condition) {
    std::stringstream sql;
    sql << "SELECT * FROM " << table.name;
    if (!condition.empty()) {
        sql << " WHERE " << condition;
    }
    sql << ";";
    return sql.str();
}
