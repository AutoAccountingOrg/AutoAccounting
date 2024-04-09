
#ifndef AUTO_HANDLER_H
#define AUTO_HANDLER_H
#include <cerrno>
#include <unistd.h>
#include <string>
#include <iostream>
#include <unordered_map>
#include "../../js/quickjspp.hpp"

class Handler {

public:
    explicit Handler(int socket);
    void handleConnection();
private:
    int socket = 0;
     std::string getHeader(const std::string &header, std::string &request);
    // 获取请求体长度
     ssize_t getContentLength( std::string &request);
     std::string parseRequest( std::string &header,  std::string &body) ;
      std::string httpResponse(const std::string &status,const  std::string &responseBody) ;
     std::string getAuthorization( std::string &request) ;
      std::string handleRoute( std::string &path,
                             std::string &requestBody,
                             std::string &authHeader,
                            const std::unordered_map<std::string,
                            std::string> &queryParams
    );
    // 解析查询字符串
     std::unordered_map<std::string, std::string> parseQuery(const std::string &query) ;
     std::string js( std::string &js) ;
};


#endif //AUTO_HANDLER_H
