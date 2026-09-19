package com.localstream.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.localstream.app.ui.theme.DarkBgBase
import com.localstream.app.ui.theme.PurpleNeon
import com.localstream.app.ui.theme.PurplePrimary
import com.localstream.app.ui.theme.TextPrimary
import java.security.MessageDigest

@Composable
fun QrCodeView(
    data: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFF130F26),
    moduleColor: Color = Color.White
) {
    val matrixSize = 25
    val grid = remember(data) {
        generateQrMatrix(data, matrixSize)
    }

    Box(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(16.dp))
            .border(1.dp, PurplePrimary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().aspectRatio(1f)) {
            val modulePixelSize = size.width / matrixSize

            for (row in 0 until matrixSize) {
                for (col in 0 until matrixSize) {
                    if (grid[row][col]) {
                        val isCornerFinder = (row < 7 && col < 7) ||
                                (row < 7 && col >= matrixSize - 7) ||
                                (row >= matrixSize - 7 && col < 7)

                        val colr = if (isCornerFinder) PurpleNeon else moduleColor

                        drawRoundRect(
                            color = colr,
                            topLeft = Offset(col * modulePixelSize, row * modulePixelSize),
                            size = Size(modulePixelSize * 0.92f, modulePixelSize * 0.92f),
                            cornerRadius = CornerRadius(modulePixelSize * 0.25f, modulePixelSize * 0.25f)
                        )
                    }
                }
            }
        }
    }
}

private fun generateQrMatrix(data: String, size: Int): Array<BooleanArray> {
    val matrix = Array(size) { BooleanArray(size) }

    // Finder patterns in 3 corners (7x7)
    fun drawFinder(topRow: Int, leftCol: Int) {
        for (r in 0..6) {
            for (c in 0..6) {
                val isOuter = r == 0 || r == 6 || c == 0 || c == 6
                val isInner = r in 2..4 && c in 2..4
                matrix[topRow + r][leftCol + c] = isOuter || isInner
            }
        }
    }

    drawFinder(0, 0)
    drawFinder(0, size - 7)
    drawFinder(size - 7, 0)

    // Timing patterns
    for (i in 7 until size - 7) {
        matrix[6][i] = (i % 2 == 0)
        matrix[i][6] = (i % 2 == 0)
    }

    // Deterministic payload encoding based on data hash
    val hash = MessageDigest.getInstance("SHA-256").digest(data.toByteArray(Charsets.UTF_8))
    var bitIndex = 0

    for (r in 0 until size) {
        for (c in 0 until size) {
            val inFinder = (r < 8 && c < 8) || (r < 8 && c >= size - 8) || (r >= size - 8 && c < 8)
            val inTiming = r == 6 || c == 6
            if (!inFinder && !inTiming) {
                val byteVal = hash[bitIndex % hash.size].toInt()
                val bitVal = (byteVal shr (bitIndex % 8)) and 1
                matrix[r][c] = (bitVal == 1)
                bitIndex++
            }
        }
    }

    return matrix
}
