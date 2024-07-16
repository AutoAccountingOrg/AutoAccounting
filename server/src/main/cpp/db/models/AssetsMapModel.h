// Created by Ankio on 2024/7/11.

#ifndef AUTOACCOUNTING_ASSETSMAPMODEL_H
#define AUTOACCOUNTING_ASSETSMAPMODEL_H

#include <string>
#include "db/orm/Utils.h"

DEFINE_TABLE(AssetsMapModel, "assetsMap")
        FIELD_PK_AI(int, id)
        FIELD(int, regex)
        FIELD(std::string, name)
        FIELD(std::string, mapName)
END_TABLE()
#endif //AUTOACCOUNTING_ASSETSMAPMODEL_H
