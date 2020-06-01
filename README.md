# 圆形轮盘选择器

**背景：产品需要对游戏的按键做成圆形，且可以下发，点击效果相当于操作按键**
***初期参照过市面上的开源，没有完全匹配要求的，最终还是自己动手做了一个，整理下了总体实现的思路和关键点***

*先上视频*
![Output sample](
https://github.com/androidsihai1/CircleWheelView/raw/master/%E8%BD%AE%E7%9B%98%E8%A7%86%E9%A2%91.gif)

## 整体思路<br>
1.绘制扇形区域和中心圆形区域  <br>
2.手指触摸位置判断（中心，扇形区域），选中区域重新绘制背景色  <br>
3.绘制中心圆弧和扇形之间白色线条  <br>
4.扇形区域文字绘制  <br>
5.为了特效，设计给的一些背景图的绘制  <br>


##特别注意点<br>
1.Android中扇形绘制起始点默认是水平方向顺时针方向，开始绘制  <br>
2.为了方便计算,canvas最好先移动中心位置（ canvas.translate(mWRadius, mWRadius)），原点坐标才会为（0，0） <br>   
###核心代码解析 <br>
1.扇形绘制（无中心部分）： 1- 扇形  2-中心圆形  使用 Path.Op.DIFFERENCE 属性就是代表 <br>
   绘制图 = 图1--图1和图2的交集 <br>
     * 获取绘制弧度所需要的path
```
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
```
2.扇形区域的保存，由于扇形的path已经保存在 mRegionList,后面直接根据手指的(x,y)判断所在扇形区域根据扇形的path设置
```
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
```
3.扇形中的文字绘制 (为了文字居中，首先获取角度的一半，获取中心圆形到圆弧2点的中间坐标，然后在中间坐标绘制文字)
```
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
```
4.圆形中心和弧形间线条的绘制(思路：根据角度找到内部圆形的坐标（x1,y2），在找到圆弧上的点(x2,y2)，path连起来，然后绘制线条)
```
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
```
5.中间文字的绘制和中心圆形位置选中和未选中用的是图片绘制，这个就没啥可说的了 <br>
6.其实该控件还支持合并，拆解，缩放，拖拽  ，但是为了简洁点，都已经被我干掉了  <br>


