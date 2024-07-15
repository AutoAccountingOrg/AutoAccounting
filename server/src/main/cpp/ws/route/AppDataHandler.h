//
// Created by Ankio on 2024/7/13.
//

#ifndef AUTOACCOUNTING_APPDATAHANDLER_H
#define AUTOACCOUNTING_APPDATAHANDLER_H


#include "ws/BaseHandler.h"

class AppDataHandler : public BaseHandler{
    Json::Value handle(const std::string &function, Json::Value &data) override;

    static Json::Value list(int page,int size,const std::string& data="",int match = -1);

};


#endif //AUTOACCOUNTING_APPDATAHANDLER_H
