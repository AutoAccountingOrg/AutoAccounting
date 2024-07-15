// Created by Ankio on 2024/7/11.

#ifndef AUTOACCOUNTING_CATEGORYMODEL_H
#define AUTOACCOUNTING_CATEGORYMODEL_H

#include <string>
#include "db/orm/Utils.h"
DEFINE_TABLE(CategoryModel, "category")
        FIELD_PK_AI(int, id)
        FIELD(std::string, name)
        FIELD(std::string, icon)
        FIELD(std::string, remoteId)
        FIELD(int, parent)
        FIELD(int, book)
        FIELD(int, sort)
        FIELD(int, type)
END_TABLE()

#endif //AUTOACCOUNTING_CATEGORYMODEL_H
