//
// Created by Ankio on 2024/7/11.
//

#ifndef AUTOACCOUNTING_APPDATAMODEL_H
#define AUTOACCOUNTING_APPDATAMODEL_H

#include <string>
#include "db/orm/Utils.h"
DEFINE_TABLE(AppDataModel, "appData")
        FIELD_PK_AI(int, id)
        FIELD(std::string, data)
        FIELD(std::string, source)
        FIELD(long, time)
        FIELD(int, match)
        FIELD(std::string, rule)
        FIELD(int, issue)
        FIELD(int, type)
END_TABLE()

#endif //AUTOACCOUNTING_APPDATAMODEL_H
