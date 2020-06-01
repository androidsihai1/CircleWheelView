package com.yy.circlewheelview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Region
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

/**
 * Created by andy on 2020/5/23
 */
class CircleWheelView : View {

    var mTextList = mutableListOf<String>() //扇形中文字
    private lateinit var mSelectPaint: Paint //扇形选中区域绘制笔
    private lateinit var mTextPaint: Paint //文字绘制笔
    private lateinit var mArcPaint: Paint //扇形绘制笔
    private lateinit var mCenterBgPaint: Paint
    private lateinit var mCenterNormalBitmap: Bitmap //中心未点击下背景
    private lateinit var mCenterPressBitmap: Bitmap  //中心按下背景
    private lateinit var mBg: Bitmap //扇形展开时候背景
    var mWRadius = 0f //外圆形半径
    var mCenterRadiu = 0f //中心圆半径
    var mArcRadiu = 0f //扇形半径

    var mRectOut = RectF() //最外层
    var mRectIn = RectF() //中心
    var mArcRect = RectF() //扇形区域
    var mChangeAngel = 0f //角度增加值
    var mIsPress = false //是否中心被按住
    var mRegionList = mutableListOf<Region>() //扇形区域位置保存图
    var mOnWheelClickListener: OnWheelClickListener? = null //选中区域内回调
    var mLastArea = -1 //手指最后按下区域位置
    var mCenterStr = "轮盘"

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attributeSet: AttributeSet?) : super(context, attributeSet, -1) {
        initView()
    }

    private fun initView() {
        mSelectPaint = Paint()
        mSelectPaint.isAntiAlias = true
        mSelectPaint.style = Paint.Style.FILL
        mSelectPaint.color = Color.parseColor("#805977c6")

        mTextPaint = Paint()
        mTextPaint.isAntiAlias = true
        mTextPaint.color = Color.WHITE
        mTextPaint.textAlign = Paint.Align.CENTER

        mArcPaint = Paint()
        mArcPaint.isAntiAlias = true
        mArcPaint.color = Color.parseColor("#4d000000")

        mCenterBgPaint = Paint()
        mCenterBgPaint.isAntiAlias = true

        mCenterNormalBitmap =
            BitmapFactory.decodeResource(context.resources, R.mipmap.btn_normal_bg)
        mCenterPressBitmap =
            BitmapFactory.decodeResource(context.resources, R.mipmap.btn_press_bg)
        mBg = BitmapFactory.decodeResource(context.resources, R.mipmap.wheel_open_bg)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val modeW = MeasureSpec.getMode(widthMeasureSpec)
        val sizeW = MeasureSpec.getSize(widthMeasureSpec)
        var widht = if (modeW == MeasureSpec.EXACTLY) {
            sizeW
        } else {
            DenUtil.dip2px(context, 165f)
        }
        mWRadius = widht / 2f
        mCenterRadiu = 3 * mWRadius / 7
        mArcRadiu = 142 * mWRadius / 165f
        mRectOut.set(-mWRadius, -mWRadius, mWRadius, mWRadius)
        mRectIn.set(
            -mCenterRadiu * 2 / 3,
            -mCenterRadiu * 2 / 3,
            mCenterRadiu * 2 / 3,
            mCenterRadiu * 2 / 3
        )
        mArcRect.set(
            -mArcRadiu, -mArcRadiu, mArcRadiu, mArcRadiu
        )

        setMeasuredDimension(widht, widht)
        Log.i(TAG, "mWRadius=$mWRadius")
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.translate(mWRadius, mWRadius)
        mChangeAngel = 360f / mTextList.size
        if (mIsPress) {
            canvas.drawBitmap(
                mBg,
                Rect(0, 0, mBg.width, mBg.height),
                RectF(-mWRadius, -mWRadius, mWRadius, mWRadius),
                mCenterBgPaint
            )
            mRegionList.clear()
            for (i in mTextList.indices) {
                val startAngel = i * mChangeAngel
                val arcPath = getArcPath(mRectIn, mArcRect, startAngel, mChangeAngel)
                canvas.drawPath(arcPath, mArcPaint)
                //canvas.drawArc(mRectOut, startAngel, mChangeAngel - 0.5f, true, mArcPaint)
                drawText(
                    canvas,
                    startAngel + mChangeAngel / 2,
                    mTextList[i],
                    mTextPaint,
                    mArcRadiu,
                    mCenterRadiu * 2 / 3
                )
                mRegionList.add(getRegion(path = arcPath))
                drawLinePath(
                    canvas,
                    mArcRadiu,
                    mCenterRadiu * 2 / 3,
                    (startAngel + mChangeAngel).toDouble()
                )
                if (mLastArea == i) {
                    canvas.drawPath(arcPath, mSelectPaint)
                }
            }
        }
        if (mIsPress) {
            canvas.drawBitmap(
                mCenterPressBitmap,
                Rect(0, 0, mCenterPressBitmap.width, mCenterPressBitmap.height),
                RectF(-mCenterRadiu, -mCenterRadiu, mCenterRadiu, mCenterRadiu),
                mCenterBgPaint
            )
        } else {
            canvas.drawBitmap(
                mCenterNormalBitmap,
                Rect(0, 0, mCenterNormalBitmap.width, mCenterNormalBitmap.height),
                RectF(-mCenterRadiu, -mCenterRadiu, mCenterRadiu, mCenterRadiu),
                mCenterBgPaint
            )
        }
        drawCenterText(canvas, mTextPaint, mCenterStr, mCenterRadiu)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (isInCenter(event)) {
                    showRect(true)
                } else {
                    return super.onTouchEvent(event)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val inAreaPos = inAreaPos(event, mRegionList)
                if (inAreaPos == -1) {
                    if (mLastArea > -1) {
                        mLastArea = -1
                        showRect(true)
                    }
                } else {
                    if (inAreaPos != mLastArea) {
                        mOnWheelClickListener?.onWheelClick(inAreaPos)
                        invalidate()
                        mLastArea = inAreaPos
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                clearData()
                showRect(false)
            }
        }
        return true
    }

    /**
     * 是否手指范围在中心圆内
     */
    private fun isInCenter(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        Log.i(TAG, "x=$x  y=$y  mWRadius=$mWRadius  mCenterRadiu=$mCenterRadiu")
        return (x >= mWRadius - mCenterRadiu && x <= mCenterRadiu + mWRadius &&
            y >= mWRadius - mCenterRadiu && y <= mCenterRadiu + mWRadius)
    }


    /**
     * 扇形是否显示
     */
    private fun showRect(isShow: Boolean) {
        mIsPress = isShow
        invalidate()
    }

    /**
     * 扇形画文字
     */
    private fun drawText(
        mCanvas: Canvas,
        textAngle: Float,
        kinds: String,
        mPaint: Paint,
        mRadius: Float,
        mCenTer: Float
    ) {
        val rect = Rect()
        mPaint.textSize = 38f
        var fontMetrics = mPaint.fontMetrics
        val dy = (fontMetrics.bottom - fontMetrics.top) / 2f - fontMetrics.bottom
        mPaint.getTextBounds(kinds, 0, kinds.length, rect)
        val pointEnd = PointF()
        val pointStart = PointF()
        val pointCengter = PointF()
        var x = 0.0
        var y = 0.0
        if (textAngle in 0.0..90.0) { //画布坐标系第一象限(数学坐标系第四象限)
            x = cos(Math.toRadians(textAngle.toDouble()))
            y = sin(Math.toRadians(textAngle.toDouble()))
        } else if (textAngle > 90 && textAngle <= 180) { //画布坐标系第二象限(数学坐标系第三象限)
            x = (-cos(Math.toRadians(180 - textAngle.toDouble())))
            y = (sin(Math.toRadians(180 - textAngle.toDouble())))
        } else if (textAngle > 180 && textAngle <= 270) { //画布坐标系第三象限(数学坐标系第二象限)
            x = (-cos(Math.toRadians(textAngle - 180.toDouble())))
            y = (-sin(Math.toRadians(textAngle - 180.toDouble())))
        } else { //画布坐标系第四象限(数学坐标系第一象限)
            x = (cos(Math.toRadians(360 - textAngle.toDouble())))
            y = (-sin(Math.toRadians(360 - textAngle.toDouble())))
        }
        pointStart.set((x * mCenTer).toFloat(), (y * mCenTer).toFloat())
        pointEnd.set((x * mRadius).toFloat(), (y * mRadius).toFloat())
        pointCengter.set((pointEnd.x + pointStart.x) / 2, (pointEnd.y + pointStart.y) / 2)
        Log.i(TAG, "X= ${(pointEnd.x + pointStart.x) / 2}  y=${(pointEnd.y + pointStart.y) / 2}")

        mCanvas.drawText(
            kinds,
            pointCengter.x,
            pointCengter.y + dy,
            mPaint
        )
    }


    /**
     * 获取绘制弧度所需要的path
     *
     * @param in
     * @param out
     * @param startAngle
     * @param angle
     * @return
     */
    private fun getArcPath(
        inSide: RectF,
        out: RectF,
        startAngle: Float,
        angle: Float
    ): Path {
        val path1 = Path()
        path1.moveTo(inSide.centerX(), inSide.centerY())
        path1.addCircle(inSide.centerX(), inSide.centerY(), 2 * mCenterRadiu / 3, Path.Direction.CW)
        val path2 = Path()
        path2.moveTo(out.centerX(), out.centerY())
        path2.arcTo(out, startAngle, angle)
        val path = Path()
        path.op(path2, path1, Path.Op.DIFFERENCE)
        return path
    }

    /**
     * 圆形中心和弧形间线条的绘制
     */
    private fun drawLinePath(
        canvas: Canvas,
        radius: Float,
        radiusN: Float,
        angele: Double
    ) {
        var linePath = Path()
        val paint = Paint()
        paint.color = Color.parseColor("#4dd8d8d8")
        paint.style = Paint.Style.STROKE
        paint.isAntiAlias = true
        paint.strokeWidth = 3f
        when (angele) {
            in 0.0f..90.0f -> {
                linePath.moveTo(
                    radiusN * cos(Math.toRadians(angele).toFloat()),
                    -radiusN * sin(Math.toRadians(angele).toFloat())
                )
                linePath.lineTo(
                    radius * cos(Math.toRadians(angele).toFloat()),
                    -radius * sin(Math.toRadians(angele).toFloat())
                )
            }
            in 90.0f..180f -> {
                linePath.moveTo(
                    -radiusN * sin(Math.toRadians(angele - 90f).toFloat()),
                    -radiusN * cos(Math.toRadians(angele - 90).toFloat())
                )
                linePath.lineTo(
                    -radius * sin(Math.toRadians(angele - 90).toFloat()),
                    -radius * cos(Math.toRadians(angele - 90).toFloat())
                )
            }
            in 180.0f..270f -> {
                linePath.moveTo(
                    -radiusN * cos(Math.toRadians(angele - 180).toFloat()),
                    radiusN * sin(Math.toRadians(angele - 180).toFloat())
                )
                linePath.lineTo(
                    -radius * cos(Math.toRadians(angele - 180).toFloat()),
                    radius * sin(Math.toRadians(angele - 180).toFloat())
                )
            }
            in 270.0f..360f -> {
                linePath.moveTo(
                    radiusN * sin(Math.toRadians(angele - 270).toFloat()),
                    radiusN * cos(Math.toRadians(angele - 270).toFloat())
                )
                linePath.lineTo(
                    radius * sin(Math.toRadians(angele - 270).toFloat()),
                    radius * cos(Math.toRadians(angele - 270).toFloat())
                )
            }
        }
        canvas.drawPath(linePath, paint)
    }

    private fun getRegion(path: Path): Region {
        val re = Region()
        val rectF = RectF()
        path.computeBounds(rectF, true)
        re.setPath(
            path,
            Region(rectF.left.toInt(), rectF.top.toInt(), rectF.right.toInt(), rectF.bottom.toInt())
        )
        return re
    }

    /**
     * 判断扇形区域
     */
    private fun inAreaPos(event: MotionEvent, regions: MutableList<Region>): Int {
        val x: Float = event.x - mWRadius
        val y: Float = event.y - mWRadius
        for (i in mRegionList.indices) {
            if (regions[i].contains(x.toInt(), y.toInt())) {
                return i
            }
        }
        return -1
    }

    private fun drawCenterText(canvas: Canvas, paint: Paint, centerTitle: String, radius: Float) {
        paint.color = Color.WHITE
        paint.textSize = 40f
        var fontMetrics = paint.fontMetrics
        val dy = (fontMetrics.bottom - fontMetrics.top) / 2f - fontMetrics.bottom
        canvas.drawText(
            centerTitle,
            0f,
            dy,
            paint
        )
    }

    private fun clearData() {
        mLastArea = -1
        mRegionList.clear()
    }

    companion object {
        const val TAG = "CircleWheelView"
    }
}