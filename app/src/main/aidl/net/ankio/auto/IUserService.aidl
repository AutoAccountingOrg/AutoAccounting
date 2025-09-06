// IAutoService.aidl
package net.ankio.auto;

// Declare any non-default types here with import statements

interface IUserService {
    void destroy() = 16777114; // Destroy method defined by Shizuku server

    String execCommand(String command) = 1;


}