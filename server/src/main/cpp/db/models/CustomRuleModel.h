// Created by Ankio on 2024/7/11.

#ifndef AUTOACCOUNTING_CUSTOMRULEMODEL_H
#define AUTOACCOUNTING_CUSTOMRULEMODEL_H

#include <string>
#include "db/orm/Utils.h"
DEFINE_TABLE(CustomRuleModel, "customRule")
        FIELD_PK_AI(int, id)
        FIELD(int, use)
        FIELD(int, sort)
        FIELD(int, auto_create)
        FIELD(std::string, js)
        FIELD(std::string, text)
        FIELD(std::string, element)
END_TABLE()
#endif //AUTOACCOUNTING_CUSTOMRULEMODEL_H
