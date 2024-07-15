// Created by Ankio on 2024/7/11.

#ifndef AUTOACCOUNTING_ASSETSMODEL_H
#define AUTOACCOUNTING_ASSETSMODEL_H

#include <string>
#include "db/orm/Utils.h"
DEFINE_TABLE(AssetsModel, "assets")
FIELD_PK_AI(int, id)
FIELD(std::string, name)
FIELD(std::string, icon)
FIELD(int, sort)
FIELD(int, type)
FIELD(std::string, extras)
END_TABLE()

#endif //AUTOACCOUNTING_ASSETSMODEL_H
