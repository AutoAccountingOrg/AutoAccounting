// Created by Ankio on 2024/7/11.

#ifndef AUTOACCOUNTING_RULESETTINGMODEL_H
#define AUTOACCOUNTING_RULESETTINGMODEL_H

#include <string>
#include "db/orm/Utils.h"
DEFINE_TABLE(RuleSettingModel, "ruleSetting")
        FIELD_PK_AI(int, id)
        FIELD(std::string, app)
        FIELD(int, type)
        FIELD(std::string, channel)
        FIELD(int, use)
        FIELD(int, auto)
END_TABLE()

#endif //AUTOACCOUNTING_RULESETTINGMODEL_H
