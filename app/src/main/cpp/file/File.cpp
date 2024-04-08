

#include <fstream>
#include <sstream>
#include <deque>
#include "File.h"
#include "../utils/trim.cpp"
void File::writeFile(const std::string &filename, const std::string &fileInfo) {
    std::ofstream outFile(filename + ".txt"); // 打开或创建文件用于写入
    if (outFile.is_open()) {
        outFile << fileInfo << std::endl; // 写入数据
        outFile.close(); // 关闭文件
    }
}

void File::trimLogFile(const std::string &filename, size_t maxLines) {
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

std::string File::formatTime() {
    // 获取当前时间和日期
    std::time_t now = std::time(nullptr);
    std::tm *ptm = std::localtime(&now);
    char buffer[32];

    // 格式化日期和时间：YYYY-MM-DD HH:MM:SS
    std::strftime(buffer, 32, "[ %Y-%m-%d %H:%M:%S ] ", ptm);

    return {buffer};
}


void File::writeLog(const std::string &fileInfo) {

    std::string timestamp = formatTime();

    // 设置日志文件的名称
    std::string logFileName = "log.txt";

    // 写入日志
    std::ofstream logFile(logFileName, std::ios_base::app);
    if (logFile.is_open()) {
        logFile << timestamp  << fileInfo << std::endl;
        logFile.close();
    }

    // 保持日志文件的行数不超过2000
    trimLogFile(logFileName, 2000);
}


void File::writeData(const std::string &fileInfo) {
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


std::string File::readFile(const std::string &filename) {
    std::ifstream inFile(filename + ".txt"); // 打开文件用于读取
    if (inFile.is_open()) {
        std::stringstream buffer;
        buffer << inFile.rdbuf(); // 将文件内容读入到字符串流中

        std::string content = buffer.str(); // 将字符串流转换为字符串

        inFile.close(); // 关闭文件

        trim(content); // 移除字符串两端的空白字符（空格、制表符、换行符等

        return content;
    } else {

        return "";
    }
}

bool File::fileExists(const std::string &string) {
    std::ifstream file(string);
    if (file.good()) {
        file.close();
        return true;
    }
    return false;
}

bool File::directoryExists(const std::string& path) {
    return std::__fs::filesystem::exists(path);
}

void File::createDir(const std::string &path) {
    std::__fs::filesystem::create_directories(path);
}
