

#ifndef AUTO_HTTP_H
#define AUTO_HTTP_H


#include <unordered_map>
#include <fstream>
#include <string>
#include "../file/File.h"

class Http {
public:
    void start();
    Http(const std::string& workspace, std::ofstream *logFile) {
        this->workspace = workspace;
        this->logFile = logFile;
        *this->logFile <<  File::formatTime() << "AutoAccounting server started." << std::endl;
    }
private:
      void createToken();
     void publishToken();
     void server() const;
    int shutdown_flag = 0;
    std::string workspace;
    std::ofstream *logFile;
     void handleConnection(int new_socket) const;
     ssize_t getContentLength(const std::string &request) const;
     std::string parseRequest(const std::string &header, const std::string &body) const;
     std::string httpResponse(const std::string &status, const std::string &responseBody) const;
     std::string getAuthorization(const std::string &request) const;

     std::string handleRoute(const std::string &path,
                                  const std::string &requestBody,
                                  const std::string &authHeader,
                                  const std::unordered_map<std::string,
                                          std::string> &queryParams
    ) const;
    // 解析查询字符串
     std::unordered_map<std::string, std::string> parseQuery(const std::string &query) const;

};


#endif //AUTO_HTTP_H
