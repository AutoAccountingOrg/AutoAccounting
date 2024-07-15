#ifndef LOGGER_H
#define LOGGER_H

#include <string>

#define LOG_LEVEL_INFO 1
#define LOG_LEVEL_WARN 2
#define LOG_LEVEL_ERROR 3
#define LOG_LEVEL_DEBUG 4

class Logger {
public:
    static void log(const std::string &msg, int level);
};

#endif // LOGGER_H
