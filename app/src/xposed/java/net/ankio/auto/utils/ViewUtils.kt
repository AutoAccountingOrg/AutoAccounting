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

import android.annotation.SuppressLint
import android.app.Activity
import android.content.res.Resources
import android.graphics.Rect
import android.os.SystemClock
import android.text.TextUtils
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.CheckedTextView
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.QuickContactBadge
import android.widget.RadioButton
import android.widget.RatingBar
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.Space
import android.widget.Spinner
import android.widget.Switch
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.SwitchCompat
import java.util.Random
import java.util.concurrent.atomic.AtomicInteger


/**
 * Created by Jason on 2017/9/9.
 */
object ViewUtils {
    private val sNextGeneratedId = AtomicInteger(1)
    @SuppressLint("NewApi")
    fun generateViewId(): Int {
        return View.generateViewId()
    }

    fun initId(view: View): Int {
        val id: Int = View.generateViewId()
        view.id = id
        return id
    }

    fun performActionClick(view: View) {
        var width = view.width
        var height = view.height
        if (width < 0) {
            width = 0
        }
        if (height < 0) {
            height = 0
        }
        var downTime = SystemClock.uptimeMillis()
        var eventTime = SystemClock.uptimeMillis() + 200
        val x = (if (width > 0) Random(downTime).nextInt(width) else 0).toFloat()
        val y = (if (height > 0) Random(eventTime).nextInt(height) else 0).toFloat()
        val metaState = 0
        var motionEvent = MotionEvent.obtain(
            downTime,
            eventTime,
            MotionEvent.ACTION_DOWN,
            x,
            y,
            metaState
        )
        view.dispatchTouchEvent(motionEvent)
        downTime = SystemClock.uptimeMillis() + 120
        eventTime = SystemClock.uptimeMillis() + 200
        motionEvent = MotionEvent.obtain(
            downTime,
            eventTime,
            MotionEvent.ACTION_UP,
            x,
            y,
            metaState
        )
        view.dispatchTouchEvent(motionEvent)
    }

    fun findViewByName(activity: Activity, packageName: String?, vararg names: String?): View? {
        val rootView = activity.window.decorView
        return findViewByName(rootView, packageName, *names)
    }

    fun findViewByName(rootView: View, packageName: String?, vararg names: String?): View? {
        val resources = rootView.resources
        for (name in names) {
            val id = resources.getIdentifier(name, "id", packageName)
            if (id == 0) {
                continue
            }
            val viewList: MutableList<View> = ArrayList()
            getChildViews(rootView as ViewGroup, id, viewList)
            val outViewListSize = viewList.size
            if (outViewListSize == 1) {
                return viewList[0]
            } else if (outViewListSize > 1) {
                for (view in viewList) {
                    if (view.isShown) {
                        return view
                    }
                }
                return viewList[0]
            }
        }
        return null
    }

    fun findViewByText(rootView: View, vararg names: String): View? {
        for (name in names) {
            val viewList: MutableList<View> = ArrayList()
            getChildViews(rootView as ViewGroup, name, viewList)
            val outViewListSize = viewList.size
            if (outViewListSize == 1) {
                return viewList[0]
            } else if (outViewListSize > 1) {
                for (view in viewList) {
                    if (view.isShown) {
                        return view
                    }
                }
                return viewList[0]
            }
        }
        return null
    }

    private var sRecycleViewClz: Class<*>? = null
    private fun getViewBaseDesc(view: View): String {
        if (sRecycleViewClz == null) {
            try {
                sRecycleViewClz = Class.forName("android.support.v7.widget.RecyclerView")
            } catch (_: ClassNotFoundException) {
            }
        }
        if (view is FrameLayout) {
            return FrameLayout::class.java.name
        } else if (view is RatingBar) {
            return RatingBar::class.java.name
        } else if (view is SeekBar) {
            return SeekBar::class.java.name
        } else if (view is TableLayout) {
            return TableLayout::class.java.name

        } else if (view is TableRow) {
            return TableRow::class.java.name
        } else if (view is LinearLayout) {
            return LinearLayout::class.java.name
        } else if (view is RelativeLayout) {
            return RelativeLayout::class.java.name
        } else if (view is GridLayout) {
            return GridLayout::class.java.name
        } else if (view is CheckBox) {
            return CheckBox::class.java.name
        } else if (view is RadioButton) {
            return RadioButton::class.java.name
        } else if (view is CheckedTextView) {
            return CheckedTextView::class.java.name
        } else if (view is Spinner) {
            return Spinner::class.java.name
        } else if (view is ProgressBar) {
            return ProgressBar::class.java.name
        } else if (view is QuickContactBadge) {
            return QuickContactBadge::class.java.name
        } else if (view is SwitchCompat) {
            return SwitchCompat::class.java.name
        } else if (view is Switch) {
            return Switch::class.java.name
        } else if (view is Space) {
            return Space::class.java.name
        } else if (view is TextView) {
            return TextView::class.java.name
        } else if (view is AppCompatImageView) {
            return AppCompatImageView::class.java.name
        } else if (view is ImageView) {
            return ImageView::class.java.name
        } else if (view is ListView) {
            return ListView::class.java.name
        } else if (view is GridView) {
            return ListView::class.java.name
        } else if (sRecycleViewClz != null && view.javaClass.isAssignableFrom(sRecycleViewClz)) {
            return sRecycleViewClz!!.name
        }
        return view.javaClass.name
    }

