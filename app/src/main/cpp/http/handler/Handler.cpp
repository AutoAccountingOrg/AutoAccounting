
#include "Handler.h"
#include <cerrno>
#include <unistd.h>
#include <string>
#include <iostream>
#include <unordered_map>
#include "../../file/File.h"
#include "../../utils/ThreadLocalStorage.h"
#include "../../utils/trim.cpp"
#include "../server/Server.h"
extern std::string workspace;
extern std::ofstream logFile;

Handler::Handler(int socket) : socket(socket) {}

/**
 * 处理连接
 * @param socket
 */
void Handler::handleConnection()   {
    std::string request, header, body;
    size_t bufferSize = 4096;
    char buffer[bufferSize];//一次读取缓存是4096
    ssize_t contentLength = -1;
    ssize_t bodyStart = 0;
    bool headerReceived = false;
    while (true) {
        memset(buffer, 0, bufferSize); // 清理缓冲区
        ssize_t bytesRead = read(socket, buffer, bufferSize - 1);
        if (bytesRead < 0) {
            perror("read error");
            // 发生错误
            if (errno == EAGAIN || errno == EWOULDBLOCK) {
                // 非阻塞模式下，没有数据可读
                continue;
            } else {
                // 其他错误，关闭连接
                break;
            }
        } else if (bytesRead == 0) {
            // 读取完成，没有更多数据
            break;
        }
        buffer[bytesRead] = '\0';
        request.append(buffer);
        // 检查是否接收到完整的HTTP请求
        if (!headerReceived && request.find("\r\n\r\n") != std::string::npos) {
            // 请求头接收完毕
            headerReceived = true;
            contentLength = getContentLength(request);
            int index = static_cast<int>(request.find("\r\n\r\n"));
            bodyStart = index + 4;
            header = request.substr(0, bodyStart);
        }

        if (headerReceived) {
            if (contentLength <= 0)break;
            // 已接收到请求头且已解析出Content-Length
            if (request.length() >= bodyStart + contentLength) {
                // 已接收到完整的请求体
                break;
            }
        }
    }

    body = request.substr(bodyStart);



    if (!header.empty()) {
        std::string response;
        try {
            response = parseRequest(header, body);
        } catch (const std::exception &e) {
            // 发生错误，发送错误响应
            response = httpResponse("500 Internal Handler Error",
                                          "An error occurred while processing the request.");
        }
        write(socket, response.c_str(), response.size());
    }

    close(socket);
    ThreadLocalStorage::clearThreadLocalStorage();



}

/**
 * 构造HTTP响应
 * @param status
 * @param responseBody
 * @return
 */
 std::string Handler::httpResponse(const std::string &status,const std::string &responseBody) {
    return "HTTP/1.1 " + status + "\r\n" // 使用 \r\n 而不是 \n 作为行结束符，以符合HTTP协议标准
           "Content-Type: text/plain\r\n"
           "Content-Length: " + std::to_string(responseBody.size()) + "\r\n"
           "Connection: close\r\n" // 添加这行来指示连接将被关闭
           "\r\n" +
           responseBody;
}



/**
 * 处理路由
 * @param path
 * @param requestBody
 * @param authHeader
 * @param queryParams
 * @return
 */
std::string Handler::handleRoute(std::string &path,
                                 std::string &requestBody,
                                  std::string &authHeader,
                                 const std::unordered_map<std::string,
                                         std::string> &queryParams
) {
    std::string response = "OK";
    std::string status = "200 OK";
    //授权校验
    if (File::readFile("token") != authHeader || authHeader.empty()) {
        Server::publishToken();
        return httpResponse("401 Incorrect Authorization", "Incorrect Authorization: "+authHeader);
    }

    if (path == "/") {
        response = "Welcome to use 自动记账";
        return httpResponse("200 OK", response);
    } else if (path == "/get") {

        if (queryParams.find("name") != queryParams.end()) {
            std::string key = queryParams.at("name");
            if (key != "token") {
                response = File::readFile(key);
            }
        }
    } else if (path == "/set") {
        if (queryParams.find("name") != queryParams.end()) {
            std::string key = queryParams.at("name");

            if (
                    key != "data"
                    && key != "log"
                    && key != "token"
                    ) {
                File::writeFile(queryParams.at("name"), requestBody);
            }
        }
    } else if (path == "/log") {
        File::writeLog(requestBody);
    } else if (path == "/data") {
        File::writeData(requestBody);
    } else if (path == "/js") {
        response = js(requestBody);
        //执行js
    } else {
        response = "404 Not Found";
        status = "404 Not Found";
    }


    return httpResponse(status, response);
}

