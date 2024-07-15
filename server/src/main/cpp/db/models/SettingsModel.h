// Created by Ankio on 2024/7/11.

#ifndef AUTOACCOUNTING_SETTINGSMODEL_H
#define AUTOACCOUNTING_SETTINGSMODEL_H

#include <string>
#include "db/orm/Utils.h"
DEFINE_TABLE(SettingsModel, "settings")
        FIELD_PK_AI(int, id)
        FIELD(std::string, app)
        FIELD(std::string, key)
        FIELD(std::string, val)
END_TABLE()

#endif //AUTOACCOUNTING_SETTINGSMODEL_H
