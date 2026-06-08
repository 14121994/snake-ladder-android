package com.example.snakeladder

import android.annotation.SuppressLint
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.aspectRatio
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private data class BoardThemeStyle(
    val frameBackground: Color,
    val frameBorder: Color,
    val cellBorder: Color,
    val numberColor: Color,
    val cellPalette: List<Color>
)


private val vibrantBoardTheme = BoardThemeStyle(
    frameBackground = Color(0xFFFFF9F1),
    frameBorder = Color(0xFFE7D2B5),
    cellBorder = Color(0xFFD6C2A5),
    numberColor = Color(0xFF493425),
    cellPalette = listOf(
        Color(0xFFFFE0B2), // amber
        Color(0xFFC8E6C9), // green
        Color(0xFFB3E5FC), // cyan
        Color(0xFFE1BEE7)  // orchid
    )
)

private val premiumMutedBoardTheme = BoardThemeStyle(
    frameBackground = Color(0xFFF7F4EE),
    frameBorder = Color(0xFFD9D2C6),
    cellBorder = Color(0xFFCFC6B7),
    numberColor = Color(0xFF4A433A),
    cellPalette = listOf(
        Color(0xFFF2E7DA), // sand
        Color(0xFFDCE8DE), // sage
        Color(0xFFDDE5EE), // mist blue
        Color(0xFFE8DFEC)  // mauve gray
    )
)

private val festivalBoardTheme = BoardThemeStyle(
    frameBackground = Color(0xFFFFF7ED),
    frameBorder = Color(0xFFE5B85A),
    cellBorder = Color(0xFFD79B32),
    numberColor = Color(0xFF4B2B00),
    cellPalette = listOf(
        Color(0xFFFFCC80),
        Color(0xFFFFF59D),
        Color(0xFFFFAB91),
        Color(0xFFA5D6A7)
    )
)

private val monsoonBoardTheme = BoardThemeStyle(
    frameBackground = Color(0xFFE8F5F7),
    frameBorder = Color(0xFF8AB6BE),
    cellBorder = Color(0xFF83AEB8),
    numberColor = Color(0xFF12343B),
    cellPalette = listOf(
        Color(0xFFB2EBF2),
        Color(0xFFC8E6C9),
        Color(0xFFD7E7F7),
        Color(0xFFE0F2F1)
    )
)

private val highContrastBoardTheme = BoardThemeStyle(
    frameBackground = Color.White,
    frameBorder = Color(0xFF111111),
    cellBorder = Color(0xFF202020),
    numberColor = Color(0xFF050505),
    cellPalette = listOf(
        Color(0xFFFFFFFF),
        Color(0xFFE2F1FF),
        Color(0xFFFFF1B8),
        Color(0xFFDDF7E1)
    )
)

private const val redDieHighlightAlpha = 0.26f

private fun diceFaceGradientColors(skin: DiceSkinOption): List<Color> {
    return when (skin) {
        DiceSkinOption.CLASSIC_RED -> listOf(
            Color(0xFFF04A40).copy(alpha = 0.97f),
            Color(0xFFC82024).copy(alpha = 0.97f),
            Color(0xFF7A1117).copy(alpha = 0.98f)
        )
        DiceSkinOption.ROYAL_BLUE -> listOf(
            Color(0xFF42A5F5).copy(alpha = 0.97f),
            Color(0xFF1565C0).copy(alpha = 0.97f),
            Color(0xFF0D2B66).copy(alpha = 0.98f)
        )
        DiceSkinOption.GOLD -> listOf(
            Color(0xFFFFE082).copy(alpha = 0.97f),
            Color(0xFFFFB300).copy(alpha = 0.97f),
            Color(0xFF7A4F00).copy(alpha = 0.98f)
        )
    }
}

private fun diceEdgeColor(skin: DiceSkinOption): Color {
    return when (skin) {
        DiceSkinOption.CLASSIC_RED -> Color(0xFF4E090D)
        DiceSkinOption.ROYAL_BLUE -> Color(0xFF08224F)
        DiceSkinOption.GOLD -> Color(0xFF4B3000)
    }
}

private fun boardCellTokenSize(pieceCountInCell: Int): Dp {
    val count = pieceCountInCell.coerceIn(1, 4)
    return when (count) {
        1 -> 16.dp
        2 -> 14.dp
        3 -> 12.dp
        else -> 11.dp
    }
}

@Composable
internal fun DiceBadge(
    value: Int,
    isRolling: Boolean,
    enabled: Boolean,
    contentDescription: String = "Roll Dice",
    diceSkin: DiceSkinOption = DiceSkinOption.CLASSIC_RED,
    boxSize: Dp = 72.dp,
    outerHorizontalPadding: Dp = 20.dp,
    outerVerticalPadding: Dp = 10.dp,
    onClick: () -> Unit
) {
    val safeValue = value.coerceIn(1, 6)
    val rollingFrames = remember {
        listOf(
            VisibleDieFaces(top = 1, left = 2, right = 3),
            VisibleDieFaces(top = 4, left = 1, right = 5),
            VisibleDieFaces(top = 2, left = 6, right = 4),
            VisibleDieFaces(top = 5, left = 3, right = 1),
            VisibleDieFaces(top = 6, left = 4, right = 2),
            VisibleDieFaces(top = 3, left = 5, right = 6)
        )
    }
    val rollingTransition = rememberInfiniteTransition(label = "redDiceRollingMotion")
    val rollingPhase by rollingTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 525, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "redDiceRollingPhase"
    )
    val visualScale by animateFloatAsState(
        targetValue = if (isRolling) 1.05f else 1f,
        animationSpec = spring(dampingRatio = 0.58f, stiffness = 360f),
        label = "redDiceVisualScale"
    )
    val activeRollPhase = if (isRolling) rollingPhase else 0f
    val tumbleAngle = activeRollPhase * (2f * PI.toFloat())
    val doubleTumbleAngle = tumbleAngle * 2f
    val shiftX = if (isRolling) sin(tumbleAngle) * 5f else 0f
    val liftY = if (isRolling) (-2f - (abs(cos(doubleTumbleAngle)) * 5f)) else 0f
    val rollingFaceIndex = ((activeRollPhase * rollingFrames.size).toInt() % rollingFrames.size)
    val renderedFaces = rollingFrames[rollingFaceIndex]

    Box(
        modifier = Modifier
            .semantics { this.contentDescription = contentDescription }
            .testTag("dice_badge")
            .graphicsLayer {
                scaleX = visualScale
                scaleY = visualScale
                translationX = shiftX
                translationY = liftY
            }
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = outerHorizontalPadding, vertical = outerVerticalPadding)
                .size(boxSize)
        ) {
            if (isRolling) {
                RollingRedDie(
                    faces = renderedFaces,
                    rollPhase = rollingPhase,
                    diceSkin = diceSkin,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                StaticRedDie(
                    value = safeValue,
                    diceSkin = diceSkin,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun StaticRedDie(
    value: Int,
    diceSkin: DiceSkinOption,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(R.drawable.red_die_source),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp)
                .clip(RoundedCornerShape(18.dp))
                .graphicsLayer {
                    alpha = 0.24f
                    scaleX = 1.22f
                    scaleY = 1.12f
                    rotationZ = -5f
                }
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dieSize = size.minDimension * 0.82f
            val topLeft = Offset(
                x = (size.width - dieSize) / 2f,
                y = (size.height - dieSize) / 2f
            )
            val dieShape = androidx.compose.ui.geometry.Size(dieSize, dieSize)
            val cornerRadius = androidx.compose.ui.geometry.CornerRadius(dieSize * 0.18f, dieSize * 0.18f)

            drawOval(
                color = Color.Black.copy(alpha = 0.18f),
                topLeft = Offset(topLeft.x + dieSize * 0.08f, topLeft.y + dieSize * 0.86f),
                size = androidx.compose.ui.geometry.Size(dieSize * 0.84f, dieSize * 0.14f)
            )
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = diceFaceGradientColors(diceSkin),
                    start = topLeft,
                    end = Offset(topLeft.x + dieSize, topLeft.y + dieSize)
                ),
                topLeft = topLeft,
                size = dieShape,
                cornerRadius = cornerRadius
            )
            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = redDieHighlightAlpha), Color.Transparent),
                    center = Offset(topLeft.x + dieSize * 0.28f, topLeft.y + dieSize * 0.22f),
                    radius = dieSize * 0.58f
                ),
                topLeft = topLeft,
                size = dieShape,
                cornerRadius = cornerRadius
            )
            drawRoundRect(
                color = diceEdgeColor(diceSkin).copy(alpha = 0.72f),
                topLeft = topLeft,
                size = dieShape,
                cornerRadius = cornerRadius,
                style = Stroke(width = dieSize * 0.035f)
            )
            drawStaticDiePips(value, topLeft, dieSize)
        }
    }
}

