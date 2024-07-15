#ifndef TABLE_H
#define TABLE_H

#include <string>
#include <vector>
#include "Field.h"

// Table 类的定义
struct Table {
    std::string name;
    std::vector<Field> fields;
    // 构造函数的声明
    Table(std::string name, std::vector<Field> fields);
};

#endif // TABLE_H
