#ifndef LOGGER_H
#define LOGGER_H

#include <string>

#define LOG_LEVEL_INFO 2
#define LOG_LEVEL_WARN 3
#define LOG_LEVEL_ERROR 4
#define LOG_LEVEL_DEBUG 1

class Logger {
public:
    static void log(const std::string &msg, int level);
    static bool debug;
};

#endif // LOGGER_H
