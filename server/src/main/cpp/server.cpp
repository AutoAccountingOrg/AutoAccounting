

#include <cstdio>
#include <unistd.h>
#include "ws/WebSocketServer.h"
#include "ws/Logger.h"

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
    printf("Server start!\n");
    if (argc > 1) {
        if (strcmp(argv[1], "debug") == 0) {
            Logger::debug = true;
        } else {
            chdir(argv[1]);
        }

        if (argc > 2 && strcmp(argv[2], "debug") == 0) {
            Logger::debug = true;
        }
    } else {
        chdir(".");
    }

    if(Logger::debug){
        printf("Server run in Debug mode!\n");
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

