//
// Created by 徐贵江 on 2024/4/8.
//

#ifndef AUTO_THREADLOCALSTORAGE_H
#define AUTO_THREADLOCALSTORAGE_H


#include <thread>
#include <iostream>
#include <map>
#include <mutex>

class ThreadLocalStorage {
public:
    static std::map<std::thread::id, std::string>& getStorage() {
        static std::map<std::thread::id, std::string> storage;
        return storage;
    }

    static std::string& getJsRes() {
        auto& storage = getStorage();
        std::lock_guard<std::mutex> lock(mutex);
        return storage[std::this_thread::get_id()];
    }

    static void clearThreadLocalStorage() {
        auto& storage = getStorage();
        std::lock_guard<std::mutex> lock(mutex);
        storage.erase(std::this_thread::get_id());
    }

private:
    static std::mutex mutex;
};

#endif //AUTO_THREADLOCALSTORAGE_H
