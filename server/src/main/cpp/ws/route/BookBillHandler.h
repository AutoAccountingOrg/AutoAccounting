//
// Created by Ankio on 2024/7/15.
//

#ifndef AUTOACCOUNTING_BOOKBILLHANDLER_H
#define AUTOACCOUNTING_BOOKBILLHANDLER_H


#include "ws/BaseHandler.h"

class BookBillHandler : public BaseHandler{
    Json::Value handle(const std::string &function, Json::Value &data) override;
    static Json::Value get(const std::string &name);

};


#endif //AUTOACCOUNTING_BOOKBILLHANDLER_H
