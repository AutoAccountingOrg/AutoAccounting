// IAccountingService.aidl
package net.ankio.auto;

interface IAccountingService {
    //记录运行日志
    void log(String prefix,String log);
    //获取配置信息
    String get(String key);
    //放置配置信息
    void put(String key, String value);
    //分析是否存在金额信息，并存储于临时目录
    void analyzeData(int dataType,String app,String data);
    //同步数据的接口
   void sql(String table,String action,String data);
   String syncData();
   String getMap(String name);
}