private data class VisibleDieFaces(
    val top: Int,
    val left: Int,
    val right: Int
)

private data class DieVector3(
    val x: Float,
    val y: Float,
    val z: Float
)

private data class DieFaceDefinition(
    val value: Int,
    val center: DieVector3,
    val u: DieVector3,
    val v: DieVector3,
    val normal: DieVector3,
    val colors: List<Color>
)

private data class ProjectedDieFace(
    val definition: DieFaceDefinition,
    val corners: List<Offset>,
    val center: DieVector3,
    val u: DieVector3,
    val v: DieVector3,
    val averageDepth: Float,
    val visibility: Float
)

@Composable
private fun RollingRedDie(
    faces: VisibleDieFaces,
    rollPhase: Float,
    diceSkin: DiceSkinOption,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val rollAngle = rollPhase * 2f * PI.toFloat()
        val rotationX = 0.78f + (sin(rollAngle * 1.35f) * 0.62f)
        val rotationY = -0.74f + (cos(rollAngle * 1.55f) * 0.66f)
        val rotationZ = sin(rollAngle * 2.1f) * 0.18f
        val pulse = abs(sin(rollAngle))
        val cubeScale = size.minDimension * 0.29f
        val staticPipRadius = size.minDimension * 0.82f * 0.065f
        val projectionCenter = Offset(
            x = size.width / 2f,
            y = (size.height / 2f) + (size.minDimension * 0.015f)
        )
        val cameraDistance = 4.5f

        fun rotate(vector: DieVector3): DieVector3 {
            val cosX = cos(rotationX)
            val sinX = sin(rotationX)
            val afterX = DieVector3(
                x = vector.x,
                y = (vector.y * cosX) - (vector.z * sinX),
                z = (vector.y * sinX) + (vector.z * cosX)
            )
            val cosY = cos(rotationY)
            val sinY = sin(rotationY)
            val afterY = DieVector3(
                x = (afterX.x * cosY) + (afterX.z * sinY),
                y = afterX.y,
                z = (-afterX.x * sinY) + (afterX.z * cosY)
            )
            val cosZ = cos(rotationZ)
            val sinZ = sin(rotationZ)
            return DieVector3(
                x = (afterY.x * cosZ) - (afterY.y * sinZ),
                y = (afterY.x * sinZ) + (afterY.y * cosZ),
                z = afterY.z
            )
        }

        fun project(vector: DieVector3): Offset {
            val perspective = cameraDistance / (cameraDistance - vector.z)
            return Offset(
                x = projectionCenter.x + (vector.x * cubeScale * perspective),
                y = projectionCenter.y - (vector.y * cubeScale * perspective)
            )
        }

        fun roundedPathOf(points: List<Offset>, cornerRadius: Float): Path {
            if (points.size < 3) {
                return Path().apply {
                    if (points.isNotEmpty()) {
                        moveTo(points.first().x, points.first().y)
                        points.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                }
            }

            val starts = mutableListOf<Offset>()
            val ends = mutableListOf<Offset>()
            points.forEachIndexed { index, current ->
                val previous = points[(index - 1 + points.size) % points.size]
                val next = points[(index + 1) % points.size]
                val previousLength = current.distanceTo(previous).coerceAtLeast(0.001f)
                val nextLength = current.distanceTo(next).coerceAtLeast(0.001f)
                val previousAmount = minOf(cornerRadius, previousLength * 0.34f)
                val nextAmount = minOf(cornerRadius, nextLength * 0.34f)
                starts += Offset(
                    x = current.x + ((previous.x - current.x) * (previousAmount / previousLength)),
                    y = current.y + ((previous.y - current.y) * (previousAmount / previousLength))
                )
                ends += Offset(
                    x = current.x + ((next.x - current.x) * (nextAmount / nextLength)),
                    y = current.y + ((next.y - current.y) * (nextAmount / nextLength))
                )
            }

            return Path().apply {
                moveTo(ends.first().x, ends.first().y)
                for (index in 1 until points.size) {
                    lineTo(starts[index].x, starts[index].y)
                    quadraticBezierTo(
                        points[index].x,
                        points[index].y,
                        ends[index].x,
                        ends[index].y
                    )
                }
                lineTo(starts.first().x, starts.first().y)
                quadraticBezierTo(
                    points.first().x,
                    points.first().y,
                    ends.first().x,
                    ends.first().y
                )
                close()
            }
        }

        val edgeColor = diceEdgeColor(diceSkin)
        val faceColors = diceFaceGradientColors(diceSkin)
        val edgeWidth = size.minDimension * 0.018f
        fun vector(x: Float, y: Float, z: Float) = DieVector3(x, y, z)
        fun shifted(face: Int, shift: Int): Int {
            return ((face.coerceIn(1, 6) - 1 + shift) % 6) + 1
        }
        fun faceDefinition(
            value: Int,
            center: DieVector3,
            u: DieVector3,
            v: DieVector3,
            normal: DieVector3,
            colors: List<Color>
        ): DieFaceDefinition {
            return DieFaceDefinition(value = value, center = center, u = u, v = v, normal = normal, colors = colors)
        }

        val faceDefinitions = listOf(
            faceDefinition(
                value = faces.right,
                center = vector(0f, 0f, 1f),
                u = vector(1f, 0f, 0f),
                v = vector(0f, 1f, 0f),
                normal = vector(0f, 0f, 1f),
                colors = faceColors
            ),
            faceDefinition(
                value = shifted(faces.right, 3),
                center = vector(0f, 0f, -1f),
                u = vector(-1f, 0f, 0f),
                v = vector(0f, 1f, 0f),
                normal = vector(0f, 0f, -1f),
                colors = faceColors
            ),
            faceDefinition(
                value = faces.top,
                center = vector(0f, 1f, 0f),
                u = vector(1f, 0f, 0f),
                v = vector(0f, 0f, -1f),
                normal = vector(0f, 1f, 0f),
                colors = faceColors
            ),
            faceDefinition(
                value = shifted(faces.top, 3),
                center = vector(0f, -1f, 0f),
                u = vector(1f, 0f, 0f),
                v = vector(0f, 0f, 1f),
                normal = vector(0f, -1f, 0f),
                colors = faceColors
            ),
            faceDefinition(
                value = faces.left,
                center = vector(-1f, 0f, 0f),
                u = vector(0f, 0f, 1f),
                v = vector(0f, 1f, 0f),
                normal = vector(-1f, 0f, 0f),
                colors = faceColors
            ),
            faceDefinition(
                value = shifted(faces.left, 3),
                center = vector(1f, 0f, 0f),
                u = vector(0f, 0f, -1f),
                v = vector(0f, 1f, 0f),
                normal = vector(1f, 0f, 0f),
                colors = faceColors
            )
        )

        val projectedFaces = faceDefinitions.map { definition ->
            val corners = listOf(
                DieVector3(
                    x = definition.center.x - definition.u.x - definition.v.x,
                    y = definition.center.y - definition.u.y - definition.v.y,
                    z = definition.center.z - definition.u.z - definition.v.z
                ),
                DieVector3(
                    x = definition.center.x + definition.u.x - definition.v.x,
                    y = definition.center.y + definition.u.y - definition.v.y,
                    z = definition.center.z + definition.u.z - definition.v.z
                ),
                DieVector3(
                    x = definition.center.x + definition.u.x + definition.v.x,
                    y = definition.center.y + definition.u.y + definition.v.y,
                    z = definition.center.z + definition.u.z + definition.v.z
                ),
                DieVector3(
                    x = definition.center.x - definition.u.x + definition.v.x,
                    y = definition.center.y - definition.u.y + definition.v.y,
                    z = definition.center.z - definition.u.z + definition.v.z
                )
            ).map { rotate(it) }
            val rotatedNormal = rotate(definition.normal)
            ProjectedDieFace(
                definition = definition,
                corners = corners.map { project(it) },
                center = rotate(definition.center),
                u = rotate(definition.u),
                v = rotate(definition.v),
                averageDepth = corners.sumOf { it.z.toDouble() }.toFloat() / corners.size,
                visibility = rotatedNormal.z
            )
        }.filter { it.visibility > 0.015f }
            .sortedBy { it.averageDepth }

        drawOval(
            color = Color.Black.copy(alpha = 0.22f + (pulse * 0.06f)),
            topLeft = Offset(projectionCenter.x - (cubeScale * 1.18f), projectionCenter.y + (cubeScale * 0.85f)),
            size = androidx.compose.ui.geometry.Size(cubeScale * 2.36f, cubeScale * 0.35f)
        )

        projectedFaces.forEach { projectedFace ->
            val facePath = roundedPathOf(projectedFace.corners, cornerRadius = cubeScale * 0.26f)
            val first = projectedFace.corners.first()
            val third = projectedFace.corners[2]
            drawPath(
                path = facePath,
                brush = Brush.linearGradient(
                    colors = projectedFace.definition.colors,
                    start = first,
                    end = third
                )
            )
            val shade = (0.055f * (1f - projectedFace.visibility.coerceIn(0f, 1f))).coerceIn(0f, 0.055f)
            if (shade > 0f) {
                drawPath(facePath, Color.Black.copy(alpha = shade))
            }
            drawPath(
                path = facePath,
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = redDieHighlightAlpha), Color.Transparent),
                    center = projectedFace.corners.minBy { it.y },
                    radius = cubeScale * 1.2f
                )
            )
            drawProjectedDiePips(
                face = projectedFace.definition.value,
                center = projectedFace.center,
                u = projectedFace.u,
                v = projectedFace.v,
                project = ::project,
                pipRadius = staticPipRadius,
                edgeWidth = edgeWidth
            )
            drawPath(facePath, edgeColor.copy(alpha = 0.92f), style = Stroke(width = edgeWidth, cap = StrokeCap.Round))
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawProjectedDiePips(
    face: Int,
    center: DieVector3,
    u: DieVector3,
    v: DieVector3,
    project: (DieVector3) -> Offset,
    pipRadius: Float,
    edgeWidth: Float
) {
    fun point(localX: Float, localY: Float): DieVector3 {
        return DieVector3(
            x = center.x + (u.x * localX) + (v.x * localY),
            y = center.y + (u.y * localX) + (v.y * localY),
            z = center.z + (u.z * localX) + (v.z * localY)
        )
    }

    val spread = 0.46f
    val pipCenters = when (face.coerceIn(1, 6)) {
        1 -> listOf(point(0f, 0f))
        2 -> listOf(point(-spread, spread), point(spread, -spread))
        3 -> listOf(point(-spread, spread), point(0f, 0f), point(spread, -spread))
        4 -> listOf(point(-spread, spread), point(spread, spread), point(-spread, -spread), point(spread, -spread))
        5 -> listOf(point(-spread, spread), point(spread, spread), point(0f, 0f), point(-spread, -spread), point(spread, -spread))
        else -> listOf(point(-spread, spread), point(spread, spread), point(-spread, 0f), point(spread, 0f), point(-spread, -spread), point(spread, -spread))
    }

    pipCenters.forEach { pip ->
        val projectedCenter = project(pip)
        val projectedU = project(
            DieVector3(
                x = pip.x + (u.x * 0.13f),
                y = pip.y + (u.y * 0.13f),
                z = pip.z + (u.z * 0.13f)
            )
        )
        val projectedV = project(
            DieVector3(
                x = pip.x + (v.x * 0.13f),
                y = pip.y + (v.y * 0.13f),
                z = pip.z + (v.z * 0.13f)
            )
        )
        val pipWidth = projectedCenter
            .distanceTo(projectedU)
            .coerceIn(pipRadius * 0.96f, pipRadius * 1.06f)
        val pipHeight = projectedCenter
            .distanceTo(projectedV)
            .coerceIn(pipRadius * 0.96f, pipRadius * 1.06f)
        val topLeftOffset = Offset(projectedCenter.x - pipWidth, projectedCenter.y - pipHeight)
        val pipSize = androidx.compose.ui.geometry.Size(pipWidth * 2f, pipHeight * 2f)
        drawOval(
            color = Color(0x662F090B),
            topLeft = Offset(
                x = projectedCenter.x - (pipWidth * 1.08f) + (pipRadius * 0.18f),
                y = projectedCenter.y - (pipHeight * 1.08f) + (pipRadius * 0.18f)
            ),
            size = androidx.compose.ui.geometry.Size(pipWidth * 2.16f, pipHeight * 2.16f)
        )
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, Color(0xFFFFEFE0), Color(0xFFE0D2C4)),
                center = Offset(projectedCenter.x - (pipWidth * 0.24f), projectedCenter.y - (pipHeight * 0.24f)),
                radius = maxOf(pipWidth, pipHeight)
            ),
            topLeft = topLeftOffset,
            size = pipSize
        )
        drawOval(
            color = Color(0x662B080A),
            topLeft = topLeftOffset,
            size = pipSize,
            style = Stroke(width = edgeWidth * 0.65f)
        )
    }
}

