//
// Created by Ankio on 2024/7/13.
//

#ifndef AUTOACCOUNTING_VERSIONMANAGER_H
#define AUTOACCOUNTING_VERSIONMANAGER_H
#include "string"

class VersionManager {
public:
    static void initVersion();


    static bool checkVersion();
private:
    static std::string version;
    static std::string getVersion();
};


#endif //AUTOACCOUNTING_VERSIONMANAGER_H
