

#include <cstdio>
#include <unistd.h>
#include "ws/WebSocketServer.h"
#define PORT 52045

int main(int argc, char** argv)
{
    printf("Server start!\n");
    // 选择默认工作目录，如果有参数就按照参数来，如果没有参数就当前路径
    if (argc > 1)
    {
        chdir(argv[1]);
    }
    else
    {
        chdir(".");
    }
    WebSocketServer server(PORT);
    printf("Server stopped\n");
}