//
// Created by Ankio on 2024/7/12.
//

#ifndef AUTOACCOUNTING_TOKENMANAGER_H
#define AUTOACCOUNTING_TOKENMANAGER_H
#include <string>

class TokenManager {
    public:
    static void  initToken();

    static void publishToken(const std::string &app,const std::string &token);

    static bool checkToken(const std::string& app,const std::string& token);
private:
    static std::string generateRandomString(int count = 8);
};


#endif //AUTOACCOUNTING_TOKENMANAGER_H
