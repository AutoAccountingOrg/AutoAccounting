// Created by Ankio on 2024/7/11.

#ifndef AUTOACCOUNTING_BOOKNAMEMODEL_H
#define AUTOACCOUNTING_BOOKNAMEMODEL_H

#include <string>
#include "db/orm/Utils.h"
DEFINE_TABLE(BookNameModel, "bookName")
        FIELD_PK_AI(int, id)
        FIELD(std::string, name)
        FIELD(std::string, icon)
END_TABLE()
#endif //AUTOACCOUNTING_BOOKNAMEMODEL_H