/**
 * 打印
 * @param args
 */
void println(qjs::rest<std::string> args)  {
    std::cout << args[0] << std::endl;
    ThreadLocalStorage::getJsRes() = args[0];
}

 std::string Handler::js(std::string &js) {
     std::cout << js << std::endl;
    qjs::Runtime runtime;
    qjs::Context context(runtime);
    try
    {

        auto& module = context.addModule("MyModule");
        module.function<&println>("print");
        context.eval(R"xxx(
             import { print } from 'MyModule';
            globalThis.print = print;
        )xxx", "<import>", JS_EVAL_TYPE_MODULE);

        context.eval(js);

        return ThreadLocalStorage::getJsRes();
    }
    catch(qjs::exception &e)
    {
        auto exc = context.getException();
        logFile <<  File::formatTime() << "JS Error: "<< (std::string) exc << std::endl;
        std::cout << (std::string) exc << std::endl;
        if((bool) exc["stack"])
            logFile <<  File::formatTime() << "JS Error: "<< (std::string) exc["stack"]  << std::endl;
    }
    return "";
}
/**
 * 解析查询参数
 * @param query
 * @return
 */
std::unordered_map<std::string, std::string> Handler::parseQuery(const std::string &query) {
    std::unordered_map<std::string, std::string> queryParams;
    std::istringstream queryStream(query);
    std::string pair;

    while (getline(queryStream, pair, '&')) {
        auto delimiterPos = pair.find('=');
        auto key = pair.substr(0, delimiterPos);
        auto value = pair.substr(delimiterPos + 1);
        trim(key);
        trim(value);
        queryParams[key] = value;
    }

    return queryParams;
}
/**
 * 获取请求头中的Authorization
 * @param request
 * @return
 */
std::string Handler::getAuthorization( std::string &request) {
    return getHeader("Authorization:",request);
}
/**
 * 解析请求
 * @param header
 * @param body
 * @return
 */
std::string Handler::parseRequest( std::string &header, std::string &body) {
    size_t pos = 0, end;

    // 解析请求行
    end = header.find('\n', pos);
    std::string requestLine = header.substr(pos, end - pos);
    // 分离方法、路径和HTTP版本
    end = requestLine.find(' ');
    std::string method = requestLine.substr(0, end);
    size_t pathStart = end + 1;
    end = requestLine.find(' ', pathStart);
    std::string path = requestLine.substr(pathStart, end - pathStart);
    std::string httpVersion = requestLine.substr(end + 1);
    // 提取查询参数
    std::unordered_map<std::string, std::string> queryParams;
    size_t queryPos = path.find('?');
    if (queryPos != std::string::npos) {
        queryParams = parseQuery(path.substr(queryPos + 1));
        path = path.substr(0, queryPos);
    }
    std::string authHeader  = getAuthorization(header);
    trim(authHeader);

    return handleRoute(path, body,authHeader , queryParams);
}



/**
 * 获取请求头中的Content-Length
 * @param request
 * @return
 */
ssize_t Handler::getContentLength( std::string &request) {
    std::string header = getHeader("Content-Length:",request);
    std::istringstream lengthStream(header);
    ssize_t length;
    lengthStream >> length;
    return length;
}

/**
 * 获取请求头中的指定字段
 * @param header
 * @param request
 * @return
 */
std::string Handler::getHeader(const std::string &header, std::string &request) {
    std::istringstream stream(request);
    std::string line;
    while (std::getline(stream, line) && line != "\r") {
        if (line.find(header) == 0) {
            return line.substr(header.size());
        }
    }
    return "";
}
