//
// Created by Ankio on 2024/7/15.
//

#ifndef AUTOACCOUNTING_CUSTOMRULEHANDLER_H
#define AUTOACCOUNTING_CUSTOMRULEHANDLER_H


#include "ws/BaseHandler.h"

class CustomRuleHandler: public BaseHandler{
    public:
    Json::Value handle(const std::string &function, Json::Value &data) override;
    };


#endif //AUTOACCOUNTING_CUSTOMRULEHANDLER_H
