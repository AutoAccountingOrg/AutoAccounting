/*
 * Copyright (C) 2024 ankio(ankio@ankio.net)
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

package net.ankio.auto.hooks.qianji.hooks

import android.app.Application
import de.robv.android.xposed.XposedHelpers
import net.ankio.auto.core.api.HookerManifest
import net.ankio.auto.core.api.PartHooker
import java.lang.reflect.Field
import java.lang.reflect.Modifier

class AutoHooker:PartHooker {
    lateinit var  addBillIntentAct: Class<*>
    override fun hook(
        hookerManifest: HookerManifest,
        application: Application?,
        classLoader: ClassLoader
    ) {

        addBillIntentAct = classLoader.loadClass("com.mutangtech.qianji.bill.auto.AddBillIntentAct")

        hookTimeout(hookerManifest)

    }


    /**
     *  public final void S0(Intent intent) {
     *         int parseInt;
     *         int i10;
     *         String string;
     *         ComponentName component;
     *         String str = null;
     *         AutoTaskLog autoTaskLog = new AutoTaskLog(System.currentTimeMillis(), (intent == null || (component = intent.getComponent()) == null) ? null : component.getPackageName(), e7.b.getInstance().getLoginUserID());
     *         if (intent != null) {
     *             Uri data = intent.getData();
     *             if (data != null) {
     *                 str = data.toString();
     *             }
     *             autoTaskLog.setValue(str);
     *             z6.a aVar = z6.a.f17726a;
     *             String str2 = n6.d.M;
     *             aVar.b(str2, "intent-data:" + data);
     *             if (data != null) {
     *                 if (!e7.b.getInstance().isLogin()) {
     *                     i10 = R.string.auto_task_not_login;
     *                 } else if (!v6.c.b("auto_task_last_time") || a7.a.timeoutApp("auto_task_last_time", FREQUENCY_LIMIT_TIME)) {
     *                     try {
     *                         String queryParameter = data.getQueryParameter(PARAM_TYPE);
     *                         parseInt = queryParameter != null ? Integer.parseInt(queryParameter) : 0;
     *                     } catch (Throwable th2) {
     *                         th2.printStackTrace();
     *                         String string2 = getResources().getString(R.string.auto_task_wrong_params_with_data, data);
     *                         i.f(string2, "getString(...)");
     *                         K0(string2, autoTaskLog);
     *                     }
     *                     if (!F0(parseInt)) {
     *                         String string3 = getResources().getString(R.string.auto_task_wrong_params_with_detail, PARAM_TYPE, String.valueOf(parseInt));
     *                         i.f(string3, "getString(...)");
     *                         K0(string3, autoTaskLog);
     *                         return;
     *                     }
     *                     String queryParameter2 = data.getQueryParameter(PARAM_CATEGORY_CHOOSE);
     *                     String queryParameter3 = data.getQueryParameter(PARAM_CATEGORY_THEME);
     *                     boolean z10 = I0(parseInt) && TextUtils.equals("1", queryParameter2);
     *                     this.O = z10;
     *                     if (z10) {
     *                         W0(queryParameter3);
     *                     } else {
     *                         setTheme(R.style.MyTheme_Transparent);
     *                         getWindow().setStatusBarColor(0);
     *                     }
     *                     c1(data, parseInt, queryParameter2, autoTaskLog);
     *                     if (this.O) {
     *                         overridePendingTransition(R.anim.activity_top_enter, 0);
     *                         return;
     *                     }
     *                     return;
     *                 } else {
     *                     i10 = R.string.auto_task_over_frequency;
     *                 }
     *                 string = getString(i10);
     *                 i.f(string, "getString(...)");
     *                 K0(string, autoTaskLog);
     *             }
     *         }
     *         string = getString(R.string.auto_task_wrong_params);
     *         i.f(string, "getString(...)");
     *         K0(string, autoTaskLog);
     *     }
     *
     */

    private fun hookTimeout(hookerManifest: HookerManifest){

        XposedHelpers.setStaticLongField(addBillIntentAct, "FREQUENCY_LIMIT_TIME", 0L)

        hookerManifest.logD("hookTimeout =${XposedHelpers.getStaticLongField(addBillIntentAct, "FREQUENCY_LIMIT_TIME")}")
    }


}