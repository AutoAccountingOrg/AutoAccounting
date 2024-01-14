// IAccountingService.aidl
package net.ankio.auto;

// Declare any non-default types here with import statements

interface IAccountingService {

             //记录运行日志
                void log(String prefix,String log);
                //获取配置信息
                String get(String key);
                //放置配置信息
                void put(String key, String value);
                  //系统级别唤醒App
                void callApp();
                //
}