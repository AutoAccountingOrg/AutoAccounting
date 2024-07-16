// Created by Ankio on 2024/7/11.

#ifndef AUTOACCOUNTING_LOGMODEL_H
#define AUTOACCOUNTING_LOGMODEL_H

#include <string>
#include "db/orm/Utils.h"
DEFINE_TABLE(LogModel, "log")
        FIELD_PK_AI(int, id)
        FIELD(std::string, date)
        FIELD(std::string, app)
        FIELD(int, hook)
        FIELD(int, level)
        FIELD(std::string, thread)
        FIELD(std::string, line)
        FIELD(std::string, log)
END_TABLE()
#endif //AUTOACCOUNTING_LOGMODEL_H
