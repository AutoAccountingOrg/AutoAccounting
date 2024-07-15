#include "RouteManager.h"

#include <utility>
#include "route/LoginHandler.h"
#include "ws/route/AppDataHandler.h"
#include "ws/route/LogHandler.h"
#include "ws/route/BillHandler.h"
#include "ws/route/AssetHandler.h"
#include "ws/route/AssetsMapHandler.h"
#include "ws/route/BookNameHandler.h"
#include "ws/route/SettingHandler.h"
#include "ws/route/CategoryHandler.h"
#include "ws/route/RuleHandler.h"
#include "ws/route/BookBillHandler.h"
#include "ws/route/CustomRuleHandler.h"

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
    registerRoute("log", []() -> HandlerPtr { return std::make_shared<LogHandler>(); });
    registerRoute("bill", []() -> HandlerPtr { return std::make_shared<BillHandler>(); });
    registerRoute("assets",[]() -> HandlerPtr { return std::make_shared<AssetHandler>(); });
    registerRoute("assets_map",[]() -> HandlerPtr { return std::make_shared<AssetsMapHandler>(); });
    registerRoute("category",[]() -> HandlerPtr { return std::make_shared<CategoryHandler>(); });
    registerRoute("book_name",[]() -> HandlerPtr { return std::make_shared<BookNameHandler>(); });
    registerRoute("setting",[]() -> HandlerPtr { return std::make_shared<SettingHandler>(); });
    registerRoute("custom",[]() -> HandlerPtr { return std::make_shared<CustomRuleHandler>(); });
    registerRoute("rule",[]() -> HandlerPtr { return std::make_shared<RuleHandler>(); });
    registerRoute("book_bill",[]() -> HandlerPtr { return std::make_shared<BookBillHandler>(); });

}
