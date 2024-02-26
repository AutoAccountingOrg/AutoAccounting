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

package net.ankio.auto.ui.componets

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import com.google.android.flexbox.FlexboxLayout


/**
 * 虚拟流动布局器
 */
class FlowLayoutManager(context: Context, attrs: AttributeSet): FlexboxLayout(context,attrs) {
    var firstWaveTextViewPosition: Int = 0
    var textAppearance = com.google.android.material.R.style.TextAppearance_Material3_HeadlineLarge
    private var arrayList:ArrayList<FlowElement> = ArrayList()


    fun appendTextView(text: String, elem: FlowElement? = null): FlowElement {
        val flowElement = FlowElement(context,this,elem)
        flowElement.setAsTextView(text)
        arrayList.add(flowElement)
        return flowElement
    }
    fun appendAddButton(callback:(FlowElement, TextView) -> Unit,elem: FlowElement?=null): FlowElement {
        val flowElement = FlowElement(context,this,elem)
        flowElement.setAsButton("+"){it, view->
            callback(it,view)
        }
        flowElement.data["text"]="+"
        arrayList.add(flowElement)
        return flowElement
    }
    fun appendWaveTextview(text:String,elem: FlowElement?=null,connector:Boolean=false,data: HashMap<String, Any>?=null,callback:(FlowElement, WaveTextView) -> Unit): FlowElement {
        val flowElement = FlowElement(context,this,elem,data)
        flowElement.setAsWaveTextview(text,connector){it,view->
            callback(it,view)
        }
        arrayList.add(flowElement)
        return flowElement
    }



    fun removedElement(flowElement: FlowElement){
        flowElement.removed()
        arrayList.remove(flowElement)
    }

    fun findAdd(): View? {
        arrayList.forEach {
            if(it.type == 0 && it.data["text"] == "+"){
                return it.getFirstView()
            }
        }
       return null
    }
    fun findFirstWave(): View? {
        arrayList.forEach {
            if(it.type == 2 && it.firstWaveTextView){
                return it.getFirstView()
            }
        }
        return null
    }

    fun getViewMap(): ArrayList<FlowElement> {
        return arrayList
    }

    fun appendButton(text: String,elem: FlowElement? = null): FlowElement {
        val flowElement = FlowElement(context,this,elem)
        flowElement.setAsButton(text){it, view->
        }
        arrayList.add(flowElement)
        return flowElement
    }


}