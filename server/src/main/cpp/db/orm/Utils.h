#ifndef UTILS_H
#define UTILS_H

#include <string>
#include <vector>
#include <sstream>
#include "Field.h"
#include "Table.h"
#include "../DbManager.h"
#include <json/json.h>

// 辅助函数：确定字段类型
template <typename T>
FieldType GetFieldType();

template <>
FieldType GetFieldType<int>();

template <>
FieldType GetFieldType<long>();

template <>
FieldType GetFieldType<double>();

template <>
FieldType GetFieldType<std::string>();

// 宏：声明字段
#define FIELD(type, name) \
    fields.push_back(Field(#name, GetFieldType<type>()));

// 宏：声明主键字段
#define FIELD_PK_AI(type, name) \
    fields.push_back(Field(#name, GetFieldType<type>(), true, true));

// 宏：定义表
#define DEFINE_TABLE(struct_name, table_name) \
    struct struct_name { \
        static std::vector<Field> fields; \
        static Table getTable() { \
            if (fields.empty()) { \
                initFields(); \
            } \
            return Table(table_name, fields); \
        } \
        static void initFields() { \
            fields.clear();

// 宏：结束表定义
#define END_TABLE() \
        } \
    };


// 生成建表语句的函数
std::string generateCreateTableSQL(const Table& table);

// 生成插入语句的函数
std::string generateInsertSQL(const Table& table, const Json::Value& json);

// 生成更新语句的函数
std::string generateUpdateSQL(const Table& table, const Json::Value& json, int id);

// 生成删除语句的函数
std::string generateDeleteSQL(const Table& table, int id);

// 生成查询语句的函数
std::string generateSelectSQL(const Table& table, int id);

// 生成条件查询语句的函数
std::string generateConditionalSelectSQL(const Table& table, const std::string& condition);

#endif // UTILS_H
