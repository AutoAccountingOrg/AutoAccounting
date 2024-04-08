#include <iostream>
#include <fstream>
#include <unistd.h>
#include <sys/wait.h>
#include <csignal>
#include <cstring>
#include <vector>
#include "http/server/Server.h"

#define NUM_WORKERS 1
#define PID_FILE "daemon.pid"

std::ofstream logFile;//日志记录
std::vector<pid_t> workerPids;  // 存储工作进程的 PID

void worker_process(int id);
void handle_signal(int sig);
void start_workers();
void stop_workers();
std::string select_workspace(char *argv[]);
std::string workspace; //工作环境

bool should_restart_workers = true;
int main(int argc, char *argv[]) {
    //一开始就选定工作目录
    workspace = select_workspace(argv);
    if (argc > 1) {
        // 处理外部命令
        std::ifstream pidFile(workspace + PID_FILE);
        pid_t pid;
        pidFile >> pid;
        pidFile.close();
        if (strcmp(argv[1], "stop") == 0) {
            if (pid > 0) {
                kill(pid, SIGTERM);
            }
            return 0;
        } else if (strcmp(argv[1], "restart") == 0) {
            if (pid > 0) {
                kill(pid, SIGHUP);
            }
            return 0;
        }else if (strcmp(argv[1], "status") == 0) {
            if (pid > 0 && kill(pid, 0) == 0) {
                std::cout << File::formatTime() <<"Daemon is running with PID " << pid << std::endl;
            } else {
                std::cout << File::formatTime() <<"Daemon is not running." << std::endl;
            }
            return 0;
        }
    }else{
        std::cout << "Usage: " << argv[0] << " [stop|restart|status|foreground|start  <path>?]" << std::endl;
        return 1;
    }
    // 打开日志文件
    logFile.open(workspace +"daemon.log");

    logFile << File::formatTime() <<"Master process started (PID: " << getpid() << ")" << std::endl;

    if (strcmp(argv[1], "foreground") != 0){
        // 创建守护进程
        pid_t pid = fork();
        if (pid > 0) {
            return 0;
        }
        setsid();


        // 设置工作目录为当前目录
        chdir(workspace.c_str());

        // 设置信号处理函数
        signal(SIGCHLD, handle_signal);
        signal(SIGTERM, handle_signal);
        signal(SIGHUP, handle_signal);

        // 保存 PID 到文件
        std::ofstream pidFile(workspace + PID_FILE);
        pidFile << getpid();
        pidFile.close();

        // 启动工作进程
        start_workers();

        while (true) {
            pause();
        }

    } else{
        // 在前台运行
       Server::server();
    }






    return 0;
}

std::string select_workspace(char *argv[]){
    if(argv[2] != nullptr) {
        return argv[2];
    }
    if(File::directoryExists("/sdcard/Android/data/net.ankio.auto.xposed/cache/shell")){
        workspace = "/sdcard/Android/data/net.ankio.auto.xposed/cache/shell/";
    }else if(File::directoryExists("/sdcard/Android/data/net.ankio.auto.helper/cache/shell")){
        workspace = "/sdcard/Android/data/net.ankio.auto.helper/cache/shell/";
    }else{
        printf( "You must have at least one of the following directories: /sdcard/Android/data/net.ankio.auto.xposed/cache/shell or /sdcard/Android/data/net.ankio.auto.helper/cache/shell" );
        exit(2);
    }
    return workspace;
}

void worker_process(int id) {
    logFile << File::formatTime() <<"Worker " << id << " started (PID: " << getpid() << ")" << std::endl;
    Server::start();
    logFile << File::formatTime() <<"Worker " << id << " stopped (PID: " << getpid() << ")" << std::endl;
}

void handle_signal(int sig) {
    switch (sig) {
        case SIGCHLD:
            pid_t pid;
            int status;
            while ((pid = waitpid(-1, &status, WNOHANG)) > 0) {
                logFile << File::formatTime() <<"Master: Detected worker process " << pid << " exit" << std::endl;
                // 从 workerPids 中移除退出的子进程 PID
                workerPids.erase(std::remove(workerPids.begin(), workerPids.end(), pid), workerPids.end());
                // 重新启动一个新的子进程
                if (!should_restart_workers) {
                    return;
                }
                pid_t newPid = fork();
                if (newPid == 0) {
                    // 在新的子进程中
                    int size = static_cast<int>(workerPids.size());
                    worker_process(size + 1);
                    exit(0);
                } else if (newPid > 0) {
                    // 在主进程中，记录新的子进程 PID
                    workerPids.push_back(newPid);
                    logFile << File::formatTime() <<"Master: Restarted worker process " << newPid << std::endl;
                } else {
                    logFile << File::formatTime() <<"Failed to fork new worker process" << std::endl;
                }
            }
            break;
        case SIGTERM:
            logFile << File::formatTime() <<"Master: Received SIGTERM, stopping" << std::endl;
            stop_workers();
            logFile.close();
            remove((workspace + PID_FILE).c_str());
            exit(0);
        case SIGHUP:
            logFile << File::formatTime() <<"Master: Received SIGHUP, restarting" << std::endl;
            stop_workers();
            logFile.close();
            logFile.open("daemon.log");
            start_workers();
            break;
        default:
            break;
    }
}

void start_workers() {
    pid_t pid;
    for (int i = 0; i < NUM_WORKERS; ++i) {
        pid = fork();
        if (pid == 0) {
            worker_process(i + 1);
            exit(0);
        } else if (pid > 0) {
            workerPids.push_back(pid);
            logFile << File::formatTime() <<"Master: Started worker process " << pid << std::endl;
        } else {
            logFile << File::formatTime() <<"Failed to fork worker process" << std::endl;
        }
    }
}

void stop_workers() {
    should_restart_workers = false;
    for (pid_t pid : workerPids) {
        kill(pid, SIGTERM);
        waitpid(pid, nullptr, 0);  // 等待工作进程退出
        logFile << File::formatTime() <<"Master: Stopped worker process " << pid << std::endl;
    }
    workerPids.clear();  // 清空工作进程 PID 列表
}
