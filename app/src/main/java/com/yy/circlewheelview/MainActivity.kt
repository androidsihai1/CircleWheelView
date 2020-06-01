package com.yy.circlewheelview

import android.os.Bundle
import android.util.Log
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var circleWheel = CircleWheelView(this)
        root_view.addView(circleWheel)
        circleWheel.mTextList.add("abcdi")
        circleWheel.mTextList.add("你么")
        circleWheel.mTextList.add("么好")
        circleWheel.mTextList.add("你啊")
        circleWheel.mTextList.add("你啊")
        circleWheel.mTextList.add("你啊")
        //circleWheel.mTextList.add("你啊")
        //circleWheel.mTextList.add("你啊")
        val lp = circleWheel.layoutParams as RelativeLayout.LayoutParams
        lp.width = DenUtil.dip2px(this, 280f)
        lp.height = DenUtil.dip2px(this, 280f)
        lp.addRule(RelativeLayout.CENTER_IN_PARENT)
        circleWheel.mOnWheelClickListener = object : OnWheelClickListener {
            override fun onWheelClick(pos: Int) {
                Log.i(TAG, "onWheelClick pos= $pos")
            }
        }
    }

    companion object {
        const val TAG = "MainActivity"
    }
}