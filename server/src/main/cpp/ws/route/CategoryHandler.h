//
// Created by Ankio on 2024/7/15.
//

#ifndef AUTOACCOUNTING_CATEGORYHANDLER_H
#define AUTOACCOUNTING_CATEGORYHANDLER_H


#include "ws/BaseHandler.h"

class CategoryHandler: public BaseHandler{
public:
    Json::Value handle(const std::string &function, Json::Value &data) override;
};


#endif //AUTOACCOUNTING_CATEGORYHANDLER_H
