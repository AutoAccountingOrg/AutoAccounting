//
// Created by Ankio on 2024/7/15.
//

#ifndef AUTOACCOUNTING_ASSETHANDLER_H
#define AUTOACCOUNTING_ASSETHANDLER_H


#include "ws/BaseHandler.h"

class AssetHandler: public BaseHandler{
public:
    Json::Value handle(const std::string &function, Json::Value &data) override;

    Json::Value list(int page, int size);
};


#endif //AUTOACCOUNTING_ASSETHANDLER_H
