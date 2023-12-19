/*
 * Copyright (C) 2023 ankio(ankio@ankio.net)
 * Licensed under the Apache License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-3.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package net.ankio.auto.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.runBlocking
import net.ankio.auto.constant.DataType
import net.ankio.auto.database.Db
import net.ankio.auto.database.table.AccountMap
import net.ankio.auto.database.table.AppData
import net.ankio.auto.hooks.android.AccountingService
import net.ankio.auto.ui.activity.RestartActivity


object ActiveUtils {
    fun getActiveAndSupportFramework(): Boolean {
        return false
    }
    fun errorMsg(context: Context):String{
        return context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }

    fun startApp(mContext:Context){
        val intent: Intent? = mContext.packageManager.getLaunchIntentForPackage("net.ankio.auto.xposed")
        if (intent != null) {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            mContext.startActivity(intent)
        }
    }
    fun onStartApp(activity: Activity){
        val service = AccountingService.get()
        if(service==null){
            val intent = Intent(activity, RestartActivity::class.java)
            activity.startActivity(intent)
            return
        }


      runBlocking {
          val data = service.syncData()

          val jsonArray = Gson().fromJson(data,JsonArray::class.java) ?: return@runBlocking

          for (string in jsonArray){
              Db.get().AppDataDao().insert(AppData.fromJSON(string.asString))
          }
      }

        //测试一下重构数据
        val data = """
            {
                "mk": 231053230251200014,
                "st": 1,
                "isSc": 0,
                "appId": "",
                "mct": 1697209372000,
                "pl": "{\"templateType\":\"BN\",\"commandType\":\"UPDATE\",\"withPushNfc\":\"Y\",\"expireLink\":\"\",\"msgType\":\"TODO\",\"icon\":\"https:\/\/gw.alipayobjects.com\/mdn\/rms_f96971\/afts\/img\/A*leNcS41oUu0AAAAAAAAAAABkARQnAQ\",\"link\":\"alipays:\/\/platformapi\/startapp?appId=20000003&actionType=toBillDetails&tradeNO=20231013200040011100670089706568&bizType=D_TRANSFER?tagid=MB_SEND_PH\",\"businessId\":\"PAY_HELPER_CARD_2088032022319723\",\"msgId\":\"ee195465b09e7082af3214ec2bab83bd00972\",\"templateCode\":\"00059_00094_zfzs001\",\"templateId\":\"WALLET-BILL@BLPaymentHelper\",\"title\":\"收到一笔转账\",\"content\":\"{\\\"status\\\":\\\"收到一笔转账\\\",\\\"date\\\":\\\"10月13日\\\",\\\"amountTip\\\":\\\"\\\",\\\"money\\\":\\\"0.01\\\",\\\"unit\\\":\\\"元\\\",\\\"infoTip\\\":\\\"\\\",\\\"failTip\\\":\\\"\\\",\\\"goto\\\":\\\"alipays:\/\/platformapi\/startapp?appId=20000003&actionType=toBillDetails&tradeNO=20231013200040011100670089706568&bizType=D_TRANSFER\\\",\\\"content\\\":[{\\\"title\\\":\\\"付款人：\\\",\\\"content\\\":\\\"从前慢 185******30\\\"}],\\\"ad\\\":[],\\\"actions\\\":[{\\\"name\\\":\\\"\\\",\\\"url\\\":\\\"\\\"},{\\\"name\\\":\\\"查看详情\\\",\\\"url\\\":\\\"alipays:\/\/platformapi\/startapp?appId=20000003&actionType=toBillDetails&tradeNO=20231013200040011100670089706568&bizType=D_TRANSFER\\\"}]}\",\"linkName\":\"\",\"bizName\":\"支付助手\",\"msgCategory\":\"bill\",\"scm\":\"27.gotone.card.ee195465b09e7082af3214ec2bab83bd00972.MB_SEND_PH.null.payment_assist.10090.10099.2019062521000502094325.9991746\",\"assistInfo\":\"{\\\"showInFriendTab\\\":false,\\\"icon\\\":\\\"https:\/\/gw.alipayobjects.com\/mdn\/rms_f96971\/afts\/img\/A*ZapXT6AjvG0AAAAAAAAAAABkARQnAQ\\\",\\\"reminderType\\\":\\\"point\\\",\\\"hiddenMsgHeader\\\":false,\\\"title\\\":\\\"支付助手\\\",\\\"reminderTypeModifyEnable\\\":false,\\\"assistId\\\":\\\"payment_assist\\\",\\\"desc\\\":\\\"支付宝资金变动通知\\\"}\",\"ih\":\"{\\\"extInfo\\\":{},\\\"iid\\\":\\\"105\\\",\\\"ioty\\\":\\\"aor\\\",\\\"itemBasicInfo\\\":{\\\"dn\\\":\\\"服务提醒\\\",\\\"ic\\\":\\\"https:\/\/gw.alipayobjects.com\/zos\/bmw-prod\/b96c31e0-64ba-4cbb-9a64-73ff17d701fd.webp\\\",\\\"lk\\\":\\\"alipays:\/\/platformapi\/startapp?appId=20000235&source=friendTab\\\"},\\\"itemUserRelation\\\":{\\\"ilf\\\":\\\"N\\\",\\\"uc\\\":{\\\"top\\\":\\\"0\\\"},\\\"version\\\":0},\\\"ity\\\":\\\"105\\\",\\\"moty\\\":\\\"u\\\",\\\"msgInfo\\\":{\\\"bm\\\":\\\"收到一笔转账￥0.01 \\\",\\\"cmid\\\":\\\"ee195465b09e7082af3214ec2bab83bd00972\\\",\\\"lmt\\\":1697209371912,\\\"msrid\\\":\\\"00059_00094_zfzs001\\\",\\\"ncs\\\":\\\"N\\\",\\\"rps\\\":\\\"num\\\",\\\"sam\\\":\\\"Y\\\",\\\"ssrid\\\":\\\"PAY_HELPER_CARD_2088032022319723\\\",\\\"urn\\\":1}}\",\"bizMonitor\":\"{\\\"serviceCode\\\":\\\"MB_SEND_PH\\\",\\\"bizName\\\":\\\"\\\",\\\"createTime\\\":1697209371973,\\\"businessId\\\":\\\"PAY_HELPER_CARD_2088032022319723\\\",\\\"messageId\\\":\\\"ee195465b09e7082af3214ec2bab83bd00972\\\",\\\"messageTitle\\\":\\\"收到一笔转账\\\",\\\"pid\\\":\\\"2088622103925679\\\",\\\"templateId\\\":\\\"WALLET-BILL@BLPaymentHelper\\\",\\\"status\\\":\\\"\\\"}\",\"languageType\":\"zh-Hans\",\"subscribeConfig\":\"0\",\"gmtCreate\":1697209371912,\"gmtValid\":1699801371907,\"operate\":\"SEND\",\"templateName\":\"支付助手\",\"homePageTitle\":\"收到一笔转账￥0.01 \",\"status\":\"\",\"extraInfo\":\"{\\\"topSubContent\\\":\\\"收到一笔转账\\\",\\\"preValue\\\":\\\"￥\\\",\\\"languageType\\\":\\\"zh-Hans\\\",\\\"isPaymentMsg\\\":true,\\\"assistName2\\\":\\\"\\\",\\\"assistName3\\\":\\\"\\\",\\\"assistName1\\\":\\\"付款人\\\",\\\"templateId\\\":\\\"WALLET-FWC@remindNumber\\\",\\\"content\\\":\\\"0.01\\\",\\\"linkName\\\":\\\"\\\",\\\"assistMsg3\\\":\\\"\\\",\\\"sceneExt2\\\":{\\\"sceneUrl\\\":\\\"alipays:\/\/platformapi\/startapp?appId=20000003&actionType=toBillDetails&tradeNO=20231013200040011100670089706568&bizType=D_TRANSFER\\\",\\\"sceneType\\\":\\\"nativeApp\\\",\\\"sceneName\\\":\\\"转账\\\",\\\"sceneIcon\\\":\\\"https:\/\/gw.alicdn.com\/tfs\/TB19XIWiIieb18jSZFvXXaI3FXa-100-100.png\\\"},\\\"assistMsg2\\\":\\\"\\\",\\\"assistMsg1\\\":\\\"从前慢 185******30\\\",\\\"assistName4\\\":\\\"\\\",\\\"assistMsg5\\\":\\\"\\\",\\\"assistMsg4\\\":\\\"\\\",\\\"assistName5\\\":\\\"\\\",\\\"buttonLink\\\":\\\"\\\",\\\"cardAdInfo\\\":{\\\"p116\\\":{\\\"bizMonitor\\\":{\\\"marketingUniqueId\\\":\\\"MUb59f576f4d944f8e8b5b77f447def2eb\\\",\\\"outBizId\\\":\\\"LIFE_MSG_TEXT|AMTT202301041527374135|-\\\",\\\"marketingRuleId\\\":\\\"MR41b88eb9d64442b09a87a890d6848fc3\\\",\\\"style\\\":\\\"actionLine\\\",\\\"marketingItemId\\\":\\\"MI501400035beb4580a6f92e161f06d081\\\"},\\\"expireTime\\\":1728745371972,\\\"marketingUniqueId\\\":\\\"MUb59f576f4d944f8e8b5b77f447def2eb\\\",\\\"minClientVersion\\\":\\\"10.1.98\\\",\\\"recommendTemplateId\\\":\\\"actionLine\\\",\\\"viewInfo\\\":{\\\"btnName\\\":\\\"支付奖励\\\",\\\"actionType\\\":\\\"link\\\",\\\"tagIcon\\\":\\\"https:\/\/mdn.alipayobjects.com\/huamei_cke3ep\/afts\/img\/A*s1xhTYe-_PQAAAAAAAAAAAAADk1-AQ\/original\\\",\\\"tagColor\\\":\\\"#FF6011\\\",\\\"actionUrl\\\":\\\"alipays:\/\/platformapi\/startapp?appId=68687805&url=https://render.alipay.com/p/yuyan/180020380000000023/home-page.html?source=LIFE_MSG_TEXT__AMTT202301041527374135&chInfo=LIFE_MSG_TEXT\\\",\\\"tag\\\":\\\"领积分，兑各类生活好物\\\"}}}}\"}"
            }
        """.trimIndent()

     //   service.analyzeData(DataType.App.type,"com.eg.android.AlipayGphone",data)


    }

    fun get(key:String):String{
        return AccountingService.get()?.get(key) ?: ""
    }

    fun put(key:String,value:String){
        AccountingService.get()?.put(key,value)
    }

    fun getAccountMap(name:String):List<AccountMap>{
        val string = AccountingService.get()?.getMap(name) ?:""
        if(string.isEmpty()){
            return arrayListOf()
        }
        return  Gson().fromJson(string, object : TypeToken<List<AccountMap?>?>() {}.type)
    }

}