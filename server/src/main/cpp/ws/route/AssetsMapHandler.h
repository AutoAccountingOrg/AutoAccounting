//
// Created by Ankio on 2024/7/15.
//

#ifndef AUTOACCOUNTING_ASSETSMAPHANDLER_H
#define AUTOACCOUNTING_ASSETSMAPHANDLER_H


#include "ws/BaseHandler.h"

class AssetsMapHandler: public BaseHandler{
public:
    Json::Value handle(const std::string &function, Json::Value &data) override;

    Json::Value list(int page, int size);

};


#endif //AUTOACCOUNTING_ASSETSMAPHANDLER_H
