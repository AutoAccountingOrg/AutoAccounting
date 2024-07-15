//
// Created by Ankio on 2024/7/13.
//

#ifndef AUTOACCOUNTING_LOGINHANDLER_H
#define AUTOACCOUNTING_LOGINHANDLER_H
#include "ws/BaseHandler.h"
class LoginHandler : public BaseHandler {
public:
    Json::Value handle(const std::string &function, Json::Value& data) override;

};
#endif //AUTOACCOUNTING_LOGINHANDLER_H