    fun getViewInfo(view: View): String {
        val stringBuffer = StringBuffer()
        stringBuffer.append(view.toString())
        stringBuffer.append(" type:").append(getViewBaseDesc(view))
        stringBuffer.append(" clz:").append(view.javaClass.name)
        if (view is EditText) {
            stringBuffer.append(" text:").append(view.text).append(" hint:").append(view.hint)
        } else if (view is TextView) {
            stringBuffer.append(" text:").append(view.text)
        }
        val location = intArrayOf(0, 0)
        view.getLocationOnScreen(location)
        stringBuffer.append(" cor x:").append(location[0]).append(" y:").append(location[1])
        val desc = view.contentDescription
        if (!TextUtils.isEmpty(desc)) {
            stringBuffer.append(" desc:").append(desc)
        }
        stringBuffer.append(" tag:").append(view.tag)
        if (view is ViewGroup) {
            stringBuffer.append(" child:").append(view.childCount)
        }
        return stringBuffer.toString()
    }



    fun getChildViews(parent: ViewGroup, text: String, outList: MutableList<View>) {
        for (i in parent.childCount - 1 downTo 0) {
            val child = parent.getChildAt(i) ?: continue
            if (child is EditText) {
                if (text == (child as TextView).text.toString()) {
                    outList.add(child)
                } else if (text == child.hint.toString()) {
                    outList.add(child)
                }
            } else if (child is TextView) {
                if (text == child.text.toString()) {
                    outList.add(child)
                }
            }
            if (child is ViewGroup) {
                getChildViews(child, text, outList)
            }
        }
    }

    fun getChildViews(parent: ViewGroup, id: Int, outList: MutableList<View>) {
        for (i in parent.childCount - 1 downTo 0) {
            val child = parent.getChildAt(i) ?: continue
            if (id == child.id) {
                outList.add(child)
            }
            if (child is ViewGroup) {
                getChildViews(child, id, outList)
            } else {
            }
        }
    }

    fun getChildViewsByType(parent: ViewGroup, type: String?, outList: MutableList<View?>) {
        for (i in parent.childCount - 1 downTo 0) {
            val child = parent.getChildAt(i) ?: continue
            if (child.javaClass.name.contains(type!!)) {
                outList.add(child)
            }
            if (child is ViewGroup) {
                getChildViewsByType(child, type, outList)
            }
        }
    }

    fun getChildViews(parent: ViewGroup, outList: MutableList<View?>) {
        for (i in parent.childCount - 1 downTo 0) {
            val child = parent.getChildAt(i) ?: continue
            outList.add(child)
            if (child is ViewGroup) {
                getChildViews(child, outList)
            }
        }
    }





    fun getViewYPosInScreen(v: View): Int {
        val location = intArrayOf(0, 0)
        v.getLocationOnScreen(location)
        return location[1]
    }

    fun removeFromSuperView(v: View) {
        val parentView = v.parent ?: return
        if (parentView is ViewGroup) {
            parentView.removeView(v)
        }
    }

    fun viewsDesc(childView: List<View>): String {
        val stringBuffer = StringBuffer()
        for (view in childView) {
            stringBuffer.append(getViewInfo(view)).append("\n")
        }
        return stringBuffer.toString()
    }

    /**
     *
     * @param viewGroup
     * @param childView
     * @return found >= 0, not found -1
     */
    fun findChildViewPosition(viewGroup: ViewGroup, childView: View): Int {
        val childViewCount = viewGroup.childCount
        for (i in 0 until childViewCount) {
            val view = viewGroup.getChildAt(i)
            if (view === childView) {
                return i
            }
        }
        return -1
    }


    fun isShown(v: View): Boolean {
        val r = Rect()
        v.getGlobalVisibleRect(r)
        return !(r.left == 0 && r.right == 0 && r.top == 0 && r.bottom == 0)
    }

    fun isShownInScreen(v: View): Boolean {
        val r = Rect()
        if (!v.getGlobalVisibleRect(r)) {
            return false
        }
        if (r.left == 0 && r.right == 0 && r.top == 0 && r.bottom == 0) {
            return false
        }
        val metrics = Resources.getSystem().displayMetrics
        return Rect.intersects(r, Rect(0, 0, metrics.widthPixels, metrics.heightPixels))
    }

    fun isViewVisibleInScreen(view: View): Boolean {
        if (!isShown(view)) {
            return false
        }
        if (!view.isAttachedToWindow) {
            return false
        }
        if (view.alpha == 0f) {
            return false
        }
        return if (view.width <= 0 && view.height <= 0) {
            false
        } else view.windowVisibility == View.VISIBLE
    }

    fun getTopestView(view: View): ViewGroup? {
        return getTopestView(view, null)
    }

    private fun getTopestView(view: View, current: ViewGroup?): ViewGroup? {
        val parent = view.rootView ?: return current
        return getTopestView(parent, parent as ViewGroup)
    }

    fun relayout(view: View) {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(view.width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(view.height, View.MeasureSpec.EXACTLY)
        )
        view.layout(view.left, view.top, view.right, view.bottom)
    }

    fun <T : ViewGroup?> findParentViewByClass(view: View, clz: Class<T>): T? {
        val parentView = view.parent ?: return null
        if (clz.isAssignableFrom(parentView.javaClass)) {
            return parentView as T
        }
        return if (parentView is View) {
            findParentViewByClass(parentView as View, clz)
        } else null
    }

    fun findParentViewByClassNamePart(view: View, classPart: String?): ViewGroup? {
        val parentView = view.parent ?: return null
        if (parentView.javaClass.name.contains(classPart!!)) {
            return parentView as ViewGroup
        }
        return if (parentView is ViewGroup) {
            findParentViewByClassNamePart(parentView, classPart)
        } else null
    }
}