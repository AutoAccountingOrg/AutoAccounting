//
// Created by Ankio on 2024/7/13.
//

#include "VersionManager.h"
std::string VersionManager::version;

void VersionManager::initVersion() {
    version = getVersion()
}


std::string VersionManager::getVersion() {
    std::string  localVersion;
    FILE *file = fopen("version.txt", "r");
    if (file == nullptr) {
        file = fopen("version.txt", "w");
        fprintf(file, "%s", "1.0.0");
        localVersion = "1.0.0";
    } else {
        char buf[1024];
        fgets(buf, 1024, file);
        localVersion =  buf;
    }

    return  localVersion;
}


bool VersionManager::checkVersion() {
    return version == getVersion();
}
