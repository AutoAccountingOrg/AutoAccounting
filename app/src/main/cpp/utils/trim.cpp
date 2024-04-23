
#include <string>

static inline void ltrim(std::string &s) {
    s.erase(s.begin(), std::find_if(s.begin(), s.end(), [](unsigned char ch) {
        return !std::isspace(ch);
    }));
}

// Trim from end (in place)
static inline void rtrim(std::string &s) {
    s.erase(std::find_if(s.rbegin(), s.rend(), [](unsigned char ch) {
        return !std::isspace(ch);
    }).base(), s.end());
}

// Trim from both ends (in place)
static inline void trim(std::string &s) {
    ltrim(s);
    rtrim(s);
}

static inline std::string replaceSubstring(std::string str, const std::string& toReplace, const std::string& replacement) {
    size_t startPos = str.find(toReplace);
    if(startPos != std::string::npos) {
        str.replace(startPos, toReplace.length(), replacement);
    }
    return str;
}
