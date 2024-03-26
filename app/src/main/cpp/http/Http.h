//
// Created by 徐贵江 on 2024/3/26.
//

#ifndef AUTO_HTTP_H
#define AUTO_HTTP_H


#include <unordered_map>
#include "../file/File.h"

class Http {
public:
    void start();

private:
     static void createToken();
     void server() const;
    int shutdown_flag = 0;
    static void handleConnection(int new_socket);
    static ssize_t getContentLength(const std::string &request);
    static std::string parseRequest(const std::string &header, const std::string &body);
    static std::string httpResponse(const std::string &status, const std::string &responseBody);
    static std::string getAuthorization(const std::string &request);

    static std::string handleRoute(const std::string &path,
                                  const std::string &requestBody,
                                  const std::string &authHeader,
                                  const std::unordered_map<std::string,
                                          std::string> &queryParams
    );
    // 解析查询字符串
    static std::unordered_map<std::string, std::string> parseQuery(const std::string &query);
};


#endif //AUTO_HTTP_H
