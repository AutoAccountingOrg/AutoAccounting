//
// Created by Ankio on 2024/7/15.
//

#ifndef AUTOACCOUNTING_SETTINGHANDLER_H
#define AUTOACCOUNTING_SETTINGHANDLER_H


#include "ws/BaseHandler.h"

class SettingHandler: public BaseHandler{
public:
    Json::Value handle(const std::string &function, Json::Value &data) override;
    static Json::Value get(const std::string &app, const std::string &key);
};


#endif //AUTOACCOUNTING_SETTINGHANDLER_H
