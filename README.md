# 圆形轮盘选择器

**背景：产品需要对游戏的按键做成圆形，且可以下发，点击效果相当于操作按键**
***初期参照过市面上的开源，没有完全匹配要求的，最终还是自己动手做了一个，整理下了总体实现的思路和关键点***

*先上图和视频*

## 整体思路<br>
1.绘制扇形区域和中心圆形区域  <br>
2.手指触摸位置判断（中心，扇形区域），选中区域重新绘制背景色  <br>
3.绘制中心圆弧和扇形之间白色线条  <br>
4.扇形区域文字绘制  <br>
5.为了特效，设计给的一些背景图的绘制  <br>


##特别注意点：  
1.Android中扇形绘制起始点默认是水平方向顺时针方向，开始绘制  
2.为了方便计算,canvas最好先移动中心位置，原点坐标才会为（0，0）:  
    canvas.translate(mWRadius, mWRadius)  
###核心代码解析  
1.扇形绘制（无中心部分）： 1- 扇形  2-中心圆形  使用 Path.Op.DIFFERENCE 属性就是代表：  
   绘制图 = 图1--图1和图2的交集  
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
2.扇形区域的保存，由于扇形的path已经保存在 mRegionList,后面直接根据手指的(x,y)判断所在扇形区域

根据扇形的path设置
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
3.扇形中的文字绘制
```
      /**
     * 扇形画文字
     */
    private fun drawText(
        mCanvas: Canvas,
        textAngle: Float,
        kinds: String,
        mPaint: Paint,
        mRadius: Float
    ) {
        val rect = Rect()
        mPaint.textSize = 38f
        mPaint.getTextBounds(kinds, 0, kinds.length, rect)
        if (textAngle in 0.0..90.0) { //画布坐标系第一象限(数学坐标系第四象限)
            mCanvas.drawText(
                kinds,
                (mRadius * 0.6 * cos(Math.toRadians(textAngle.toDouble()))).toFloat(),
                (mRadius * 0.7 * sin(Math.toRadians(textAngle.toDouble()))).toFloat() + rect.height() / 2,
                mPaint
            )
        } else if (textAngle > 90 && textAngle <= 180) { //画布坐标系第二象限(数学坐标系第三象限)
            mCanvas.drawText(
                kinds,
                (-mRadius * 0.6 * cos(Math.toRadians(180 - textAngle.toDouble()))).toFloat(),
                (mRadius * 0.7 * sin(Math.toRadians(180 - textAngle.toDouble()))).toFloat() + rect.height() / 2,
                mPaint
            )
        } else if (textAngle > 180 && textAngle <= 270) { //画布坐标系第三象限(数学坐标系第二象限)
            mCanvas.drawText(
                kinds,
                (-mRadius * 0.6 * cos(Math.toRadians(textAngle - 180.toDouble()))).toFloat(),
                (-mRadius * 0.7 * sin(Math.toRadians(textAngle - 180.toDouble()))).toFloat() + rect.height() / 2,
                mPaint
            )
        } else { //画布坐标系第四象限(数学坐标系第一象限)
            mCanvas.drawText(
                kinds,
                (mRadius * 0.6 * cos(Math.toRadians(360 - textAngle.toDouble()))).toFloat(),
                (-mRadius * 0.7 * sin(Math.toRadians(360 - textAngle.toDouble()))).toFloat() + rect.height() / 2,
                mPaint
            )
        }
    }
```
4.圆形中心和弧形间线条的绘制(思路：根据角度找到内部圆形的坐标（x1,y2），在找到圆弧上的点(x2,y2)，path连起来，然后绘制线条)
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

5.中间文字的绘制和中心圆形位置选中和未选中用的是图片绘制，这个就没啥可说的了


