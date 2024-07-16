//
// Created by Ankio on 2024/7/16.
//

#include "JsHandler.h"
#include "ws/Logger.h"
#include "db/Database.h"
#include "SettingsModel.h"
#include "SettingHandler.h"
#include "AppDataHandler.h"
#include "AppDataModel.h"
#include "common.h"
#include "RuleHandler.h"
#include "BillHandler.h"

Json::Value JsHandler::handle(const std::string &function, Json::Value &data) {
    if (function == "analyze") {
        return analyze(data["data"].asString(), data["app"].asString(), data["type"].asInt(), data["call"].asInt());
    } else if (function == "run") {
        return {run(data["js"].asString())};
    }
    return {};
}

Json::Value
JsHandler::analyze(const std::string &data, const std::string &app, int type, int call) {
    int dataId = 0;
    Json::Value appData(Json::ValueType::objectValue);
    int time = std::time(nullptr);
    // 直接存入AppData
    if (call == 1) {
        appData["data"] = data;
        appData["source"] = app;
        appData["time"] = time;
        appData["type"] = type;
        appData["match"] = 0;
        appData["rule"] = "";
        appData["issue"] = 0;
        // = 1 是来自其他应用，而非用户主动发起，所以无论是否成功，均需要存入AppData
        dataId = AppDataHandler::add(appData);
        appData["id"] = dataId;
    }
    Json::Value ret(Json::ValueType::objectValue);
    // 根据app_type取js，编译优化的时候应该自动生成这样的值
    std::string key = app + std::to_string(type) + "_rule";
    std::string js = SettingHandler::get("server", key);
    if (js.empty()) {
        Logger::log("Js not found, please rebuild js.", LOG_LEVEL_ERROR);
        return ret;
    }

    // 拼接脚本
    std::string billJs = R"(
        let window = {};
        window.data = JSON.parse(')" + data + R"(');
    )";

    billJs += js;

    billJs += R"(
    const data = window.data || '';

const rules = window.rules || [];

