#ifndef ROUTEMANAGER_H
#define ROUTEMANAGER_H

#include <string>
#include <unordered_map>
#include <functional>
#include <memory>
#include "BaseHandler.h"

typedef std::shared_ptr<BaseHandler> HandlerPtr;
typedef std::unordered_map<std::string, std::function<HandlerPtr()>> RouteMap;

class RouteManager {
public:
    static void registerRoute(const std::string &module, std::function<HandlerPtr()> handler);
    static HandlerPtr getHandler(const std::string &module);
    static void initRoute();

private:
    static RouteMap route_map;
};

#endif // ROUTEMANAGER_H
