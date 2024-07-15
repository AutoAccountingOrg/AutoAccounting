#ifndef BASE_HANDLER_H
#define BASE_HANDLER_H

#include <json/json.h>

class BaseHandler {
public:
    virtual Json::Value handle(const std::string &function, Json::Value& data) = 0;

};

#endif
