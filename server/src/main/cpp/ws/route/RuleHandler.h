//
// Created by Ankio on 2024/7/15.
//

#ifndef AUTOACCOUNTING_RULEHANDLER_H
#define AUTOACCOUNTING_RULEHANDLER_H


#include "ws/BaseHandler.h"

class RuleHandler : public  BaseHandler{
    Json::Value handle(const std::string &function, Json::Value &data) override;
    static Json::Value get(const std::string &name);
};


#endif //AUTOACCOUNTING_RULEHANDLER_H
