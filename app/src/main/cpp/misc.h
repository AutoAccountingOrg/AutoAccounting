//
// Created by Ankio on 2024/4/26.
//

#ifndef AUTOACCOUNTING_MISC_H
#define AUTOACCOUNTING_MISC_H

#include <string>
#include <vector>

void trim(std::string &s);

std::vector<std::string> split(const std::string &s, const std::string &delim);

std::string replaceSubstring(const std::string &str, const std::string &oldStr, const std::string &newStr);

#endif //AUTOACCOUNTING_MISC_H
