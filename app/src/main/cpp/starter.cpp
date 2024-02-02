#include <iostream>
#include <sstream>
#include <string>
#include <unistd.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <sys/wait.h>
#include <fstream>
#include <unordered_map>
#include <sys/stat.h>
#include <sys/fcntl.h>

#define PORT 52045
#define MAX_CONNECTIONS 6
#define TOKEN "qSohhh91qLBMtIMpdXoOwGn8vvVKJx6UXkZkiW"

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


std::string httpResponse(const std::string &status,const std::string &responseBody) {
    return "HTTP/1.1 " + status + "\nContent-Type: text/plain\nContent-Length: " +
           std::to_string(responseBody.size()) + "\n\n" + responseBody;
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
        return content;
    } else {
        return "";
    }
}

std::string
handleRoute(const std::string &path, const std::string &requestBody, const std::string &authHeader,const std::unordered_map<std::string, std::string>& queryParams) {
    std::string response = "OK";
    std::string status = "200 OK";
    if (path == "/") {
        response = "Welcome to use 自动记账";
        return httpResponse("200 OK", response);
    } else if (TOKEN != authHeader) {
        return httpResponse("401 Incorrect Authorization", "Incorrect Authorization");
    } else if (path == "/get") {
        if (queryParams.find("name") != queryParams.end()) {
            response = readFile(queryParams.at("name"));
        }
    } else if (path == "/set") {
        if (queryParams.find("name") != queryParams.end()) {
            std::string key = queryParams.at("name");
            if(key!="auth" && key!="data" && key!="log"){
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

std::unordered_map<std::string, std::string> parseQuery(const std::string& query) {
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

std::string parseRequest(const std::string &request) {
    size_t pos = 0, end;

    // 解析请求行
    end = request.find("\n", pos);
    std::string requestLine = request.substr(pos, end - pos);
    pos = end + 1; // 更新位置

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

    // 解析头部
    std::string authHeader;
    while ((end = request.find("\n", pos)) != std::string::npos) {
        std::string line = request.substr(pos, end - pos);
        pos = end + 1; // 更新位置

        // 移除回车符
        if (!line.empty() && line.back() == '\r') {
            line.pop_back();
        }

        // 跳过空行
        if (line.empty()) {
            break; // 头部结束
        }

        // 分割头部
        size_t colonPos = line.find(":");
        std::string headerName = line.substr(0, colonPos);
        std::string headerValue = line.substr(colonPos + 2);

        trim(headerName);
        trim(headerValue);

        // 处理特定头部
        if (headerName == "Authorization" || headerName == "authorization") {
            authHeader = headerValue;
        }
    }

    // 读取请求体
    std::string body = request.substr(pos);

    return handleRoute(path, body, authHeader, queryParams);
}


void handleConnection(int socket) {
    std::string request;
    const size_t bufferSize = 4096;
    char buffer[bufferSize];

    while (true) {
        ssize_t bytesRead = read(socket, buffer, bufferSize - 1);

        if (bytesRead < 0) {
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

        if (bytesRead < bufferSize - 1) {
            // 读取到的数据少于缓冲区大小，假设已经到达请求的末尾
            break;
        }
    }

    if (!request.empty()) {
        std::string response = parseRequest(request);
        write(socket, response.c_str(), response.size());
    }

    close(socket);
}
void daemonize()
{
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
    if (chdir("/data/local/tmp/autoAccount") < 0) exit(EXIT_FAILURE);

    // 关闭所有打开的文件描述符
    for (int x = sysconf(_SC_OPEN_MAX); x>=0; x--)
    {
        close (x);
    }

    // 重定向标准输入、输出和错误到/dev/null
    open("/dev/null",O_RDWR);
    dup(0);
    dup(0);
}


int main() {
    daemonize();

    // 守护进程步骤结束，以下是原有的服务器代码
    int server_fd, new_socket;
    struct sockaddr_in address{};
    int opt = 1;
    int addrlen = sizeof(address);

    if ((server_fd = socket(AF_INET, SOCK_STREAM, 0)) == 0) {
        perror("socket failed");
        exit(EXIT_FAILURE);
    }

    if (setsockopt(server_fd, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt))) {
        perror("setsockopt SO_REUSEADDR failed");
        exit(EXIT_FAILURE);
    }

    if (setsockopt(server_fd, SOL_SOCKET, SO_REUSEPORT, &opt, sizeof(opt))) {
        perror("setsockopt SO_REUSEPORT failed");
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

    while (true) {
        if ((new_socket = accept(server_fd, (struct sockaddr *) &address, (socklen_t *) &addrlen)) < 0) {
            perror("accept");
            exit(EXIT_FAILURE);
        }

        pid_t pid = fork();
        if (pid == 0) {
            // Child process
            close(server_fd);
            handleConnection(new_socket);
            exit(0);
        } else if (pid > 0) {
            // Parent process
            close(new_socket);
            waitpid(-1, nullptr, WNOHANG); // Clean up zombie processes
        } else {
            perror("fork");
            exit(EXIT_FAILURE);
        }
    }

    return 0;
}