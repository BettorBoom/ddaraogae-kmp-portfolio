package dev.ddaraogae.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

/** One day on a walk chart. [km] is the day's total distance; [today] draws the label orange. */
class ChartPoint(val label: String, val km: Float, val today: Boolean = false)

private val emptyYLabels = listOf("1km", "3km", "6km", "9km", "12km")
private val AxisWidth = 44.dp
private val EdgePadding = 14.dp
private val FlatEdge = 8.dp
private const val VisibleDays = 7

private fun oneDecimal(value: Float): String {
    val tenths = (value * 10f).roundToInt()
    return "${tenths / 10}.${tenths % 10}"
}

/** The original's value text: whole meters below 1 km, otherwise one decimal of km. */
internal fun chartValue(km: Float): String =
    if (km < 1f) "${floor(km * 1000f).toInt()}m" else oneDecimal(km) + "km"

/** Y axis ceiling like the original: round the maximum up (10 m / 0.1 km), then add 10%. */
internal fun chartCeiling(maxKm: Float): Float {
    val adjusted = if (maxKm < 1f) ceil(maxKm * 1000f / 10f) * 10f / 1000f * 1.1f
        else ceil(maxKm / 0.1f) * 0.1f * 1.1f
    return if (adjusted > 0f) adjusted else if (maxKm < 1f) 0.01f else 0.1f
}

/** Round tick values (1/2/5 x 10^n) from 0 up to [ceiling]: about five intervals. */
internal fun chartTicks(ceiling: Float): List<Float> {
    val raw = ceiling / 5f
    val magnitude = 10f.pow(floor(log10(raw)))
    val step = listOf(1f, 2f, 5f, 10f).map { it * magnitude }.first { it >= raw }
    return generateSequence(0f) { it + step }.takeWhile { it <= ceiling + step * 0.001f }.toList()
}

private fun tickText(km: Float, meters: Boolean): String =
    if (meters) "${(km * 1000f).roundToInt()}m" else oneDecimal(km) + "km"

private fun DrawScope.plotRange(): ClosedFloatingPointRange<Float> = 16.dp.toPx()..(size.height - 22.dp.toPx())

private fun ClosedFloatingPointRange<Float>.yFor(value: Float, min: Float, max: Float): Float =
    endInclusive - (value - min) / (max - min) * (endInclusive - start)

/**
 * The original's MPAndroidChart line chart: linear brown line, ringed points with value labels,
 * left axis only, gray grid on a white plot. [scrollable] pans seven days at a time (history).
 * With no walks it shows the fixed empty axes (1/3/6/9/12 km); the caller adds the empty chip.
 */
@Composable
fun WalkLineChart(
    points: List<ChartPoint>,
    hasData: Boolean,
    modifier: Modifier = Modifier,
    scrollable: Boolean = false,
    description: String = "산책 거리 그래프",
) {
    val colors = AppTheme.colors
    val measurer = rememberTextMeasurer()
    // MPAndroidChart sizes its text in dp, so the chart does not grow with the font scale.
    val density = LocalDensity.current
    val axisStyle = TextStyle(fontSize = with(density) { 10.dp.toSp() }, color = colors.black)
    val valueStyle = TextStyle(fontSize = with(density) { 9.dp.toSp() }, color = colors.black)
    val maxKm = points.maxOfOrNull { it.km } ?: 0f
    val ceiling = if (hasData) chartCeiling(maxKm) else 5f
    val ticks = if (hasData) chartTicks(ceiling) else listOf(1f, 2f, 3f, 4f, 5f)
    val labels = if (hasData) ticks.map { tickText(it, maxKm < 1f) } else emptyYLabels
    val min = if (hasData) 0f else 1f
    Row(modifier.background(colors.white).semantics { contentDescription = description }) {
        Canvas(Modifier.width(AxisWidth).fillMaxHeight()) {
            val plot = plotRange()
            labels.forEachIndexed { index, text ->
                val layout = measurer.measure(text, axisStyle)
                drawText(layout, topLeft = Offset(size.width - layout.size.width - 4.dp.toPx(),
                    plot.yFor(ticks[index], min, ceiling) - layout.size.height / 2f))
            }
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxHeight()) {
            val unit = (maxWidth - EdgePadding) / VisibleDays
            val contentWidth = if (scrollable) EdgePadding + unit * points.size else maxWidth
            val scroll = rememberScrollState()
            // The original opens on the first days; showing today avoids hiding the latest walks.
            val today = points.indexOfFirst { it.today }
            val unitPx = with(LocalDensity.current) { unit.toPx() }
            LaunchedEffect(today, unitPx) {
                if (scrollable && today >= 0) scroll.scrollTo(((today - 5) * unitPx).toInt().coerceAtLeast(0))
            }
            Box(Modifier.fillMaxHeight().then(if (scrollable) Modifier.horizontalScroll(scroll) else Modifier)) {
                Canvas(Modifier.width(contentWidth).fillMaxHeight().clipToBounds()) {
                    drawPlot(measurer, axisStyle, valueStyle, points, hasData, scrollable, ceiling, ticks, min, colors,
                        (if (scrollable) EdgePadding else FlatEdge).toPx(), unit.toPx())
                }
            }
        }
    }
}

