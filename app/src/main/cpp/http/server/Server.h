

#ifndef AUTO_SERVER_H
#define AUTO_SERVER_H


#include <unordered_map>
#include <fstream>
#include <string>
#include "../../file/File.h"
#include "../../js/quickjspp.hpp"




class Server{
public:
    static void start();
    //发布token
    static void publishToken();
    //启动服务器
    static void server();

private:
    //创建token
    static void createToken();

};


#endif //AUTO_SERVER_H
