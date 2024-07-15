#include "RouteManager.h"

#include <utility>
#include "route/LoginHandler.h"
#include "ws/route/AppDataHandler.h"
#include "ws/route/LogHandler.h"

RouteMap RouteManager::route_map;

void RouteManager::registerRoute(const std::string &module, std::function<HandlerPtr()> handler) {
    route_map[module] = std::move(handler);
}

HandlerPtr RouteManager::getHandler(const std::string &module) {
    auto it = route_map.find(module);
    if (it != route_map.end()) {
        return it->second();
    }
    return nullptr;
}

void RouteManager::initRoute() {
    registerRoute("login", []() -> HandlerPtr { return std::make_shared<LoginHandler>(); });
    registerRoute("data", []() -> HandlerPtr { return std::make_shared<AppDataHandler>(); });
    registerRoute("log", []() -> HandlerPtr { return std::make_shared<LogHandler>(); }
}