private fun Offset.distanceTo(other: Offset): Float {
    val dx = x - other.x
    val dy = y - other.y
    return sqrt((dx * dx) + (dy * dy))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStaticDiePips(
    face: Int,
    topLeft: Offset,
    dieSize: Float
) {
    val left = topLeft.x + dieSize * 0.30f
    val centerX = topLeft.x + dieSize * 0.50f
    val right = topLeft.x + dieSize * 0.70f
    val top = topLeft.y + dieSize * 0.30f
    val centerY = topLeft.y + dieSize * 0.50f
    val bottom = topLeft.y + dieSize * 0.70f
    val pipRadius = dieSize * 0.065f
    val pipCenters = when (face.coerceIn(1, 6)) {
        1 -> listOf(Offset(centerX, centerY))
        2 -> listOf(Offset(left, top), Offset(right, bottom))
        3 -> listOf(Offset(left, top), Offset(centerX, centerY), Offset(right, bottom))
        4 -> listOf(Offset(left, top), Offset(right, top), Offset(left, bottom), Offset(right, bottom))
        5 -> listOf(Offset(left, top), Offset(right, top), Offset(centerX, centerY), Offset(left, bottom), Offset(right, bottom))
        else -> listOf(Offset(left, top), Offset(right, top), Offset(left, centerY), Offset(right, centerY), Offset(left, bottom), Offset(right, bottom))
    }

    pipCenters.forEach { center ->
        drawCircle(
            color = Color(0x662F090B),
            radius = pipRadius * 1.08f,
            center = Offset(center.x + pipRadius * 0.18f, center.y + pipRadius * 0.18f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, Color(0xFFFFEFE0), Color(0xFFE0D2C4)),
                center = Offset(center.x - pipRadius * 0.26f, center.y - pipRadius * 0.26f),
                radius = pipRadius * 1.45f
            ),
            radius = pipRadius,
            center = center
        )
    }
}

