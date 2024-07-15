//
// Created by Ankio on 2024/7/15.
//

#ifndef AUTOACCOUNTING_LOGHANDLER_H
#define AUTOACCOUNTING_LOGHANDLER_H


#include "ws/BaseHandler.h"

class LogHandler: public BaseHandler {
public:
    Json::Value handle(const std::string &function, Json::Value &data) override;

    static Json::Value list(int page, int size);

};


#endif //AUTOACCOUNTING_LOGHANDLER_H
