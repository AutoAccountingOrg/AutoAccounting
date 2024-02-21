#include <iostream>
#include <sstream>
#include <random>
#include <string>
#include <unistd.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <sys/wait.h>
#include <fstream>
#include <unordered_map>
#include <sys/stat.h>
#include <sys/fcntl.h>
#include <thread>

#define PORT 52045
#define MAX_CONNECTIONS 32

static inline void ltrim(std::string &s) {
    s.erase(s.begin(), std::find_if(s.begin(), s.end(), [](unsigned char ch) {
        return !std::isspace(ch);
    }));
}

// Trim from end (in place)
static inline void rtrim(std::string &s) {
    s.erase(std::find_if(s.rbegin(), s.rend(), [](unsigned char ch) {
        return !std::isspace(ch);
    }).base(), s.end());
}

// Trim from both ends (in place)
static inline void trim(std::string &s) {
    ltrim(s);
    rtrim(s);
}


std::string httpResponse(const std::string &status, const std::string &responseBody) {

    return "HTTP/1.1 " + status + "\r\n" // 使用 \r\n 而不是 \n 作为行结束符，以符合HTTP协议标准
                                  "Content-Type: text/plain\r\n"
                                  "Content-Length: " + std::to_string(responseBody.size()) + "\r\n"
                                                                                             "Connection: close\r\n" // 添加这行来指示连接将被关闭
                                                                                             "\r\n" +
           responseBody;
}


void writeFile(const std::string &filename, const std::string &fileInfo) {
    std::ofstream outFile(filename + ".txt"); // 打开或创建文件用于写入
    if (outFile.is_open()) {
        outFile << fileInfo << std::endl; // 写入数据
        outFile.close(); // 关闭文件
    }
}

void trimLogFile(const std::string &filename, size_t maxLines) {
    std::deque<std::string> lines;
    std::string line;
    std::ifstream file(filename);

    // 读取所有行到deque
    while (getline(file, line)) {
        lines.push_back(line);
    }
    file.close();

    // 如果行数超过限制，则删除最早的行
    while (lines.size() > maxLines) {
        lines.pop_front();
    }

    // 重写文件
    std::ofstream outFile(filename);
    for (const auto &l: lines) {
        outFile << l << std::endl;
    }
}

std::string formatTime(std::time_t time) {
    std::tm *ptm = std::localtime(&time);
    char buffer[32];

    // 格式化日期和时间：YYYY-MM-DD HH:MM:SS
    std::strftime(buffer, 32, "%Y-%m-%d %H:%M:%S", ptm);

    return std::string(buffer);
}


void writeLog(const std::string &fileInfo) {
    // 获取当前时间和日期
    std::time_t now = std::time(nullptr);
    std::string timestamp = formatTime(now);

    // 设置日志文件的名称
    std::string logFileName = "log.txt";

    // 写入日志
    std::ofstream logFile(logFileName, std::ios_base::app);
    if (logFile.is_open()) {
        logFile << "[" << timestamp << "] " << fileInfo << std::endl;
        logFile.close();
    }

    // 保持日志文件的行数不超过2000
    trimLogFile(logFileName, 2000);
}


void writeData(const std::string &fileInfo) {
    std::string logFileName = "data.txt";
    // 写入数据
    std::ofstream logFile(logFileName, std::ios_base::app);
    if (logFile.is_open()) {
        logFile << fileInfo << std::endl;
        logFile.close();
    }

    // 保持数据的行数不超过500
    trimLogFile(logFileName, 500);
}


std::string readFile(const std::string &filename) {
    std::ifstream inFile(filename + ".txt"); // 打开文件用于读取

    if (inFile.is_open()) {
        std::stringstream buffer;
        buffer << inFile.rdbuf(); // 将文件内容读入到字符串流中

        std::string content = buffer.str(); // 将字符串流转换为字符串

        inFile.close(); // 关闭文件

        trim(content); // 移除字符串两端的空白字符（空格、制表符、换行符等

        return content;
    } else {
        return "";
    }
}

