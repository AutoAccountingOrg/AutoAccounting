#include <string>
#include <random>
#include <thread>
#include <sys/socket.h>
#include <netinet/in.h>
#include <unistd.h>
#include "Server.h"
#include "misc.h"
#include "Handler.h"
#include <queue>
#include <csignal>
#include <sys/mman.h>
#include <sys/wait.h>

#define PORT 52045
#define MAX_CONNECTIONS 128

extern std::string workspace;
extern bool debug;
extern void output(const std::string& message);


/**
 * 启动HTTP服务器
 */
void Server::start() {
    output("[INFO] Web服务端工作进程启动 [PID:" + std::to_string(getpid()) + "]");
    createToken();
    publishToken();
    try {
        server();
    } catch (const std::exception &e) {
        output("[ERROR] 工作进程启动异常");
    }
    output("[WARN] Web服务进程退出");
}

/**
 * 创建token
 */
void Server::createToken() {
    //如果token文件不为空，则不创建新的token
    std::string token = File::readFile(workspace + "token");
    if (!token.empty()) {
        output("[INFO] 使用已有token:" + token);
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
    File::writeFile(workspace + "token", token);
    output("[INFO] 创建新token:" + token);
}

/**
 * 发布token
 */
void Server::publishToken() {
    const std::string token = File::readFile(workspace + "token");
    if (File::fileExists(workspace + "apps.txt")) {
        std::string apps = File::readFile(workspace + "apps");
        output("[INFO] 发布token到应用程序:" + apps);
        std::istringstream stream(apps);
        std::string line;
        while (std::getline(stream, line)) {
            trim(line);
            std::string path = "/sdcard/Android/data/" + line + "/shell/";
            //使用函数创建文件夹
            File::createDir(path);
            File::writeFile(path + "token", token);
        }
    }
}

/**
 * 启动HTTP服务器
 */
void Server::server() {
    output("[INFO] 启动HTTP服务器");

    if ((server_fd = socket(AF_INET, SOCK_STREAM, 0)) == 0) {
        return;
    }

    int opt = 1;
    // 设置 SO_REUSEADDR 选项
    if (setsockopt(server_fd, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt))) {
        return;
    }

    address.sin_family = AF_INET;
    address.sin_addr.s_addr = INADDR_ANY;
    address.sin_port = htons(PORT);

    if (bind(server_fd, (struct sockaddr *) &address, sizeof(address)) < 0) {
        output("[ERROR] 端口绑定失败");
        return;
    }

    if (listen(server_fd, MAX_CONNECTIONS) < 0) {
        return;
    }

  //  int count = 0;
    while (true) {
        int new_socket;
        int addrlen = sizeof(address);
        if ((new_socket = accept(server_fd, (struct sockaddr *) &address, (socklen_t *) &addrlen)) < 0) {
            output("[ERROR] 连接失败");
            continue;
        }

      //  count++;

       // output("[INFO] 连接数：" + std::to_string(count) + " 个");

     //   output("[INFO] 新连接：" + std::to_string(new_socket));

        startWorker(new_socket);

    }
    close(server_fd);
    output("[INFO] HTTP服务器关闭");
  //  server();
}


void Server::startWorker(int socket) {

    output("[INFO] 启动工作线程");
    std::thread workerThread([this, socket]() {
        processWorker(socket);
    });
    workerThread.detach(); // 分离线程，让它自行运行

}


/**
 * 处理工作进程（Child Process）
 * @param socket
 */
void Server::processWorker(int socket) {
    output("[INFO] 工作线程  开始处理任务" + std::to_string(socket));
    Handler handler(socket);
    handler.handleConnection();
    close(socket);
    output("[INFO] 工作线程 处理" + std::to_string(socket) + " 结束");
}