private fun resolveBoardTheme(option: BoardThemeOption): BoardThemeStyle {
    return when (option) {
        BoardThemeOption.VIBRANT -> vibrantBoardTheme
        BoardThemeOption.PREMIUM_MUTED -> premiumMutedBoardTheme
        BoardThemeOption.FESTIVAL -> festivalBoardTheme
        BoardThemeOption.MONSOON -> monsoonBoardTheme
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
internal fun Board(
    state: GameState,
    displayPositions: List<Float>,
    tokenVisualOverrides: Map<Int, Offset?>,
    ladderZoomEffect: LadderZoomEffect?,
    snakeZoomEffect: SnakeZoomEffect?,
    boardThemeOption: BoardThemeOption,
    tokenTrail: TokenTrailOption = TokenTrailOption.NONE,
    highContrastBoard: Boolean = false,
    focusHighlight: BoardFocusHighlight? = null,
    onInspectRoute: (isLadder: Boolean, start: Int, end: Int) -> Unit = { _, _, _ -> }
) {
    val boardLayout = BoardLayouts.byId(state.boardLayoutId)
    val snakes = boardLayout.snakes
    val ladders = boardLayout.ladders

    val currentTurnPlayerIndex = if (state.winnerIndex == null) {
        state.currentPlayerIndex
    } else {
        null
    }
    val currentTurnPlayerName = currentTurnPlayerIndex?.let { state.players[it].name }
    val currentTurnPosition = currentTurnPlayerIndex?.let { state.players.getOrNull(it)?.position }
    val boardTheme = if (highContrastBoard) highContrastBoardTheme else resolveBoardTheme(boardThemeOption)
    val boardSummary = buildList {
        add("Snake and Ladder board.")
        currentTurnPlayerIndex
            ?.let { state.players.getOrNull(it) }
            ?.let { add("Current turn: ${it.name} on cell ${it.position}.") }
        state.lastMovePlayerIndex
            ?.let { state.players.getOrNull(it) }
            ?.let { add("Last moved: ${it.name} on cell ${it.position}.") }
        add("${state.players.size} players on board.")
    }.joinToString(" ")

    Card(modifier = Modifier.fillMaxWidth()) {
        BoxWithConstraints(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
                .aspectRatio(1f, matchHeightConstraintsFirst = true)
                .clip(RoundedCornerShape(14.dp))
                .background(boardTheme.frameBackground)
                .border(1.dp, boardTheme.frameBorder, RoundedCornerShape(14.dp))
                .semantics { contentDescription = boardSummary }
        ) {
            val cellWidth = maxWidth / 10
            val cellHeight = maxHeight / 10
            Box(modifier = Modifier.fillMaxSize()) {
                BoardGridLayer(
                    boardCellHeight = cellHeight,
                    theme = boardTheme,
                    modifier = Modifier.fillMaxSize()
                )
                SnakeLadderLayer(
                    snakes = snakes,
                    ladders = ladders,
                    ladderZoomEffect = ladderZoomEffect,
                    snakeZoomEffect = snakeZoomEffect,
                    highContrastBoard = highContrastBoard,
                    modifier = Modifier.fillMaxSize()
                )
                BoardNumbersLayer(
                    cellWidth = cellWidth,
                    cellHeight = cellHeight,
                    theme = boardTheme,
                    highContrastBoard = highContrastBoard,
                    modifier = Modifier.fillMaxSize()
                )
                BoardFxLayer(
                    currentTurnPosition = currentTurnPosition,
                    activeTraps = state.activeTraps,
                    highContrastBoard = highContrastBoard,
                    modifier = Modifier.fillMaxSize()
                )
                TrapBadgesLayer(
                    activeTraps = state.activeTraps,
                    cellWidth = cellWidth,
                    cellHeight = cellHeight,
                    highContrastBoard = highContrastBoard,
                    modifier = Modifier.fillMaxSize()
                )
                SpecialTileBadgesLayer(
                    specialTiles = boardLayout.specialTiles,
                    cellWidth = cellWidth,
                    cellHeight = cellHeight,
                    highContrastBoard = highContrastBoard,
                    modifier = Modifier.fillMaxSize()
                )
                TokensLayer(
                    players = state.players,
                    displayPositions = displayPositions,
                    tokenVisualOverrides = tokenVisualOverrides,
                    currentTurnPlayerName = currentTurnPlayerName,
                    cellWidth = cellWidth,
                    cellHeight = cellHeight,
                    tokenTrail = tokenTrail,
                    highContrastBoard = highContrastBoard,
                    modifier = Modifier.fillMaxSize()
                )
                BoardFocusLayer(
                    highlight = focusHighlight,
                    cellWidth = cellWidth,
                    cellHeight = cellHeight,
                    highContrastBoard = highContrastBoard,
                    modifier = Modifier.fillMaxSize()
                )
                RouteTapTargetsLayer(
                    snakes = snakes,
                    ladders = ladders,
                    cellWidth = cellWidth,
                    cellHeight = cellHeight,
                    onInspectRoute = onInspectRoute,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun BoardFocusLayer(
    highlight: BoardFocusHighlight?,
    cellWidth: Dp,
    cellHeight: Dp,
    highContrastBoard: Boolean,
    modifier: Modifier = Modifier
) {
    if (highlight == null) return

    val labelAtTop = positionToRowCol(highlight.cell.coerceIn(1, 100)).first >= 5
    Box(modifier = modifier.testTag("board_focus_layer")) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cellWidthPx = size.width / 10f
            val cellHeightPx = size.height / 10f
            val ringColor = if (highContrastBoard) Color(0xFF0057B8) else Color(0xFFF59E0B)
            val fillColor = ringColor.copy(alpha = 0.14f)
            val radius = minOf(cellWidthPx, cellHeightPx) * 0.42f
            val secondaryRadius = minOf(cellWidthPx, cellHeightPx) * 0.34f

            fun drawFocusCell(cell: Int, selectedRadius: Float, strokeWidth: Float) {
                val center = cellCenter(cell, cellWidthPx, cellHeightPx)
                drawCircle(color = fillColor, radius = selectedRadius, center = center)
                drawCircle(
                    color = ringColor,
                    radius = selectedRadius,
                    center = center,
                    style = Stroke(width = strokeWidth)
                )
            }

            drawFocusCell(highlight.cell, radius, 4.2f)
            highlight.secondaryCell
                ?.takeIf { it != highlight.cell }
                ?.let { drawFocusCell(it, secondaryRadius, 3.2f) }
        }

        Text(
            text = highlight.label,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF3E2723),
            maxLines = 2,
            modifier = Modifier
                .align(if (labelAtTop) Alignment.TopCenter else Alignment.BottomCenter)
                .padding(horizontal = cellWidth * 0.5f, vertical = cellHeight * 0.18f)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.94f))
                .border(1.dp, Color(0xFFE0B761), RoundedCornerShape(999.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp)
                .testTag("board_focus_label")
        )
    }
}

@Composable
private fun RouteTapTargetsLayer(
    snakes: Map<Int, Int>,
    ladders: Map<Int, Int>,
    cellWidth: Dp,
    cellHeight: Dp,
    onInspectRoute: (isLadder: Boolean, start: Int, end: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val targetSize = minOf(cellWidth, cellHeight) * 0.72f
    Box(modifier = modifier) {
        ladders.forEach { (start, end) ->
            RouteTapTarget(
                cell = start,
                cellWidth = cellWidth,
                cellHeight = cellHeight,
                targetSize = targetSize,
                contentDescription = "Ladder from cell $start to $end. Tap to highlight endpoints.",
                testTag = "ladder_endpoint_${start}_${end}_start",
                onClick = { onInspectRoute(true, start, end) }
            )
            RouteTapTarget(
                cell = end,
                cellWidth = cellWidth,
                cellHeight = cellHeight,
                targetSize = targetSize,
                contentDescription = "Ladder from cell $start to $end. Tap to highlight endpoints.",
                testTag = "ladder_endpoint_${start}_${end}_end",
                onClick = { onInspectRoute(true, start, end) }
            )
        }
        snakes.forEach { (head, tail) ->
            RouteTapTarget(
                cell = head,
                cellWidth = cellWidth,
                cellHeight = cellHeight,
                targetSize = targetSize,
                contentDescription = "Snake from cell $head to $tail. Tap to highlight endpoints.",
                testTag = "snake_endpoint_${head}_${tail}_head",
                onClick = { onInspectRoute(false, head, tail) }
            )
            RouteTapTarget(
                cell = tail,
                cellWidth = cellWidth,
                cellHeight = cellHeight,
                targetSize = targetSize,
                contentDescription = "Snake from cell $head to $tail. Tap to highlight endpoints.",
                testTag = "snake_endpoint_${head}_${tail}_tail",
                onClick = { onInspectRoute(false, head, tail) }
            )
        }
    }
}

@Composable
private fun RouteTapTarget(
    cell: Int,
    cellWidth: Dp,
    cellHeight: Dp,
    targetSize: Dp,
    contentDescription: String,
    testTag: String,
    onClick: () -> Unit
) {
    val (row, col) = positionToRowCol(cell)
    Box(
        modifier = Modifier
            .offset(
                x = (cellWidth * col.toFloat()) + ((cellWidth - targetSize) / 2f),
                y = (cellHeight * row.toFloat()) + ((cellHeight - targetSize) / 2f)
            )
            .size(targetSize)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics { this.contentDescription = contentDescription }
            .testTag(testTag)
    )
}

@Composable
private fun BoardGridLayer(
    boardCellHeight: Dp,
    theme: BoardThemeStyle,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        repeat(10) { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(10) { col ->
                    GridCell(
                        row = row,
                        col = col,
                        position = rowColToPosition(row, col),
                        theme = theme,
                        modifier = Modifier
                            .weight(1f)
                            .height(boardCellHeight)
                    )
                }
            }
        }
    }
}


@Composable
private fun SnakeLadderLayer(
    snakes: Map<Int, Int>,
    ladders: Map<Int, Int>,
    ladderZoomEffect: LadderZoomEffect?,
    snakeZoomEffect: SnakeZoomEffect?,
    highContrastBoard: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cellWidth = size.width / 10f
        val cellHeight = size.height / 10f

        ladders.forEach { (start, end) ->
            val originalStartCenter = cellCenter(start, cellWidth, cellHeight)
            val originalEndCenter = cellCenter(end, cellWidth, cellHeight)
            val zoomingThisLadder = ladderZoomEffect?.start == start && ladderZoomEffect.end == end
            val zoomScale = if (zoomingThisLadder) ladderZoomEffect.scale else 1f
            val zoomAlpha = if (zoomingThisLadder) ladderZoomEffect.alpha else 1f
            val ladderCenter = Offset(
                x = (originalStartCenter.x + originalEndCenter.x) / 2f,
                y = (originalStartCenter.y + originalEndCenter.y) / 2f
            )
            fun zoomPoint(point: Offset): Offset {
                if (!zoomingThisLadder) return point
                return Offset(
                    x = ladderCenter.x + ((point.x - ladderCenter.x) * zoomScale),
                    y = ladderCenter.y + ((point.y - ladderCenter.y) * zoomScale)
                )
            }

            val startCenter = zoomPoint(originalStartCenter)
            val endCenter = zoomPoint(originalEndCenter)
            val railOffset = cellWidth * 0.12f * zoomScale

            val leftStart = Offset(startCenter.x - railOffset, startCenter.y)
            val leftEnd = Offset(endCenter.x - railOffset, endCenter.y)
            val rightStart = Offset(startCenter.x + railOffset, startCenter.y)
            val rightEnd = Offset(endCenter.x + railOffset, endCenter.y)

            if (zoomingThisLadder) {
                val glow = Color(0x55FFD54F).copy(alpha = zoomAlpha * 0.7f)
                val strokeScale = 1f + ((zoomScale - 1f) * 0.35f)
                drawLine(glow, leftStart, leftEnd, strokeWidth = 20f * strokeScale, cap = StrokeCap.Round)
                drawLine(glow, rightStart, rightEnd, strokeWidth = 20f * strokeScale, cap = StrokeCap.Round)
            }

            val railStrokeScale = 1f + ((zoomScale - 1f) * 0.35f)
            val ladderOutlineAlpha = if (highContrastBoard) 0.92f else 0.54f
            val ladderFillAlpha = if (highContrastBoard) 0.96f else 0.78f
            val ladderFill = if (highContrastBoard) Color(0xFF6D4C41) else Color(0xFF9A776A)
            drawLine(Color(0xFF1B1B1B).copy(alpha = zoomAlpha * ladderOutlineAlpha), leftStart, leftEnd, strokeWidth = 9f * railStrokeScale, cap = StrokeCap.Round)
            drawLine(Color(0xFF1B1B1B).copy(alpha = zoomAlpha * ladderOutlineAlpha), rightStart, rightEnd, strokeWidth = 9f * railStrokeScale, cap = StrokeCap.Round)
            drawLine(ladderFill.copy(alpha = zoomAlpha * ladderFillAlpha), leftStart, leftEnd, strokeWidth = 5.5f * railStrokeScale, cap = StrokeCap.Round)
            drawLine(ladderFill.copy(alpha = zoomAlpha * ladderFillAlpha), rightStart, rightEnd, strokeWidth = 5.5f * railStrokeScale, cap = StrokeCap.Round)

            for (step in 1..4) {
                val t = step / 5f
                val rungStart = Offset(
                    x = leftStart.x + (leftEnd.x - leftStart.x) * t,
                    y = leftStart.y + (leftEnd.y - leftStart.y) * t
                )
                val rungEnd = Offset(
                    x = rightStart.x + (rightEnd.x - rightStart.x) * t,
                    y = rightStart.y + (rightEnd.y - rightStart.y) * t
                )
                drawLine(Color(0xFF1B1B1B).copy(alpha = zoomAlpha * ladderOutlineAlpha), rungStart, rungEnd, strokeWidth = 5.5f * railStrokeScale, cap = StrokeCap.Round)
                drawLine(Color(0xFFE7D7CC).copy(alpha = zoomAlpha * ladderFillAlpha), rungStart, rungEnd, strokeWidth = 3.4f * railStrokeScale, cap = StrokeCap.Round)
            }

            // Endpoint markers make start/end of each mapped ladder obvious.
            drawCircle(Color(0xFF4E342E).copy(alpha = zoomAlpha), radius = 6f * railStrokeScale, center = startCenter)
            drawCircle(Color(0xFF4E342E).copy(alpha = zoomAlpha), radius = 6f * railStrokeScale, center = endCenter)
        }

        snakes.forEach { (head, tail) ->
            val originalHeadCenter = cellCenter(head, cellWidth, cellHeight)
            val originalTailCenter = cellCenter(tail, cellWidth, cellHeight)
            val zoomingThisSnake = snakeZoomEffect?.start == head && snakeZoomEffect.end == tail
            val snakeScale = if (zoomingThisSnake) snakeZoomEffect.scale else 1f
            val snakeAlpha = if (zoomingThisSnake) snakeZoomEffect.alpha else 1f
            val snakeCenter = Offset(
                x = (originalHeadCenter.x + originalTailCenter.x) / 2f,
                y = (originalHeadCenter.y + originalTailCenter.y) / 2f
            )
            fun zoomSnakePoint(point: Offset): Offset {
                if (!zoomingThisSnake) return point
                return Offset(
                    x = snakeCenter.x + ((point.x - snakeCenter.x) * snakeScale),
                    y = snakeCenter.y + ((point.y - snakeCenter.y) * snakeScale)
                )
            }
            val headCenter = zoomSnakePoint(originalHeadCenter)
            val tailCenter = zoomSnakePoint(originalTailCenter)

            // Build a smooth snake body path from head to tail with alternating bends.
            val steps = 7
            val points = mutableListOf<Offset>()
            points.add(headCenter)
            for (i in 1 until steps) {
                val t = i / steps.toFloat()
                val x = headCenter.x + (tailCenter.x - headCenter.x) * t
                val yBase = headCenter.y + (tailCenter.y - headCenter.y) * t
                val swayBase = cellWidth * 0.2f
                val sway = if (i % 2 == 0) swayBase else -swayBase
                points.add(Offset(x + sway, yBase))
            }
            points.add(tailCenter)

            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    val prev = points[i - 1]
                    val curr = points[i]
                    val midX = (prev.x + curr.x) / 2f
                    val midY = (prev.y + curr.y) / 2f
                    quadraticBezierTo(prev.x, prev.y, midX, midY)
                }
                lineTo(points.last().x, points.last().y)
            }

            val snakeStrokeScale = 1f + ((snakeScale - 1f) * 0.35f)
            if (zoomingThisSnake) {
                drawPath(
                    path,
                    Color(0x55FF8A80).copy(alpha = snakeAlpha * 0.75f),
                    style = Stroke(width = 20f * snakeStrokeScale, cap = StrokeCap.Round)
                )
            }

            val snakeOutline = if (highContrastBoard) Color(0xFF1B0000) else Color(0xFF4A0E0E)
            val snakeFill = if (highContrastBoard) Color(0xFFC62828) else Color(0xFFE53935)
            drawPath(path, snakeOutline.copy(alpha = snakeAlpha), style = Stroke(width = 15f * snakeStrokeScale, cap = StrokeCap.Round))
            drawPath(path, snakeFill.copy(alpha = snakeAlpha), style = Stroke(width = 10f * snakeStrokeScale, cap = StrokeCap.Round))

            // Spot pattern for a more realistic look.
            points.drop(1).dropLast(1).forEachIndexed { idx, p ->
                if (idx % 2 == 0) {
                    drawCircle(Color(0xFF7F1D1D).copy(alpha = snakeAlpha), radius = 2.6f * snakeStrokeScale, center = p)
                }
            }

            // Head + eyes + tongue.
            drawCircle(Color.White.copy(alpha = snakeAlpha * 0.92f), radius = 11.5f * snakeStrokeScale, center = headCenter)
            drawCircle(Color(0xFFD32F2F).copy(alpha = snakeAlpha), radius = 9f * snakeStrokeScale, center = headCenter)
            drawCircle(Color(0xFF2E2E2E).copy(alpha = snakeAlpha), radius = 1.4f * snakeStrokeScale, center = Offset(headCenter.x - 2.2f * snakeStrokeScale, headCenter.y - 1.8f * snakeStrokeScale))
            drawCircle(Color(0xFF2E2E2E).copy(alpha = snakeAlpha), radius = 1.4f * snakeStrokeScale, center = Offset(headCenter.x + 2.2f * snakeStrokeScale, headCenter.y - 1.8f * snakeStrokeScale))
            val tongueStart = Offset(headCenter.x, headCenter.y + 8f * snakeStrokeScale)
            drawLine(Color(0xFFB71C1C).copy(alpha = snakeAlpha), tongueStart, Offset(tongueStart.x - 3f * snakeStrokeScale, tongueStart.y + 5f * snakeStrokeScale), strokeWidth = 1.6f * snakeStrokeScale, cap = StrokeCap.Round)
            drawLine(Color(0xFFB71C1C).copy(alpha = snakeAlpha), tongueStart, Offset(tongueStart.x + 3f * snakeStrokeScale, tongueStart.y + 5f * snakeStrokeScale), strokeWidth = 1.6f * snakeStrokeScale, cap = StrokeCap.Round)

            drawCircle(snakeOutline.copy(alpha = snakeAlpha), radius = 5.8f * snakeStrokeScale, center = tailCenter)
            drawCircle(Color.White.copy(alpha = snakeAlpha * 0.86f), radius = 2.4f * snakeStrokeScale, center = tailCenter)
        }
    }
}

