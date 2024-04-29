
#include <zconf.h>
#include <string>
#include "misc.h"
#include <vector>


std::vector<std::string> split(const std::string &s, const std::string &delim) {
    std::vector<std::string> result;
    size_t start = 0;
    size_t end = 0;
    while ((end = s.find(delim, start)) != std::string::npos) {
        result.push_back(s.substr(start, end - start));
        start = end + delim.length();
    }
    result.push_back(s.substr(start));
    return result;
}

void trim(std::string &s) {
    s.erase(s.begin(), std::find_if(s.begin(), s.end(), [](unsigned char ch) {
        return !std::isspace(ch);
    }));

    s.erase(std::find_if(s.rbegin(), s.rend(), [](unsigned char ch) {
        return !std::isspace(ch);
    }).base(), s.end());
}


std::string
replaceSubstring(const std::string &str, const std::string &oldStr, const std::string &newStr) {
    std::string result = str;
    size_t pos = 0;
    while ((pos = result.find(oldStr, pos)) != std::string::npos) {
        result.replace(pos, oldStr.length(), newStr);
        pos += newStr.length();
    }
    return result;
}

