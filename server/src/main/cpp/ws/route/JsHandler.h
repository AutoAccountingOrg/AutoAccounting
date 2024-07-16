//
// Created by Ankio on 2024/7/16.
//

#ifndef AUTOACCOUNTING_JSHANDLER_H
#define AUTOACCOUNTING_JSHANDLER_H


#include "ws/BaseHandler.h"
#include "quickjspp/quickjspp.hpp"
#include <map>
#include <thread>
class JsHandler: public BaseHandler{
public:
    Json::Value handle(const std::string &function, Json::Value &data) override;
private:
    static std::string run(const std::string &js);
    Json::Value  analyze(const std::string &data,const std::string &app,int type,int call);
    static std::map<std::thread::id, std::string> resultMap;
    static std::mutex resultMapMutex;
    static void print(qjs::rest<std::string> args);
};


#endif //AUTOACCOUNTING_JSHANDLER_H