@Composable
private fun BoardNumbersLayer(
    cellWidth: Dp,
    cellHeight: Dp,
    theme: BoardThemeStyle,
    highContrastBoard: Boolean,
    modifier: Modifier = Modifier
) {
    val badgeWidth = minOf(cellWidth * 0.74f, 34.dp)
    val badgeHeight = minOf(cellHeight * 0.38f, 18.dp)
    val background = if (highContrastBoard) Color.White.copy(alpha = 0.98f) else theme.frameBackground.copy(alpha = 0.92f)
    val border = if (highContrastBoard) Color.Black.copy(alpha = 0.88f) else theme.frameBorder.copy(alpha = 0.76f)
    Box(modifier = modifier.clearAndSetSemantics { }) {
        (1..100).forEach { position ->
            val (row, col) = positionToRowCol(position)
            Box(
                modifier = Modifier
                    .offset(
                        x = (cellWidth * col.toFloat()) + 2.dp,
                        y = (cellHeight * row.toFloat()) + 2.dp
                    )
                    .width(badgeWidth)
                    .height(badgeHeight)
                    .clip(RoundedCornerShape(4.dp))
                    .background(background)
                    .border(0.55.dp, border, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = position.toString(),
                    fontSize = if (position >= 100) 7.sp else 8.sp,
                    lineHeight = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.numberColor
                )
            }
        }
    }
}

