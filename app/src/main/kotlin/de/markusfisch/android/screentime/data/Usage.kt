package de.markusfisch.android.screentime.data

import android.graphics.*
import de.markusfisch.android.screentime.app.db
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private const val DAY_IN_MS = 86400000L

fun drawUsageChart(
	width: Int,
	height: Int,
	timestamp: Long,
	days: Int,
	lastDaysString: String,
	usagePaint: Paint,
	dialPaint: Paint,
	textPaint: Paint
): Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
	Canvas(this).drawUsageChartAt(
		width / 2f,
		height / 2f,
		min(width, height) / 2f,
		timestamp,
		days,
		lastDaysString,
		usagePaint,
		dialPaint,
		textPaint
	)
}

private val squareRect = RectF()
private fun Canvas.drawUsageChartAt(
	centerX: Float,
	centerY: Float,
	radius: Float,
	timestamp: Long,
	days: Int,
	lastDaysString: String,
	usagePaint: Paint,
	dialPaint: Paint,
	textPaint: Paint
) {
	drawCircle(centerX, centerY, radius, dialPaint)
	val seconds = drawRecordsBetween(
		startOfDay(timestamp - DAY_IN_MS * days),
		endOfDay(timestamp),
		squareRect.apply {
			set(
				centerX - radius,
				centerY - radius,
				centerX + radius,
				centerY + radius
			)
		},
		usagePaint
	)
	drawClockFace(centerX, centerY, radius, textPaint)
	drawCenter(
		centerX,
		centerY,
		radius * .45f,
		dialPaint,
		textPaint,
		timeRangeColloquial(seconds),
		lastDaysString
	)
}

private fun Canvas.drawRecordsBetween(
	from: Long,
	to: Long,
	rect: RectF,
	paint: Paint
): Long {
	var total = 0L
	fun drawPie(start: Long, duration: Long) {
		total += duration
		drawArc(
			rect,
			dayTimeToAngle(start - from) - 90f,
			dayTimeToAngle(duration),
			true,
			paint
		)
	}

	val lastStart = db.forEachRecordBetween(from, to) { start, duration ->
		drawPie(start, duration)
	}
	// Draw the ongoing record, if there's one.
	if (lastStart > 0L) {
		drawPie(lastStart, System.currentTimeMillis() - lastStart)
	}
	return total / 1000L
}

private fun dayTimeToAngle(ms: Long): Float = 360f / DAY_IN_MS * ms.toFloat()

private const val TAU = Math.PI + Math.PI
private const val PI2 = Math.PI / 2
private val numberBounds = Rect()
private fun Canvas.drawClockFace(
	centerX: Float,
	centerY: Float,
	radius: Float,
	textPaint: Paint
) {
	val numberRadius = radius * .85f
	val dotRadius = radius * .95f
	val dotSize = dotRadius * .01f
	textPaint.textSize = dotRadius * .1f
	val steps = 24
	val step = TAU / steps
	var angle = 0.0
	var i = steps
	do {
		val a = angle - PI2
		drawTextCentered(
			"$i",
			centerX + numberRadius * cos(a).toFloat(),
			centerY + numberRadius * sin(a).toFloat(),
			textPaint,
			numberBounds
		)
		i = (i + 1) % steps
		angle += step
	} while (i > 0)
	i = steps * 4
	val smallDotSize = dotSize * .5f
	val smallStep = step / 4f
	while (i > -1) {
		val a = angle - PI2
		drawCircle(
			centerX + dotRadius * cos(a).toFloat(),
			centerY + dotRadius * sin(a).toFloat(),
			if (i % 4 == 0) dotSize else smallDotSize,
			textPaint
		)
		angle += smallStep
		--i
	}
}

private fun Canvas.drawTextCentered(
	text: String,
	x: Float,
	y: Float,
	textPaint: Paint,
	textBounds: Rect
) {
	textPaint.getTextBounds(text, 0, text.length, textBounds)
	drawText(
		text,
		x - textBounds.centerX().toFloat(),
		y - textBounds.centerY().toFloat(),
		textPaint
	)
}

private val sumBounds = Rect()
private val daysBounds = Rect()
private val sumPaint = Paint()
private fun Canvas.drawCenter(
	centerX: Float,
	centerY: Float,
	radius: Float,
	dialPaint: Paint,
	textPaint: Paint,
	sumText: String,
	daysText: String
) {
	drawCircle(centerX, centerY, radius, dialPaint)
	sumPaint.apply {
		color = textPaint.color
		style = textPaint.style
		typeface = textPaint.typeface
		textSize = radius * .3f
		getTextBounds(sumText, 0, sumText.length, sumBounds)
	}
	textPaint.apply {
		textSize = radius * .2f
		getTextBounds(daysText, 0, daysText.length, daysBounds)
	}
	val half = (sumBounds.height() + daysBounds.height() * 1.75f) / 2f
	val top = centerY - half
	val bottom = centerY + half
	drawText(
		sumText,
		centerX - sumBounds.centerX(),
		top + sumBounds.height() / 2 - sumBounds.centerY(),
		sumPaint
	)
	drawText(
		daysText,
		centerX - daysBounds.centerX(),
		bottom - daysBounds.height() / 2 - daysBounds.centerY(),
		textPaint
	)
}
