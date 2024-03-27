#include <string>
#include <random>
#include <thread>
#include <sys/socket.h>
#include <netinet/in.h>
#include <unistd.h>
#include <sstream>
#include <unordered_map>
#include "Http.h"
#include "../utils/trim.cpp"
#define PORT 52045
#define MAX_CONNECTIONS 32


/**
 * 启动HTTP服务器
 */
void Http::start() {
    Http::createToken();
    Http::publishToken();
    this->server();
}
/**
 * 创建token
 */
void Http::createToken() {
    //如果token文件不为空，则不创建新的token
    if (!File::readFile("token").empty())return;

    const std::string chars =
            "0123456789"
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            "abcdefghijklmnopqrstuvwxyz";
    const size_t length = 64;

    std::random_device rd;
    std::mt19937 generator(rd());
    std::uniform_int_distribution<> distribution(0, chars.size() - 1);

    std::string token;
    for (size_t i = 0; i < length; ++i) {
        token += chars[distribution(generator)];
    }
   File::writeFile("token", token);
}

/**
 * 发布token
 */
void Http::publishToken(){
    //发布到所有支持的App
    const std::string token = File::readFile("token");
    //发布到所有支持的App
    //如果存在apps.txt，则读取并解析每一行
    //每一行的格式为：包名
    //然后将token发送到包名对应的App的缓存目录
    //例如：/sdcard/Android/data/com.example.app/cache/shell/token.txt
    //如果文件不存在，则创建文件
    //如果文件存在，则覆盖文件
    //文件内容为token
    if(File::fileExists("apps.txt")){
        std::string apps = File::readFile("apps.txt");
        std::istringstream stream(apps);
        std::string line;
        while (std::getline(stream, line)) {
            trim(line);
            std::string path = "/sdcard/Android/data/" + line + "/cache/shell/";
            //使用函数创建文件夹
            File::createDir(path);
            File::writeFile(path+"token.txt", token);
        }
    }
}

/**
 * 启动HTTP服务器
 */
void Http::server() const {
    int server_fd, new_socket;
    struct sockaddr_in address{};
    int addrlen = sizeof(address);

    if ((server_fd = socket(AF_INET, SOCK_STREAM, 0)) == 0) {
        perror("socket failed");
        exit(EXIT_FAILURE);
    }

    address.sin_family = AF_INET;
    address.sin_addr.s_addr = INADDR_ANY;
    address.sin_port = htons(PORT);

    if (bind(server_fd, (struct sockaddr *) &address, sizeof(address)) < 0) {
        perror("bind failed");
        exit(EXIT_FAILURE);
    }

    if (listen(server_fd, MAX_CONNECTIONS) < 0) {
        perror("listen");
        exit(EXIT_FAILURE);
    }


    while (this->shutdown_flag == 0) {
        if ((new_socket = accept(server_fd, (struct sockaddr *) &address, (socklen_t *) &addrlen)) <
            0) {
            perror("accept");
            exit(EXIT_FAILURE);
        }
        std::thread t(Http::handleConnection, new_socket);
        t.detach(); // 让线程在后台运行
    }

    close(server_fd);
}
/**
 * 处理连接
 * @param socket
 */
void Http::handleConnection(int socket) {
    std::string request, header, body;
    const size_t bufferSize = 4096;
    char buffer[bufferSize];

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
            contentLength = Http::getContentLength(request);
            bodyStart = request.find("\r\n\r\n") + 4;
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
            response = Http::httpResponse("500 Internal Server Error",
                                    "An error occurred while processing the request.");
        }
        write(socket, response.c_str(), response.size());
    }

    close(socket);
}

/**
 * 构造HTTP响应
 * @param status
 * @param responseBody
 * @return
 */
std::string Http::httpResponse(const std::string &status, const std::string &responseBody) {

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
std::string Http::handleRoute(const std::string &path,
                        const std::string &requestBody,
                        const std::string &authHeader,
                        const std::unordered_map<std::string,
                                std::string> &queryParams
) {
    std::string response = "OK";
    std::string status = "200 OK";
    //授权校验
    if (File::readFile("token") != authHeader || authHeader.empty()) {
        return httpResponse("401 Incorrect Authorization", "Incorrect Authorization");
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
    } else {
        response = "404 Not Found";
        status = "404 Not Found";
    }


    return httpResponse(status, response);
}
/**
 * 解析查询参数
 * @param query
 * @return
 */
std::unordered_map<std::string, std::string> Http::parseQuery(const std::string &query) {
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
std::string Http::getAuthorization(const std::string &request) {
    std::istringstream stream(request);
    std::string line;
    while (std::getline(stream, line) && line != "\r") {
        if (line.find("Authorization:") == 0) {
            return line.substr(14);
        }
    }
    return "";
}
/**
 * 解析请求
 * @param header
 * @param body
 * @return
 */
std::string Http::parseRequest(const std::string &header, const std::string &body) {
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
        queryParams = Http::parseQuery(path.substr(queryPos + 1));
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
ssize_t Http::getContentLength(const std::string &request) {
    std::istringstream stream(request);
    std::string line;
    while (std::getline(stream, line) && line != "\r") {
        if (line.find("Content-Length:") == 0) {
            std::istringstream lengthStream(line.substr(15));
            ssize_t length;
            lengthStream >> length;
            return length;
        }
    }
    return -1;
}