@Composable
private fun BoardFxLayer(
    currentTurnPosition: Int?,
    activeTraps: List<BoardTrap>,
    highContrastBoard: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cellWidth = size.width / 10f
        val cellHeight = size.height / 10f
        activeTraps.forEach { trap ->
            val trapCenter = cellCenter(trap.cell, cellWidth, cellHeight)
            val trapRadius = minOf(cellWidth, cellHeight) * 0.26f
            val trapFill = if (highContrastBoard) Color(0x99210044) else Color(0x668B1E3F)
            val trapStroke = if (highContrastBoard) Color(0xFF210044) else Color(0xFFE91E63)
            drawCircle(
                color = trapFill,
                radius = trapRadius,
                center = trapCenter
            )
            drawCircle(
                color = trapStroke,
                radius = trapRadius,
                center = trapCenter,
                style = Stroke(width = if (highContrastBoard) 4.2f else 3.2f)
            )
        }
        currentTurnPosition?.let { focusPosition ->
            val focusCenter = cellCenter(focusPosition, cellWidth, cellHeight)
            val radius = (minOf(cellWidth, cellHeight) * 0.38f)
            drawCircle(
                color = Color(0x66F59E0B),
                radius = radius,
                center = focusCenter,
                style = Stroke(width = 3.2f)
            )
        }
    }
}

