package com.xero.xerocamera.Scanner.ScannerModule

import android.animation.ArgbEvaluator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.View

val Int.toPx: Int get() = (this * Resources.getSystem().displayMetrics.density).toInt()

class ScannerOverlay @JvmOverloads constructor(
  private val view: View?,
  private val hapticFeedBack: Int,
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

  private var scanSuccessful = false
  private val successColor = Color.GREEN // Or any color you prefer

  private lateinit var backgroundShape: Path
  private lateinit var qrScannerShape: Path
  private lateinit var qrScannerCornersShape: Path

  private val edgeLength = 30.toPx
  private var qrScannerWidth = 250.toPx
  private var qrScannerHeight = 250.toPx

  // VERTICAL POSITION 0f - 1f
  private val verticalOffset = 0.4f

  // HORIZONTAL POSITION 0f - 1f
  private val horizontalOffset = 0.5f

  // EDGES OF QR SCANNER
  private var xAxisLeftEdge = 0f
  private var xAxisRightEdge = 0f
  private var yAxisTopEdge = 0f
  private var yAxisBottomEdge = 0f

  // RADIUS
  private val radius = 10.toPx.toFloat()
  private val frameStrokeWidth = 5.toPx.toFloat()
  private val backgroundPaint = Paint().apply {
    setARGB(150, 0, 0, 0)
  }
  private val transparentPaint = Paint().apply {
    color = Color.TRANSPARENT
  }

  private fun animateSuccessColor() {
    val startWidth = qrScannerWidth
    val startHeight = qrScannerHeight
    val endWidth = 220.toPx
    val endHeight = 220.toPx
    
    val colorHolder = PropertyValuesHolder.ofObject(
      "color",
      ArgbEvaluator(),
      Color.WHITE,
      successColor
    )
    val widthHolder = PropertyValuesHolder.ofInt("width", startWidth, endWidth)
    val heightHolder = PropertyValuesHolder.ofInt("height", startHeight, endHeight)

    val animator = ValueAnimator().apply {
      setValues(colorHolder, widthHolder, heightHolder)
      duration = 100 // Duration in milliseconds
      addUpdateListener { animator ->
        framePaint.color = animator.getAnimatedValue("color") as Int
        qrScannerWidth = animator.getAnimatedValue("width") as Int
        qrScannerHeight = animator.getAnimatedValue("height") as Int
        invalidate()
      }
    }
    animator.start()
  }

  fun setScanSuccessful(successful: Boolean) {
    scanSuccessful = successful
    if (successful) {
      animateSuccessColor()
      provideHapticFeedback(view, hapticFeedBack)
    } else {
      framePaint.color = Color.WHITE
      qrScannerWidth = 250.toPx
      qrScannerHeight = 250.toPx
      invalidate()
    }
  }

  private val framePaint = Paint().apply {
    isAntiAlias = true
    strokeWidth = frameStrokeWidth
    style = Paint.Style.STROKE
  }

  private fun createBackgroundPath() = Path().apply {
    lineTo(right.toFloat(), 0f)
    lineTo(right.toFloat(), bottom.toFloat())
    lineTo(0f, bottom.toFloat())
    lineTo(0f, 0f)
    fillType = Path.FillType.EVEN_ODD
  }

  private fun createQrPath() = Path().apply {
    // Start at top-left corner
    moveTo(xAxisLeftEdge + radius, yAxisTopEdge)
    // Top edge and top-right corner
    lineTo(xAxisRightEdge - radius, yAxisTopEdge)
    arcTo(
      xAxisRightEdge - 2 * radius,
      yAxisTopEdge,
      xAxisRightEdge,
      yAxisTopEdge + 2 * radius,
      270f,
      90f,
      false
    )

    // Right edge and bottom-right corner
    lineTo(xAxisRightEdge, yAxisBottomEdge - radius)
    arcTo(
      xAxisRightEdge - 2 * radius,
      yAxisBottomEdge - 2 * radius,
      xAxisRightEdge,
      yAxisBottomEdge,
      0f,
      90f,
      false
    )

    // Bottom edge and bottom-left corner
    lineTo(xAxisLeftEdge + radius, yAxisBottomEdge)
    arcTo(
      xAxisLeftEdge,
      yAxisBottomEdge - 2 * radius,
      xAxisLeftEdge + 2 * radius,
      yAxisBottomEdge,
      90f,
      90f,
      false
    )

    // Left edge and top-left corner
    lineTo(xAxisLeftEdge, yAxisTopEdge + radius)
    arcTo(
      xAxisLeftEdge,
      yAxisTopEdge,
      xAxisLeftEdge + 2 * radius,
      yAxisTopEdge + 2 * radius,
      180f,
      90f,
      false
    )

    close() // This closes the path by connecting back to the start point
    fillType = Path.FillType.EVEN_ODD
  }

  private fun createCutoutCornersPath() = Path().apply {
    // Top-left corner
    moveTo(xAxisLeftEdge, yAxisTopEdge + edgeLength)
    lineTo(xAxisLeftEdge, yAxisTopEdge + radius)
    arcTo(
      xAxisLeftEdge,
      yAxisTopEdge,
      xAxisLeftEdge + 2 * radius,
      yAxisTopEdge + 2 * radius,
      180f,
      90f,
      false
    )
    lineTo(xAxisLeftEdge + edgeLength, yAxisTopEdge)

    // Top-right corner
    moveTo(xAxisRightEdge - edgeLength, yAxisTopEdge)
    lineTo(xAxisRightEdge - radius, yAxisTopEdge)
    arcTo(
      xAxisRightEdge - 2 * radius,
      yAxisTopEdge,
      xAxisRightEdge,
      yAxisTopEdge + 2 * radius,
      270f,
      90f,
      false
    )
    lineTo(xAxisRightEdge, yAxisTopEdge + edgeLength)

    // Bottom-right corner
    moveTo(xAxisRightEdge, yAxisBottomEdge - edgeLength)
    lineTo(xAxisRightEdge, yAxisBottomEdge - radius)
    arcTo(
      xAxisRightEdge - 2 * radius,
      yAxisBottomEdge - 2 * radius,
      xAxisRightEdge,
      yAxisBottomEdge,
      0f,
      90f,
      false
    )
    lineTo(xAxisRightEdge - edgeLength, yAxisBottomEdge)

    // Bottom-left corner
    moveTo(xAxisLeftEdge + edgeLength, yAxisBottomEdge)
    lineTo(xAxisLeftEdge + radius, yAxisBottomEdge)
    arcTo(
      xAxisLeftEdge,
      yAxisBottomEdge - 2 * radius,
      xAxisLeftEdge + 2 * radius,
      yAxisBottomEdge,
      90f,
      90f,
      false
    )
    lineTo(xAxisLeftEdge, yAxisBottomEdge - edgeLength)
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    canvas.apply {
      xAxisLeftEdge = width * horizontalOffset - qrScannerWidth / 2f
      xAxisRightEdge = width * horizontalOffset + qrScannerWidth / 2f
      yAxisTopEdge = height * verticalOffset - qrScannerHeight / 2f
      yAxisBottomEdge = height * verticalOffset + qrScannerHeight / 2f

      backgroundShape = createBackgroundPath()
      qrScannerShape = createQrPath()
      qrScannerCornersShape = createCutoutCornersPath()
      backgroundShape.addPath(qrScannerShape)

      drawPath(backgroundShape, backgroundPaint)
      drawPath(qrScannerShape, transparentPaint)
      if (!scanSuccessful) {
        framePaint.color = Color.WHITE
      }
      drawPath(qrScannerCornersShape, framePaint)
    }
  }

  fun provideHapticFeedback(view: View?, hapticFeedBack : Int) =
    view?.performHapticFeedback(
      hapticFeedBack, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
    )
}