

#ifndef AUTO_SERVER_H
#define AUTO_SERVER_H

#include <unordered_map>
#include <fstream>
#include <string>
#include "File.h"
#include "js/quickjspp.hpp"
#include <queue>
#include <map>
#include <netinet/in.h>
#include <list>
#include <thread>

class Server {
public:
    void start();

    //发布token
    static void publishToken();

    //启动服务器
    void server();

    int server_fd = 0;
    struct sockaddr_in address{};
private:
    //创建token
    static void createToken();

    static void processWorker(int socket);

    void startWorker(int socket);

};


#endif //AUTO_SERVER_H
