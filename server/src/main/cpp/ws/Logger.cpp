#include "Logger.h"
#include "json/value.h"
#include "db/Database.h"
#include "LogModel.h"
#include <cstdio>
#include <ctime>

void Logger::log(const std::string &msg, int level) {
    std::time_t now = std::time(nullptr);
    std::tm *ptm = std::localtime(&now);
    char buffer[32];
    std::strftime(buffer, 32, "%Y-%m-%d %H:%M:%S", ptm);
    std::string date = {buffer};

    Json::Value log;
    log["date"] = date;
    log["app"] = "server";
    log["hook"] = 0;
    log["thread"] = "main";
    log["line"] = "server";
    log["log"] = msg;
    log["level"] = level;

    if(!debug && level < LOG_LEVEL_ERROR){
        return;

    }

    Database::getInstance().insert(LogModel::getTable(), log);
    std::string level_str;
    switch (level) {
        case LOG_LEVEL_INFO:
            level_str = "INFO";
            break;
        case LOG_LEVEL_WARN:
            level_str = "WARN";
            break;
        case LOG_LEVEL_ERROR:
            level_str = "ERROR";
            break;
        case LOG_LEVEL_DEBUG:
            level_str = "DEBUG";
            break;
        default:
            level_str = "INFO";
            break;
    }
    printf("[ %s ] [ %s ] %s\n", buffer, level_str.c_str(), msg.c_str());
}

bool Logger::debug = false;