@Composable
private fun TrapBadgesLayer(
    activeTraps: List<BoardTrap>,
    cellWidth: Dp,
    cellHeight: Dp,
    highContrastBoard: Boolean,
    modifier: Modifier = Modifier
) {
    val badgeSize = minOf(cellWidth, cellHeight) * 0.36f
    val background = if (highContrastBoard) Color(0xFF210044) else Color.White.copy(alpha = 0.96f)
    val foreground = if (highContrastBoard) Color.White else Color(0xFF8B1E3F)
    val border = if (highContrastBoard) Color.White else Color(0xFFE91E63)
    Box(modifier = modifier) {
        activeTraps.forEach { trap ->
            val (row, col) = positionToRowCol(trap.cell)
            Box(
                modifier = Modifier
                    .offset(
                        x = (cellWidth * col.toFloat()) + (cellWidth * 0.32f),
                        y = (cellHeight * row.toFloat()) + (cellHeight * 0.32f)
                    )
                    .size(badgeSize)
                    .clip(CircleShape)
                    .background(background)
                    .border(1.3.dp, border, CircleShape)
                    .testTag("trap_tile_badge_${trap.cell}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "TR",
                    fontSize = 7.sp,
                    lineHeight = 7.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = foreground
                )
            }
        }
    }
}

