
#ifndef AUTO_HANDLER_H
#define AUTO_HANDLER_H

#include <cerrno>
#include <unistd.h>
#include <string>
#include <iostream>
#include <unordered_map>
#include "js/quickjspp.hpp"

#define CRLF "\r\n"
#define CRLF_2 "\r\n\r\n"

class Handler {

public:
    explicit Handler(int socket);

    void handleConnection();

private:
    int socket = 0;

    static std::string getHeader(const std::string &header, const std::string &request);

    // 获取请求体长度
    static ssize_t getContentLength(const std::string &request);

    std::string parseRequest(const std::string &header, const std::string &body);

    static std::string httpResponse(const std::string &status, const std::string &responseBody);

    std::string handleRoute(const std::string &path,
                            const std::string &requestBody,
                            const std::string &authHeader,
                            const std::unordered_map<std::string,
                                    std::string> &queryParams
    );

    // 解析查询字符串
    static std::unordered_map<std::string, std::string> parseQuery(const std::string &query);

    static std::string js(const std::string &js);

    static std::string rule(const std::string &data);

    static std::string category(const std::string &data);

    void static println(qjs::rest<std::string> args);

    thread_local static std::string result;
};


#endif //AUTO_HANDLER_H
