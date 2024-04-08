#include <string>
#include <random>
#include <thread>
#include <sys/socket.h>
#include <netinet/in.h>
#include <unistd.h>
#include "Server.h"
#include "../../utils/trim.cpp"
#include "../handler/Handler.h"
#define PORT 52045
#define MAX_CONNECTIONS 32

extern std::string workspace;
extern std::ofstream logFile;

/**
 * 启动HTTP服务器
 */
void Server::start() {
    createToken();
    publishToken();
    server();
}
/**
 * 创建token
 */
void Server::createToken() {
    //如果token文件不为空，则不创建新的token
    std::string  token = File::readFile(workspace + "token");
    if (!token.empty()) {
        logFile << File::formatTime() << "Token already exists:"<<token << std::endl;
        return;
    }

    const std::string chars =
            "0123456789"
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            "abcdefghijklmnopqrstuvwxyz";
    const size_t length = 64;

    std::random_device rd;
    std::mt19937 generator(rd());
    int size = static_cast<int>(chars.size());
    std::uniform_int_distribution<> distribution(0, size - 1);
    token = "";
    for (size_t i = 0; i < length; ++i) {
        token += chars[distribution(generator)];
    }
   File::writeFile(workspace +"token", token);
    logFile << File::formatTime() << "Create new token:"<<token << std::endl;
}

/**
 * 发布token
 */
void Server::publishToken() {
    const std::string token = File::readFile(workspace +"token");
    if(File::fileExists(workspace + "apps.txt")){
        logFile << File::formatTime() << "Find Apps.txt " << std::endl;
        std::string apps = File::readFile(workspace +"apps");
        logFile << File::formatTime() << "Find Apps.txt Path => "<< workspace << "apps.txt" << std::endl;
        logFile << File::formatTime() << "Find Apps.txt => "<< apps << std::endl;

        std::istringstream stream(apps);
        std::string line;
        while (std::getline(stream, line)) {
            trim(line);
            std::string path = "/sdcard/Android/data/" + line + "/shell/";
            //使用函数创建文件夹
            File::createDir(path);
            File::writeFile(path + "token", token);
            logFile << File::formatTime() << "Publish Token :" << path + "token.txt" << std::endl;
        }
    }
}
//处理连接
void handleConnection(int socket) {
    Handler handler(socket);
    handler.handleConnection();
}
/**
 * 启动HTTP服务器
 */
void Server::server()  {
    logFile <<  File::formatTime() << "AutoAccounting server started." << std::endl;
    int server_fd, new_socket;
    struct sockaddr_in address{};
    int addrlen = sizeof(address);

    if ((server_fd = socket(AF_INET, SOCK_STREAM, 0)) == 0) {
        perror("socket failed");
        exit(EXIT_FAILURE);
    }

    int opt = 1;
    // 设置 SO_REUSEADDR 选项
    if (setsockopt(server_fd, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt))) {
        perror("setsockopt");
        exit(EXIT_FAILURE);
    }

    address.sin_family = AF_INET;
    address.sin_addr.s_addr = INADDR_ANY;
    address.sin_port = htons(PORT);

    if (bind(server_fd, (struct sockaddr *) &address, sizeof(address)) < 0) {
        perror("bind failed");
        logFile << File::formatTime() << "Bind port "<< PORT << "failed" << std::endl;
        exit(EXIT_FAILURE);
    }

    if (listen(server_fd, MAX_CONNECTIONS) < 0) {
        perror("listen");
        exit(EXIT_FAILURE);
    }

    int count = 0;
    while (count < MAX_CONNECTIONS / 2) { //连接超过16重启，保证稳定性，客户端需要有重试机制，例如重试3次，间隔 1秒，3秒，5秒这样
        if ((new_socket = accept(server_fd, (struct sockaddr *) &address, (socklen_t *) &addrlen)) < 0) {
            perror("accept");
            break;
        }
        std::thread t(handleConnection, new_socket);
        t.detach(); // 让线程在后台运行
        count++;
    }

    close(server_fd);
    close(new_socket);
}