std::string handleRoute(const std::string &path,
            const std::string &requestBody,
            const std::string &authHeader,
            const std::unordered_map<std::string,
            std::string> &queryParams
            ) {
    std::string response = "OK";
    std::string status = "200 OK";
   // std::cout<<authHeader<<":"<<"Authorization"<<std::endl;
    //授权校验
    if (readFile("token") != authHeader || authHeader.empty()) {
        return httpResponse("401 Incorrect Authorization", "Incorrect Authorization");
    }

    if (path == "/") {
        response = "Welcome to use 自动记账";
        return httpResponse("200 OK", response);
    } else if (path == "/get") {
        if (queryParams.find("name") != queryParams.end()) {
            std::string key = queryParams.at("name");
            if (key != "token") {
                response = readFile(key);
            }
        }
    } else if (path == "/set") {
        if (queryParams.find("name") != queryParams.end()) {
            std::string key = queryParams.at("name");

//            std::cout<<key<<":"<<requestBody<<std::endl;

            if (
                    key != "data"
                    && key != "log"
                    && key != "token"
                    ) {
                writeFile(queryParams.at("name"), requestBody);
            }
        }
    } else if (path == "/log") {
        writeLog(requestBody);
    } else if (path == "/data") {
        writeData(requestBody);
    } else {
        response = "404 Not Found";
        status = "404 Not Found";
    }


    return httpResponse(status, response);
}

std::unordered_map<std::string, std::string> parseQuery(const std::string &query) {
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

std::string getAuthorization(const std::string &request) {
    std::istringstream stream(request);
    std::string line;
    while (std::getline(stream, line) && line != "\r") {
        if (line.find("Authorization:") == 0) {
            return line.substr(14);
        }
    }
    return "";
}

std::string parseRequest(const std::string &header, const std::string &body) {
    size_t pos = 0, end;

    // 解析请求行
    end = header.find("\n", pos);
    std::string requestLine = header.substr(pos, end - pos);
    // 分离方法、路径和HTTP版本
    end = requestLine.find(" ");
    std::string method = requestLine.substr(0, end);
    size_t pathStart = end + 1;
    end = requestLine.find(" ", pathStart);
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

ssize_t getContentLength(const std::string &request) {
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


void handleConnection(int socket) {
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
            contentLength = getContentLength(request);
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
            response = httpResponse("500 Internal Server Error",
                                    "An error occurred while processing the request.");
        }
        write(socket, response.c_str(), response.size());
    }

    close(socket);
}

void daemonize(std::string &path) {
    pid_t pid;

    // 创建子进程
    pid = fork();

    // 如果创建失败，退出
    if (pid < 0) exit(EXIT_FAILURE);

    // 如果是父进程，退出
    if (pid > 0) exit(EXIT_SUCCESS);

    // 设置文件权限掩码
    umask(0);

    // 创建新的会话，成为会话领导，脱离控制终端
    if (setsid() < 0) exit(EXIT_FAILURE);

    // 改变工作目录
    if (chdir(path.c_str()) < 0) exit(EXIT_FAILURE);

    // 关闭所有打开的文件描述符
    for (int x = sysconf(_SC_OPEN_MAX); x >= 0; x--) {
        close(x);
    }

    // 重定向标准输入、输出和错误到/dev/null
    open("/dev/null", O_RDWR);
    dup(0);
    dup(0);
}

void createToken() {

    if (readFile("token") != "")return;

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

    writeFile("token", token);
}


// 全局标志，指示程序是否应该开始关闭流程
volatile sig_atomic_t shutdown_flag = 0;

void signal_handler(int signum) {
    std::cout << "Received signal " << signum << ", initiating shutdown..." << std::endl;
    shutdown_flag = 1;
}


int main(int argc, char *argv[]) {

    if (argc < 2) {
        std::cout << "Usage: " << argv[0] << " <path>" << std::endl;
        exit(EXIT_FAILURE);
    }
    std::string path = argv[1];

    daemonize(path);

// 注册信号处理函数
    signal(SIGINT, signal_handler);
    signal(SIGTERM, signal_handler);



    // 守护进程步骤结束，以下是原有的服务器代码

    createToken();

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

    while (shutdown_flag == 0) {
        if ((new_socket = accept(server_fd, (struct sockaddr *) &address, (socklen_t *) &addrlen)) <
            0) {
            perror("accept");
            exit(EXIT_FAILURE);
        }
        std::thread t(handleConnection, new_socket);
        t.detach(); // 让线程在后台运行
    }

    close(server_fd);

    return 0;
}