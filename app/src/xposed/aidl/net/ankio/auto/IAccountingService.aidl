// IAccountingService.aidl
package net.ankio.auto;

interface IAccountingService {
    //记录运行日志
    void log(String prefix,String log);
    //获取配置信息
    String get(String key);
    //放置配置信息
    void put(String key, String value);
    //系统级别唤醒App，只有当自动记账获取的数据有效时才进行唤醒
    void launchApp(String billInfo);
    //分析是否存在金额信息，并存储于临时目录
    String analyzeData(String data);
    //自动记账app启动时向服务接口获取临时的信息并存入数据库
    void getDataInfo();
}