private fun DrawScope.drawPlot(
    measurer: TextMeasurer, axisStyle: TextStyle, valueStyle: TextStyle, points: List<ChartPoint>, hasData: Boolean, scrollable: Boolean,
    ceiling: Float, ticks: List<Float>, min: Float, colors: AppColors, edge: Float, unit: Float,
) {
    val plot = plotRange()
    val gridColor = Color(0xFF888888)
    val gridWidth = 0.5.dp.toPx()
    ticks.forEach { value ->
        val y = plot.yFor(value, min, ceiling)
        drawLine(gridColor, Offset(0f, y), Offset(size.width, y), gridWidth)
    }
    val count = points.size
    val xs = List(count) { index ->
        when {
            scrollable -> edge + unit * index
            count > 1 -> edge + (size.width - 2 * edge) * index / (count - 1)
            else -> size.width / 2f
        }
    }
    // An empty history axis labels seven evenly spread days; a chart with data labels every day.
    val labelled = if (hasData || !scrollable || count <= VisibleDays) points.indices.toList()
        else (0 until VisibleDays).map { (it * (count - 1) / (VisibleDays - 1f)).roundToInt() }.distinct()
    labelled.forEach { index ->
        drawLine(gridColor, Offset(xs[index], plot.start), Offset(xs[index], plot.endInclusive), gridWidth)
        val style = if (points[index].today) axisStyle.copy(color = colors.orange) else axisStyle
        val layout = measurer.measure(points[index].label, style)
        val left = (xs[index] - layout.size.width / 2f).coerceIn(0f, (size.width - layout.size.width).coerceAtLeast(0f))
        drawText(layout, topLeft = Offset(left, plot.endInclusive + 6.dp.toPx()))
    }
    drawLine(gridColor, Offset(0f, plot.endInclusive), Offset(size.width, plot.endInclusive), gridWidth)
    if (!hasData) return
    val ys = points.map { plot.yFor(it.km, 0f, ceiling) }
    for (index in 0 until count - 1) {
        drawLine(colors.brown, Offset(xs[index], ys[index]), Offset(xs[index + 1], ys[index + 1]), 2.dp.toPx())
    }
    points.forEachIndexed { index, point ->
        // The original's circle color is a resource-id bug; the intended light_blue is used.
        drawCircle(Color(0xFF7598C9), 4.dp.toPx(), Offset(xs[index], ys[index]))
        drawCircle(colors.white, 2.dp.toPx(), Offset(xs[index], ys[index]))
        val layout = measurer.measure(chartValue(point.km), valueStyle)
        val left = (xs[index] - layout.size.width / 2f).coerceIn(0f, (size.width - layout.size.width).coerceAtLeast(0f))
        drawText(layout, topLeft = Offset(left, ys[index] - 6.dp.toPx() - layout.size.height))
    }
}
