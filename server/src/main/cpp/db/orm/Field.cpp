#include "Field.h"

Field::Field(std::string name, FieldType type, bool isPrimaryKey, bool isAutoIncrement)
        : name(name), type(type), isPrimaryKey(isPrimaryKey), isAutoIncrement(isAutoIncrement) {}
