// Created by Ankio on 2024/7/11.

#ifndef AUTOACCOUNTING_BILLINFOMODEL_H
#define AUTOACCOUNTING_BILLINFOMODEL_H

#include <string>
#include "db/orm/Utils.h"
DEFINE_TABLE(BillInfoModel, "billInfo")
FIELD_PK_AI(int, id)
        FIELD(int, type)
        FIELD(std::string, currency)
        FIELD(double, money)
        FIELD(double, fee)
        FIELD(long, time)
        FIELD(std::string, shopName)
        FIELD(std::string, shopItem)
        FIELD(std::string, cateName)
        FIELD(std::string, extendData)
        FIELD(std::string, bookName)
        FIELD(std::string, accountNameFrom)
        FIELD(std::string, accountNameTo)
        FIELD(std::string, fromApp)
        FIELD(int, groupId)
        FIELD(std::string, channel)
        FIELD(int, syncFromApp)
        FIELD(std::string, remark)
        FIELD(int, auto)
END_TABLE()

#endif //AUTOACCOUNTING_BILLINFOMODEL_H
