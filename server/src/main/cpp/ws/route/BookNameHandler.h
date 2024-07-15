//
// Created by Ankio on 2024/7/15.
//

#ifndef AUTOACCOUNTING_BOOKNAMEHANDLER_H
#define AUTOACCOUNTING_BOOKNAMEHANDLER_H


#include "ws/BaseHandler.h"

class BookNameHandler: public BaseHandler{
public:
    Json::Value handle(const std::string &function, Json::Value &data) override;

};


#endif //AUTOACCOUNTING_BOOKNAMEHANDLER_H
