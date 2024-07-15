#ifndef FIELD_H
#define FIELD_H

#include <string>
#include "FieldType.h"

struct Field {
    std::string name;
    FieldType type;
    bool isPrimaryKey;
    bool isAutoIncrement;

    Field(std::string name, FieldType type, bool isPrimaryKey = false, bool isAutoIncrement = false);
};

#endif // FIELD_H
