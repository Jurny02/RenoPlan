package dev.jurczyk.renoplan.ui.roomeditor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jurczyk.renoplan.domain.geometry.Point
import dev.jurczyk.renoplan.domain.geometry.outlineVertices
import dev.jurczyk.renoplan.domain.model.Millimetres
import dev.jurczyk.renoplan.domain.model.Turn
import dev.jurczyk.renoplan.domain.model.Wall
import dev.jurczyk.renoplan.ui.theme.RenoPlanTheme
import org.jetbrains.compose.resources.stringResource
import renoplan.shared.generated.resources.Res
import renoplan.shared.generated.resources.room_outline_preview
import kotlin.math.min

/**
 * Read-only drawing of the outline with wall numbers, so the user can match numbers to real walls.
 * The start point is marked with a dot; an outline that does not close shows the gap.
 */
@Composable
fun OutlinePreview(
    walls: List<Wall>,
    modifier: Modifier = Modifier,
) {
    val points = remember(walls) { outlineVertices(walls) }
    val textMeasurer = rememberTextMeasurer()
    val colors = MaterialTheme.colorScheme
    val style =
        OutlineStyle(
            line = colors.primary,
            start = colors.tertiary,
            labelBackground = colors.surface,
            label = MaterialTheme.typography.labelLarge.copy(color = colors.onSurface),
        )
    val description = stringResource(Res.string.room_outline_preview)
    Canvas(modifier = modifier.fillMaxWidth().height(220.dp).semantics { contentDescription = description }) {
        if (points.size < 2) return@Canvas
        val project = Projection(points, size, padding = 28.dp.toPx())
        drawOutline(points.map(project::toOffset), style, strokeWidth = 3.dp.toPx(), startRadius = 5.dp.toPx())
        walls.forEachIndexed { index, wall ->
            val middle = (project.toOffset(points[index]) + project.toOffset(points[index + 1])) / 2f
            drawWallNumber(wall.number, middle, textMeasurer, style, backgroundRadius = 11.dp.toPx())
        }
    }
}

private class OutlineStyle(
    val line: Color,
    val start: Color,
    val labelBackground: Color,
    val label: TextStyle,
)

/** Fits millimetre points into the canvas, keeping proportions; +Y in the outline points up. */
private class Projection(
    points: List<Point>,
    canvas: Size,
    padding: Float,
) {
    private val minX = points.minOf { it.x }
    private val maxY = points.maxOf { it.y }
    private val spanX = (points.maxOf { it.x } - minX).coerceAtLeast(1)
    private val spanY = (maxY - points.minOf { it.y }).coerceAtLeast(1)
    private val scale = min((canvas.width - 2 * padding) / spanX, (canvas.height - 2 * padding) / spanY)
    private val left = (canvas.width - spanX * scale) / 2
    private val top = (canvas.height - spanY * scale) / 2

    fun toOffset(point: Point): Offset = Offset(left + (point.x - minX) * scale, top + (maxY - point.y) * scale)
}

private fun DrawScope.drawOutline(
    offsets: List<Offset>,
    style: OutlineStyle,
    strokeWidth: Float,
    startRadius: Float,
) {
    val path =
        Path().apply {
            moveTo(offsets.first().x, offsets.first().y)
            offsets.drop(1).forEach { lineTo(it.x, it.y) }
        }
    drawPath(path, style.line, style = Stroke(width = strokeWidth, join = StrokeJoin.Round))
    drawCircle(style.start, radius = startRadius, center = offsets.first())
}

private fun DrawScope.drawWallNumber(
    number: Int,
    center: Offset,
    textMeasurer: TextMeasurer,
    style: OutlineStyle,
    backgroundRadius: Float,
) {
    val layout = textMeasurer.measure(number.toString(), style.label)
    drawCircle(style.labelBackground, radius = backgroundRadius, center = center)
    drawText(layout, topLeft = center - Offset(layout.size.width / 2f, layout.size.height / 2f))
}

@Preview
@Composable
private fun OutlinePreviewPreview() {
    val walls =
        listOf(
            3_200 to Turn.RIGHT,
            2_500 to Turn.RIGHT,
            2_800 to Turn.RIGHT,
            400 to Turn.LEFT,
            400 to Turn.RIGHT,
            2_100 to Turn.RIGHT,
        ).mapIndexed { index, (length, turn) -> Wall(index + 1, Millimetres(length.toLong()), turn) }
    RenoPlanTheme {
        OutlinePreview(walls = walls)
    }
}