for (const rule of rules) {
  let result = null;
  try {
    result = rule.obj.get(data);
    if (
      result !== null &&
      result.money !== null &&
      parseFloat(result.money) > 0
    ) {
      result.ruleName = rule.name;
      print(JSON.stringify(result));
      break;
    }
  } catch (e) {
    print(e.message);
  }
}
)";


    std::string result = run(billJs);

    Logger::log("Js result: " + result, LOG_LEVEL_INFO);

    Json::Value _json;
    Json::Reader _reader;
    if (!_reader.parse(result, _json)) {
        Logger::log("json parse error", LOG_LEVEL_ERROR);
        ret["data"] = "json parse error";
        return ret;
    }


    float money = _json["money"].asFloat();
    int bill_type = _json["type"].asInt();

    std::string shopName = replaceSubstring(_json["shopName"].asString(), "'", "\"");
    std::string shopItem = replaceSubstring(_json["shopItem"].asString(), "'", "\"");
    std::time_t now = std::time(nullptr);
    //time时间戳格式化为：HH:mm
    char buffer[32];
    std::tm *ptm = std::localtime(&now);
    std::strftime(buffer, 32, "%H:%M", ptm);
    std::string timeStr = buffer;

    std::string channel = _json["channel"].asString();
    Logger::log("channel: " + channel, LOG_LEVEL_INFO);

    std::string ruleName = channel;
    //如果ruleName含有-，截取  - 前面
    if (channel.find('-') != std::string::npos) {
        ruleName = channel.substr(0, channel.find('-'));
    }
    trim(ruleName);
    //规则使用的名称和渠道名称不对应
    Json::Value rule = RuleHandler::get(ruleName);

    if (call == 1 && dataId > 0) {
        appData["match"] = 1;
        appData["rule"] = channel;

        Database::getInstance().update(AppDataModel::getTable(), appData, dataId);
    }
    std::string cate_js = SettingHandler::get("server", "cate_js");
    if (cate_js.empty()) {
        Logger::log("Cate Js not found, please download js.", LOG_LEVEL_ERROR);
        return ret;
    }
    std::string customJs = SettingHandler::get("server", "custom_js");

    std::string categoryJs =
            "var window = {money:" + std::to_string(money) + ", type:" +
            std::to_string(bill_type) + ", shopName:'" + shopName +
            "', shopItem:'" + shopItem + "', time:'" + timeStr + "'};\n" +
            "function getCategory(money,type,shopName,shopItem,time){ " + customJs +
            " return null};\n" +
            "var categoryInfo = getCategory(window.money,window.type,window.shopName,window.shopItem,window.time);" +
            "if(categoryInfo !== null) { print(JSON.stringify(categoryInfo));  } else { " +
            cate_js + " "
                      "print(JSON.stringify(category.get(money, type, shopName, shopItem, time)));}";


    std::string categoryResult = run(categoryJs);
    Json::Value categoryJson;
    if (!_reader.parse(categoryResult, categoryJson)) {
        Logger::log("json parse error", LOG_LEVEL_ERROR);
        ret["data"] = "json parse error";
        return ret;
    }

    Logger::log("category result: " + categoryJson.toStyledString(), LOG_LEVEL_INFO);
    std::string bookName = categoryJson["book"].asString();
    std::string cateName = categoryJson["category"].asString();

    _json["bookName"] = bookName;
    _json["cateName"] = cateName;
    _json["time"] = time;
    _json["fromApp"] = app;
    _json["auto"] = 0;
    if (!rule.empty()) {
        _json["auto"] = rule["auto_record"].asInt();

    }

    Logger::log("自动记账识别结果：" + _json.toStyledString(), LOG_LEVEL_INFO);
    //拉起自动记账app
    if (call == 1) {
        try {

            int id = BillHandler::add(_json);


            std::string cmd =
                    R"(am start -a "net.ankio.auto.ACTION_SHOW_FLOATING_WINDOW" -d "autoaccounting://bill?id=)" +
                    std::to_string(id) +
                    R"(" --ez "android.intent.extra.NO_ANIMATION" true -f 0x10000000)";
            //写日志
            Logger::log("执行命令 " + cmd, LOG_LEVEL_INFO);
            system(cmd.c_str());
        } catch (const std::exception &e) {
            Logger::log("拉起自动记账失败：" + std::string(e.what()), LOG_LEVEL_ERROR);
        }

    }

    ret["data"] = _json;


    return ret;
}

std::string JsHandler::run(const std::string &js) {
    qjs::Runtime runtime;
    qjs::Context context(runtime);
    std::thread::id id = std::thread::id();
    try {
        auto &module = context.addModule("MyModule");
        module.function<&JsHandler::print>("print");
        context.eval(R"xxx(
             import { print } from 'MyModule';
            globalThis.print = print;
        )xxx", "<import>", JS_EVAL_TYPE_MODULE);

        context.eval(js);
        std::lock_guard<std::mutex> lock(resultMapMutex);
        std::string data = resultMap[id];
        resultMap.erase(id);
        return data;
    }
    catch (qjs::exception &e) {
        auto exc = context.getException();
        Logger::log("Js Error: " + (std::string) exc, LOG_LEVEL_ERROR);
        if ((bool) exc["stack"])
            Logger::log("Js Error: " + (std::string) exc["stack"], LOG_LEVEL_ERROR);
    }
    return "";
}

void JsHandler::print(qjs::rest<std::string> args) {
    std::string str;
    for (auto &arg: args) {
        str += arg;
    }
    std::lock_guard<std::mutex> lock(resultMapMutex);
    resultMap[std::this_thread::get_id()] = str;
}

std::map<std::thread::id, std::string> JsHandler::resultMap;
std::mutex JsHandler::resultMapMutex;