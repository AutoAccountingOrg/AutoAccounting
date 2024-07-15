#ifndef TABLE_H
#define TABLE_H

#include <string>
#include <vector>
#include "Field.h"

struct Table {
    std::string name;
    std::vector<Field> fields;

    Table(std::string name, std::vector<Field> fields);
};

#endif // TABLE_H
