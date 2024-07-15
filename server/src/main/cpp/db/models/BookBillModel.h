// Created by Ankio on 2024/7/11.

#ifndef AUTOACCOUNTING_BOOKBILLMODEL_H
#define AUTOACCOUNTING_BOOKBILLMODEL_H

#include <string>
#include "db/orm/Utils.h"
DEFINE_TABLE(BookBillModel, "bookBill")
        FIELD_PK_AI(int, id)
        FIELD(double, amount)
        FIELD(int, time)
        FIELD(std::string, remark)
        FIELD(std::string, billId)
        FIELD(int, type)
        FIELD(std::string, book)
        FIELD(std::string, category)
        FIELD(std::string, accountFrom)
        FIELD(std::string, accountTo)
END_TABLE()

#endif //AUTOACCOUNTING_BOOKBILLMODEL_H
