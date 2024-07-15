// Created by Ankio on 2024/7/11.

#ifndef AUTOACCOUNTING_RULEMODEL_H
#define AUTOACCOUNTING_RULEMODEL_H

#include <string>
#include "db/orm/Utils.h"
DEFINE_TABLE(RuleModel, "rule")
        FIELD_PK_AI(int, id)
        FIELD(std::string, app)
        FIELD(int, type)
        FIELD(std::string, js)
        FIELD(std::string, version)
END_TABLE()

#endif //AUTOACCOUNTING_RULEMODEL_H
