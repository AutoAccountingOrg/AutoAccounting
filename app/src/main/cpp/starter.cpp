#include <iostream>
#include <fstream>
#include <unistd.h>
#include <csignal>
#include <cstring>
#include <vector>
#include <sys/wait.h>
#include "Server.h"
#include "starter.h"
#include "misc.h"


std::string select_workspace(char *argv[]);

std::string workspace; //工作环境
Server autoAccountingServer;
bool debug = false;
std::string version = "1.0.1";
void output(const std::string& message) {
    std::cout << File::formatTime()  <<message << std::endl;
}
void startServer();
void handle_sigchld(int sig) {
    int status;
    pid_t pid;
    bool shouldRestart = true;
    while ((pid = waitpid(-1, &status, WNOHANG)) > 0) {
        if (WIFEXITED(status)) {
            printf("[WARN] 子进程 %d 正常退出，退出码为：%d\n", pid, WEXITSTATUS(status));
        } else if (WIFSIGNALED(status)) {
            int code = WTERMSIG(status);
            if (code == TOO_MATCH_CONNECTIONS_ERROR || code == BIND_ADDRESS_ERROR){
                shouldRestart = false;
            }
            printf("[WARN] 子进程 %d 因为信号 %d 退出\n", pid, WTERMSIG(status));
        }
    }
    if(shouldRestart)startServer();
}

int main(int argc, char *argv[]) {
    //一开始就选定工作目录
    workspace = select_workspace(argv);
    version = File::readFile(workspace + "VERSION");
    debug = File::readFile(workspace + "debug") == "true";

    if (argc > 1) {
        output("[INFO] 自动记账服务 Version: " + version);
        std::string mode = debug ? "调试" : "生产";
        output("[INFO] 当前模式 : " + mode);
        // 处理外部命令
        std::ifstream pidFile(workspace + PID_FILE);
        pid_t pid;
        pidFile >> pid;
        pidFile.close();
    } else {
        output("[ERROR]  使用方法: " + std::string(argv[0]) + " foreground|start [<path>?]");
        return 1;
    }

    output("[INFO] 工作目录: " + workspace);
    output("[INFO] 父进程启动: " + std::to_string(getpid()));


    if (strcmp(argv[1], "foreground") == 0) {
        output("[INFO] 服务前台运行中 ");
        autoAccountingServer.server();
    } else {
        signal(SIGCHLD, handle_sigchld);
        startServer();
        while (true){
            pause();
        }
    }
}

void startServer() {
    output("[INFO] 服务将以守护进程的方式运行 ");

    // 创建守护进程
    pid_t pid = fork();
    if (pid > 0) {
        output("[INFO] 父进程结束。 ");

    }else if(pid < 0){
        output("[ERROR] 创建子进程失败。 ");
        exit(FORK_CHILD_ERROR);
    }else{
        setsid();
        chdir(workspace.c_str());
        autoAccountingServer = Server();
        autoAccountingServer.start();
    }
}



std::string select_workspace(char *argv[]) {
    if (argv[2] != nullptr) {
        return argv[2];
    }

    std::string packages[] = {
            "net.ankio.auto.xposed",
            "net.ankio.auto.helper"
    };

    for (auto &package: packages) {
        if (File::directoryExists("/sdcard/Android/data/" + package + "/cache/shell")) {
            workspace = "/sdcard/Android/data/" + package + "/cache/shell/";
            return workspace;
        }
    }

    output("[ERROR] 缺失工作目录，请传入参数指定工作目录。");
    exit(EXIT_FAILURE);
}
