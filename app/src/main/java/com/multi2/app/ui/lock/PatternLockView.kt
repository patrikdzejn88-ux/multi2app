package com.multi2.app.ui.lock

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color

/**
 * 3x3 pattern lock input — parity with the original's lockpattern view.
 * Emits the pattern as a dot-index string (e.g. "0124"); min 4 dots enforced
 * by the caller ("Connect at least four dots. Try again.").
 */
@Composable
fun PatternLockView(
    onPatternComplete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val dotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
    val selectedColor = MaterialTheme.colorScheme.primary

    var size by remember { mutableStateOf(IntSize.Zero) }
    var selected by remember { mutableStateOf(listOf<Int>()) }
    var current by remember { mutableStateOf<Offset?>(null) }

    fun centerOf(index: Int): Offset {
        val col = index % 3
        val row = index / 3
        val w = size.width / 3f
        val h = size.height / 3f
        return Offset(w * col + w / 2, h * row + h / 2)
    }

    fun hitTest(pos: Offset): Int? {
        if (size.width == 0) return null
        for (i in 0 until 9) {
            val c = centerOf(i)
            val radius = size.minDimension / 9f
            if ((pos - c).getDistance() <= radius) return i
        }
        return null
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .onSizeChanged { size = it }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { start ->
                        selected = emptyList()
                        hitTest(start)?.let { selected = listOf(it) }
                        current = start
                    },
                    onDrag = { change, _ ->
                        current = change.position
                        val hit = hitTest(change.position)
                        if (hit != null && hit !in selected) {
                            selected = selected + hit
                        }
                    },
                    onDragEnd = {
                        if (selected.isNotEmpty()) {
                            onPatternComplete(selected.joinToString("") { it.toString() })
                        }
                        selected = emptyList()
                        current = null
                    },
                    onDragCancel = {
                        selected = emptyList()
                        current = null
                    },
                )
            },
    ) {
        // lines between selected dots
        if (selected.size > 1) {
            for (i in 0 until selected.size - 1) {
                drawLine(
                    color = lineColor,
                    start = centerOf(selected[i]),
                    end = centerOf(selected[i + 1]),
                    strokeWidth = 8f,
                )
            }
        }
        // trailing line to finger
        val finger = current
        if (selected.isNotEmpty() && finger != null) {
            drawLine(
                color = lineColor.copy(alpha = 0.5f),
                start = centerOf(selected.last()),
                end = finger,
                strokeWidth = 8f,
            )
        }
        // dots
        for (i in 0 until 9) {
            val c = centerOf(i)
            val isSelected = i in selected
            drawCircle(
                color = if (isSelected) selectedColor else dotColor,
                radius = if (isSelected) 28f else 18f,
                center = c,
            )
            if (isSelected) {
                drawCircle(color = Color.White, radius = 10f, center = c)
            }
        }
    }
}
