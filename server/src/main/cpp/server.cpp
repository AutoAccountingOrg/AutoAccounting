

#include <cstdio>
#include <unistd.h>
#include "ws/WebSocketServer.h"
#define PORT 52045
#include <iostream>
#include <unistd.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <csignal>
#include <cstdlib>

pid_t child_pid;

void startChildProcess() {
    child_pid = fork();
    if (child_pid < 0) {
        std::cerr << "Fork failed!" << std::endl;
        exit(EXIT_FAILURE);
    } else if (child_pid == 0) {
        // 子进程执行的代码
        std::cout << "Child process started with PID: " << getpid() << std::endl;
        WebSocketServer server(PORT);
    }
}
time_t getNextRestartTime() {
    time_t now = time(nullptr);
    struct tm* now_tm = localtime(&now);

    // 设置为第二天的2点
    now_tm->tm_hour = 2;
    now_tm->tm_min = 0;
    now_tm->tm_sec = 0;
    now_tm->tm_mday += 1;

    return mktime(now_tm);
}

void restartChildProcess() {
    std::cout << "Restarting child process..." << std::endl;
    if (child_pid > 0) {
        kill(child_pid, SIGKILL); // 杀掉子进程
        waitpid(child_pid, nullptr, 0); // 等待子进程结束
    }
    startChildProcess(); // 重新启动子进程
}
int main(int argc, char** argv){
    printf("Server start!");
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
   /* startChildProcess(); // 启动子进程

    while (true) {
        time_t now = time(nullptr);
        time_t next_restart_time = getNextRestartTime();
        int seconds_to_sleep = static_cast<int>(difftime(next_restart_time, now));

        std::cout << "Sleeping for " << seconds_to_sleep << " seconds until next restart..." << std::endl;
        sleep(seconds_to_sleep); // 等待到指定时间

        restartChildProcess(); // 重新启动子进程
    }*/

    return 0;
}