@Composable
private fun SpecialTileBadgesLayer(
    specialTiles: List<BoardTile>,
    cellWidth: Dp,
    cellHeight: Dp,
    highContrastBoard: Boolean,
    modifier: Modifier = Modifier
) {
    val badgeSize = minOf(cellWidth, cellHeight) * if (highContrastBoard) 0.34f else 0.30f
    Box(modifier = modifier) {
        specialTiles.forEach { tile ->
            val (row, col) = positionToRowCol(tile.cell)
            val xOffset = (cellWidth * col.toFloat()) + (cellWidth * 0.62f) - (badgeSize / 2f)
            val yOffset = (cellHeight * row.toFloat()) + (cellHeight * 0.08f)
            val accent = tileAccentColor(tile.type)
            Box(
                modifier = Modifier
                    .offset(x = xOffset, y = yOffset)
                    .size(badgeSize)
                    .clip(CircleShape)
                    .background(if (highContrastBoard) accent else Color.White.copy(alpha = 0.96f))
                    .border(1.2.dp, if (highContrastBoard) Color.White else accent, CircleShape)
                    .testTag("special_tile_badge_${tile.cell}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tileGlyph(tile.type),
                    fontSize = 8.sp,
                    lineHeight = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (highContrastBoard) Color.White else accent
                )
            }
        }
    }
}

@Composable
private fun TokensLayer(
    players: List<PlayerState>,
    displayPositions: List<Float>,
    tokenVisualOverrides: Map<Int, Offset?>,
    currentTurnPlayerName: String?,
    cellWidth: Dp,
    cellHeight: Dp,
    tokenTrail: TokenTrailOption,
    highContrastBoard: Boolean,
    modifier: Modifier = Modifier
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val cellWidthPx = with(density) { cellWidth.toPx() }
    val cellHeightPx = with(density) { cellHeight.toPx() }
    val occupiedCounts = remember(displayPositions) {
        displayPositions
            .map { it.roundToInt().coerceIn(1, 100) }
            .groupingBy { it }
            .eachCount()
    }

    Box(modifier = modifier) {
        players.forEachIndexed { index, player ->
            val animatedPosition = displayPositions.getOrElse(index) { player.position.toFloat() }
            val normalizedOverride = tokenVisualOverrides[index]
            val snappedCell = animatedPosition.roundToInt().coerceIn(1, 100)
            val tokenSize = boardCellTokenSize(occupiedCounts[snappedCell] ?: 1)
            val slotIndex = displayPositions
                .map { it.roundToInt().coerceIn(1, 100) }
                .withIndex()
                .filter { it.value == snappedCell }
                .indexOfFirst { it.index == index }
                .coerceAtLeast(0)

            val slotOffset = when (slotIndex) {
                0 -> Offset(-0.14f, -0.14f)
                1 -> Offset(0.14f, -0.14f)
                2 -> Offset(-0.14f, 0.14f)
                else -> Offset(0.14f, 0.14f)
            }

            val centerPx = normalizedOverride?.let {
                Offset(
                    x = it.x.coerceIn(0f, 1f) * (cellWidthPx * 10f),
                    y = it.y.coerceIn(0f, 1f) * (cellHeightPx * 10f)
                )
            } ?: interpolatedCellCenter(animatedPosition, cellWidthPx, cellHeightPx)
            val centerXDp = with(density) { centerPx.x.toDp() }
            val centerYDp = with(density) { centerPx.y.toDp() }
            val offsetScalePx = cellWidthPx * 0.18f
            val slotXDp = with(density) { (slotOffset.x * offsetScalePx).toDp() }
            val slotYDp = with(density) { (slotOffset.y * offsetScalePx).toDp() }

            val pulseTransition = rememberInfiniteTransition(label = "turnPiecePulse_$index")
            val pulseScale by pulseTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.38f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 420),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "turnPieceScale_$index"
            )

            val isCurrentTurnPiece = currentTurnPlayerName == player.name
            Box(
                modifier = Modifier
                    .offset(
                        x = centerXDp - (tokenSize / 2) + slotXDp,
                        y = centerYDp - (tokenSize / 2) + slotYDp
                    )
                    .size(tokenSize)
                    .graphicsLayer {
                        val scale = if (isCurrentTurnPiece) pulseScale else 1f
                        scaleX = scale
                        scaleY = scale
                    }
                    .background(
                        brush = Brush.radialGradient(
                            colors = tokenColorsFor(player.avatarId, player.color)
                        ),
                        shape = CircleShape
                    )
                    .border(if (highContrastBoard) 2.4.dp else 1.9.dp, tokenBorderFor(player.avatarId), CircleShape)
                    .semantics {
                        contentDescription = "${player.name}, cell $snappedCell"
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (index + 1).toString(),
                    fontSize = if (tokenSize <= 11.dp) 6.sp else 7.sp,
                    lineHeight = 7.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
            if (tokenTrail != TokenTrailOption.NONE && normalizedOverride != null) {
                val trailColor = when (tokenTrail) {
                    TokenTrailOption.NONE -> Color.Transparent
                    TokenTrailOption.SPARK -> Color(0xFFFFC107)
                    TokenTrailOption.RIBBON -> player.color.copy(alpha = 0.68f)
                }
                Box(
                    modifier = Modifier
                        .offset(
                            x = centerXDp - (tokenSize * 0.75f) + slotXDp,
                            y = centerYDp - (tokenSize * 0.75f) + slotYDp
                        )
                        .size(tokenSize * 1.5f)
                        .border(1.4.dp, trailColor, CircleShape)
                        .testTag("token_trail_${index + 1}")
                )
            }
        }
    }
}

private fun tokenColorsFor(avatarId: String, fallback: Color): List<Color> {
    return avatarTokenColors(avatarId, fallback)
}

private fun tokenBorderFor(avatarId: String): Color {
    return avatarTokenBorder(avatarId)
}

private fun interpolatedCellCenter(position: Float, cellWidth: Float, cellHeight: Float): Offset {
    val clamped = position.coerceIn(1f, 100f)
    val startCell = floor(clamped).toInt().coerceIn(1, 100)
    val endCell = ceil(clamped).toInt().coerceIn(1, 100)
    if (startCell == endCell) return cellCenter(startCell, cellWidth, cellHeight)

    val t = clamped - startCell
    val start = cellCenter(startCell, cellWidth, cellHeight)
    val end = cellCenter(endCell, cellWidth, cellHeight)
    return Offset(
        x = start.x + ((end.x - start.x) * t),
        y = start.y + ((end.y - start.y) * t)
    )
}

@Composable
private fun GridCell(
    row: Int,
    col: Int,
    position: Int,
    theme: BoardThemeStyle,
    modifier: Modifier = Modifier
) {
    val base = theme.cellPalette[(row + col) % theme.cellPalette.size]
    val fill = Brush.verticalGradient(
        colors = listOf(
            base.copy(alpha = 0.97f),
            base.copy(alpha = 0.82f)
        )
    )
    Box(
        modifier = modifier
            .border(0.6.dp, theme.cellBorder)
            .background(fill)
            .padding(2.dp)
    )
}
