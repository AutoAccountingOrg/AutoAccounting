#include "Table.h"

#include <utility>

Table::Table(std::string name, std::vector<Field> fields)
        : name(std::move(name)), fields(std::move(fields)) {}
