
#ifndef AUTO_HANDLER_H
#define AUTO_HANDLER_H
#include <cerrno>
#include <unistd.h>
#include <string>
#include <iostream>
#include <unordered_map>

class Handler {

public:
    explicit Handler(int socket);
    void handleConnection() const;
private:
    int socket;
    static std::string getHeader(const std::string &header, std::string &request);
    // 获取请求体长度
    static ssize_t getContentLength( std::string &request);
    static std::string parseRequest( std::string &header,  std::string &body) ;
    static  std::string httpResponse(const std::string &status,const  std::string &responseBody) ;
    static std::string getAuthorization( std::string &request) ;
    static  std::string handleRoute( std::string &path,
                             std::string &requestBody,
                             std::string &authHeader,
                            const std::unordered_map<std::string,
                            std::string> &queryParams
    );
    // 解析查询字符串
    static std::unordered_map<std::string, std::string> parseQuery(const std::string &query) ;
    static std::string js( std::string &js) ;
};


#endif //AUTO_HANDLER_H
