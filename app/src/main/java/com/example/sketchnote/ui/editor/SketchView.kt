package com.example.sketchnote.ui.editor

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.max

enum class BrushType { PENCIL, BRUSH, MARKER, HIGHLIGHTER, CRAYON, ERASER, PEN }
class SketchView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    private var backgroundImage: Bitmap? = null
    var onDrawChanged: (() -> Unit)? = null
    var onHeightExpanded: ((Int) -> Unit)? = null

    private var lastRestoredHash: Int = -1

    private var canvasHeight = 220.dp2px(context)
    private val minHeight = 220.dp2px(context)
    private val expandThreshold = 60.dp2px(context)
    private val expandAmount = 120.dp2px(context)

    private val paintCache = HashMap<String, Paint>()

    private data class DrawAction(
        val path: Path,
        val color: Int,
        val strokeWidth: Float,
        val brushType: BrushType,
        val alpha: Int = 255,
        val points: MutableList<PathPoint> = mutableListOf()
    )

    private val actions = mutableListOf<DrawAction>()
    private val undoneActions = mutableListOf<DrawAction>()
    private var currentPath = Path()
    private val currentPoints = mutableListOf<PathPoint>()

    private var drawingBitmap: Bitmap? = null
    private var drawingCanvas: Canvas? = null
    private var needsFullRedraw = true

    private var lastX = 0f
    private var lastY = 0f
    private var currentVelocity = 0f
    private var lastTimestamp = 0L

    var isRulerMode = false
    var rulerAngleDeg = 0f
    private var rulerStartX = 0f
    private var rulerStartY = 0f

    private val matrix = Matrix()
    private val inverseMatrix = Matrix()
    private var scaleFactor = 1f
    private var lastPointerDist = 0f
    private var isZooming = false

    var strokeColor: Int = Color.BLACK
    var strokeWidth: Float = 8f
    var brushType: BrushType = BrushType.CRAYON

    fun canUndo() = actions.isNotEmpty()
    fun canRedo() = undoneActions.isNotEmpty()
    fun isEmpty() = actions.isEmpty()

    private fun Int.dp2px(ctx: Context) =
        (this * ctx.resources.displayMetrics.density).toInt()

    private fun makePaint(
        color: Int,
        width: Float,
        brush: BrushType,
        alpha: Int = 255,
        velocity: Float = 0f
    ): Paint {
        val key = "$color-$width-$brush-$alpha"
        if (velocity == 0f) {
            paintCache[key]?.let { return it }
        }

        val adjustedWidth = when {
            brush == BrushType.ERASER -> width * 2.5f
            velocity > 0f -> (width * (1f - (velocity / 8000f).coerceIn(0f, 0.5f))).coerceIn(width * 0.4f, width)
            else -> width
        }

        val adjustedAlpha = when {
            brush == BrushType.ERASER || brush == BrushType.HIGHLIGHTER -> alpha
            velocity > 0f -> (alpha * (1f - (velocity / 8000f).coerceIn(0f, 0.6f))).toInt().coerceIn(60, alpha)
            else -> alpha
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            this.strokeWidth = adjustedWidth
            this.alpha = adjustedAlpha
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND

            when (brush) {
                BrushType.CRAYON -> {
                    this.alpha = adjustedAlpha.coerceIn(160, 200)
                    strokeCap = Paint.Cap.ROUND
                }
                BrushType.PENCIL -> {
                    this.alpha = adjustedAlpha.coerceIn(150, 210)
                    this.strokeWidth = adjustedWidth * 0.8f
                }
                BrushType.HIGHLIGHTER -> {
                    strokeCap = Paint.Cap.SQUARE
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
                    this.alpha = 120
                    this.strokeWidth = width * 2.5f
                }
                BrushType.MARKER -> {
                    this.alpha = adjustedAlpha.coerceIn(200, 240)
                    this.strokeWidth = width * 1.2f
                }
                BrushType.BRUSH -> {
                    this.alpha = adjustedAlpha.coerceIn(150, 190)
                    this.strokeWidth = adjustedWidth * 1.1f
                }
                BrushType.ERASER -> {
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                }
                else -> {
                    this.alpha = adjustedAlpha
                }
            }
        }

        if (velocity == 0f) {
            paintCache[key] = paint
        }
        return paint
    }

    private fun ensureBuffer() {
        val w = width.coerceAtLeast(1)
        val h = canvasHeight.coerceAtLeast(1)
        if (drawingBitmap == null || drawingBitmap!!.width != w || drawingBitmap!!.height != h) {
            drawingBitmap?.recycle()
            drawingBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            drawingCanvas = Canvas(drawingBitmap!!)
            needsFullRedraw = true
        }
    }

    private fun redrawBuffer() {
        val canvas = drawingCanvas ?: return
        canvas.drawColor(Color.WHITE)

        backgroundImage?.let {
            canvas.drawBitmap(it, null, RectF(0f, 0f, width.toFloat(), canvasHeight.toFloat()), null)
        }

        actions.forEach { action ->
            canvas.drawPath(
                action.path,
                makePaint(action.color, action.strokeWidth, action.brushType, action.alpha)
            )
        }
        needsFullRedraw = false
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        needsFullRedraw = true
        ensureBuffer()
        redrawBuffer()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(w, canvasHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        ensureBuffer()
        if (needsFullRedraw) redrawBuffer()

        canvas.save()
        canvas.concat(matrix)

        drawingBitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }

        if (!currentPath.isEmpty) {
            canvas.drawPath(
                currentPath,
                makePaint(strokeColor, strokeWidth, brushType, velocity = currentVelocity)
            )
        }

        canvas.restore()

        if (isRulerMode) drawRuler(canvas)

        val bottomHintY = canvasHeight - expandThreshold / 2f
        val hintPaint = Paint().apply {
            color = Color.parseColor("#22AAAAAA")
            strokeWidth = 1f
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(8f, 8f), 0f)
        }
        canvas.drawLine(40f, bottomHintY, width - 40f, bottomHintY, hintPaint)
    }

    private fun drawRuler(canvas: Canvas) {
        val cx = width / 2f
        val cy = height * 0.75f
        val rulerW = width * 0.85f
        val rulerH = 44f

        canvas.save()
        canvas.rotate(rulerAngleDeg, cx, cy)

        val rulerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CCDDEEEE")
            style = Paint.Style.FILL
        }
        val rulerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#88AABBCC")
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#88667788")
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#88445566")
            textSize = 10f
            textAlign = Paint.Align.CENTER
        }

        val rect = RectF(cx - rulerW / 2, cy - rulerH / 2, cx + rulerW / 2, cy + rulerH / 2)
        canvas.drawRoundRect(rect, 8f, 8f, rulerPaint)
        canvas.drawRoundRect(rect, 8f, 8f, rulerBorderPaint)

        val startX = cx - rulerW / 2 + 10f
        val endX = cx + rulerW / 2 - 10f
        var x = startX
        var cmCount = 0
        while (x <= endX) {
            val tickH = if (cmCount % 10 == 0) rulerH * 0.5f
            else if (cmCount % 5 == 0) rulerH * 0.35f
            else rulerH * 0.2f
            canvas.drawLine(x, cy - tickH / 2, x, cy + tickH / 2, tickPaint)
            if (cmCount % 10 == 0 && cmCount > 0) {
                canvas.drawText("${cmCount / 10}", x, cy - rulerH * 0.25f - 2f, textPaint)
            }
            x += (rulerW - 20f) / 100f
            cmCount++
        }
        canvas.restore()
    }

    private fun mapToCanvas(x: Float, y: Float): FloatArray {
        val pts = floatArrayOf(x, y)
        inverseMatrix.mapPoints(pts)
        return pts
    }

    private fun snapToRuler(x: Float, y: Float): Pair<Float, Float> {
        if (!isRulerMode) return Pair(x, y)
        val cx = width / 2f
        val cy = height * 0.75f
        val angleRad = Math.toRadians(rulerAngleDeg.toDouble())
        val cos = Math.cos(angleRad).toFloat()
        val sin = Math.sin(angleRad).toFloat()
        val dx = x - cx
        val dy = y - cy
        val proj = dx * cos + dy * sin
        return Pair(cx + proj * cos, cy + proj * sin)
    }

    private fun checkAndExpandCanvas(y: Float) {
        val realY = y / scaleFactor - (matrix.let {
            val vals = FloatArray(9); it.getValues(vals); vals[Matrix.MTRANS_Y]
        }) / scaleFactor
        if (realY > canvasHeight - expandThreshold) {
            canvasHeight += expandAmount
            needsFullRedraw = true
            requestLayout()
            onHeightExpanded?.invoke(
                (canvasHeight / context.resources.displayMetrics.density).toInt()
            )
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.pointerCount == 2) {
            isZooming = true
            handleZoom(event)
            return true
        }
        if (isZooming && event.action == MotionEvent.ACTION_UP) {
            isZooming = false
            return true
        }
        if (isZooming) return true

        val rawX = event.x
        val rawY = event.y
        val mapped = mapToCanvas(rawX, rawY)
        var x = mapped[0]
        var y = mapped[1]

        if (isRulerMode) {
            val snapped = snapToRuler(x, y)
            x = snapped.first; y = snapped.second
        }

        val now = event.eventTime

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                undoneActions.clear()
                currentPath = Path()
                currentPath.moveTo(x, y)
                currentPoints.clear()
                currentPoints.add(PathPoint(x, y, PointType.MOVE))
                lastX = x; lastY = y
                lastTimestamp = now
                currentVelocity = 0f
                if (isRulerMode) { rulerStartX = x; rulerStartY = y }
                invalidate()
            }

            MotionEvent.ACTION_MOVE -> {
                val dt = (now - lastTimestamp).coerceAtLeast(1L)
                val dist = Math.hypot((x - lastX).toDouble(), (y - lastY).toDouble()).toFloat()
                currentVelocity = dist / dt * 1000f

                if (isRulerMode) {
                    currentPath = Path()
                    currentPath.moveTo(rulerStartX, rulerStartY)
                    currentPath.lineTo(x, y)
                } else {
                    val midX = (lastX + x) / 2f
                    val midY = (lastY + y) / 2f
                    currentPath.quadTo(lastX, lastY, midX, midY)
                }

                currentPoints.add(PathPoint(x, y, PointType.LINE))
                lastX = x; lastY = y
                lastTimestamp = now

                checkAndExpandCanvas(rawY)
                val margin = strokeWidth * 2 + 10f
                invalidate(
                    (rawX - margin).toInt().coerceAtLeast(0),
                    (rawY - margin).toInt().coerceAtLeast(0),
                    (rawX + margin).toInt().coerceAtMost(width),
                    (rawY + margin).toInt().coerceAtMost(height)
                )
            }

            MotionEvent.ACTION_UP -> {
                if (isRulerMode) {
                    currentPath = Path()
                    currentPath.moveTo(rulerStartX, rulerStartY)
                    currentPath.lineTo(x, y)
                    currentPoints.clear()
                    currentPoints.add(PathPoint(rulerStartX, rulerStartY, PointType.MOVE))
                    currentPoints.add(PathPoint(x, y, PointType.LINE))
                } else {
                    currentPath.lineTo(x, y)
                    currentPoints.add(PathPoint(x, y, PointType.LINE))
                }

                val finishedAction = DrawAction(
                    path = Path(currentPath),
                    color = strokeColor,
                    strokeWidth = strokeWidth,
                    brushType = brushType,
                    points = currentPoints.toMutableList()
                )
                actions.add(finishedAction)

                drawingCanvas?.drawPath(
                    finishedAction.path,
                    makePaint(finishedAction.color, finishedAction.strokeWidth, finishedAction.brushType, finishedAction.alpha)
                )

                currentPath = Path()
                currentPoints.clear()
                currentVelocity = 0f
                invalidate()
                onDrawChanged?.invoke()
            }
        }
        return true
    }

    private fun handleZoom(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_POINTER_DOWN -> {
                lastPointerDist = pointerDist(event)
            }
            MotionEvent.ACTION_MOVE -> {
                val dist = pointerDist(event)
                if (lastPointerDist > 0) {
                    val scale = dist / lastPointerDist
                    val newScale = (scaleFactor * scale).coerceIn(0.5f, 4f)
                    val pivotX = (event.getX(0) + event.getX(1)) / 2f
                    val pivotY = (event.getY(0) + event.getY(1)) / 2f
                    matrix.postScale(newScale / scaleFactor, newScale / scaleFactor, pivotX, pivotY)
                    matrix.invert(inverseMatrix)
                    scaleFactor = newScale
                    needsFullRedraw = true
                    invalidate()
                }
                lastPointerDist = dist
            }
        }
    }

    private fun pointerDist(event: MotionEvent): Float {
        val dx = event.getX(0) - event.getX(1)
        val dy = event.getY(0) - event.getY(1)
        return Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
    }

    fun getSerializableActions(): List<SerializableAction> =
        actions.map { a ->
            SerializableAction(
                points = a.points.toList(),
                color = a.color,
                strokeWidth = a.strokeWidth,
                brushType = a.brushType,
                alpha = a.alpha
            )
        }

    fun restoreFromSerializable(serialized: List<SerializableAction>) {
        val hash = serialized.hashCode()
        if (hash == lastRestoredHash) return
        lastRestoredHash = hash

        actions.clear()
        undoneActions.clear()
        paintCache.clear()

        var maxY = minHeight.toFloat()

        serialized.forEach { sa ->
            val path = Path()
            var prevX = 0f
            var prevY = 0f
            sa.points.forEachIndexed { _, pt ->
                when (pt.type) {
                    PointType.MOVE -> {
                        path.moveTo(pt.x, pt.y)
                        prevX = pt.x; prevY = pt.y
                        maxY = max(maxY, pt.y)
                    }
                    PointType.LINE -> {
                        val midX = (prevX + pt.x) / 2f
                        val midY = (prevY + pt.y) / 2f
                        path.quadTo(prevX, prevY, midX, midY)
                        prevX = pt.x; prevY = pt.y
                        maxY = max(maxY, pt.y)
                    }
                }
            }
            actions.add(
                DrawAction(
                    path = path,
                    color = sa.color,
                    strokeWidth = sa.strokeWidth,
                    brushType = sa.brushType,
                    alpha = sa.alpha,
                    points = sa.points.toMutableList()
                )
            )
        }

        if (maxY + expandThreshold > canvasHeight) {
            canvasHeight = (maxY + expandThreshold * 2).toInt()
            requestLayout()
        }

        needsFullRedraw = true
        invalidate()
    }

    fun toggleRuler() {
        isRulerMode = !isRulerMode
        invalidate()
    }

    fun rotateRuler(deltaDeg: Float) {
        rulerAngleDeg = (rulerAngleDeg + deltaDeg) % 360f
        invalidate()
    }

    fun undo() {
        if (canUndo()) {
            undoneActions.add(actions.removeAt(actions.lastIndex))
            paintCache.clear()
            needsFullRedraw = true
            invalidate()
            onDrawChanged?.invoke()
        }
    }

    fun redo() {
        if (canRedo()) {
            actions.add(undoneActions.removeAt(undoneActions.lastIndex))
            paintCache.clear()
            needsFullRedraw = true
            invalidate()
            onDrawChanged?.invoke()
        }
    }

    fun clear() {
        actions.clear()
        undoneActions.clear()
        paintCache.clear()
        canvasHeight = minHeight
        lastRestoredHash = -1
        needsFullRedraw = true
        requestLayout()
        invalidate()
        onDrawChanged?.invoke()
    }

    fun setBackgroundImage(bitmap: Bitmap) {
        backgroundImage = bitmap
        needsFullRedraw = true
        invalidate()
    }

    fun getColorAtPoint(x: Float, y: Float): Int {
        if (x < 0 || x >= width || y < 0 || y >= height) return Color.BLACK
        return getBitmap().getPixel(x.toInt(), y.toInt())
    }

    fun getBitmap(): Bitmap {
        val bmp = Bitmap.createBitmap(
            width.coerceAtLeast(1),
            canvasHeight.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        draw(Canvas(bmp))
        return bmp
    }
}