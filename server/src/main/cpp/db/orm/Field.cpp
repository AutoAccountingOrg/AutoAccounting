#include "Field.h"

#include <utility>

Field::Field(std::string name, FieldType type, bool isPrimaryKey, bool isAutoIncrement)
        : name(std::move(name)), type(type), isPrimaryKey(isPrimaryKey), isAutoIncrement(isAutoIncrement) {}
