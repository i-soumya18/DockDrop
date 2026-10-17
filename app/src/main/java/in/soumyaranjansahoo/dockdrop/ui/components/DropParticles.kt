package `in`.soumyaranjansahoo.dockdrop.ui.components

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockMono
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTheme
import `in`.soumyaranjansahoo.dockdrop.ui.theme.LocalReducedMotion
import `in`.soumyaranjansahoo.dockdrop.ui.theme.ParticleDocument
import `in`.soumyaranjansahoo.dockdrop.ui.theme.ParticleOther
import `in`.soumyaranjansahoo.dockdrop.ui.theme.ParticlePhoto
import `in`.soumyaranjansahoo.dockdrop.ui.theme.ParticleVideo
import java.util.Locale
import kotlin.math.sin
import kotlin.random.Random

data class Particle(
    val id: Long,
    var progress: Float, // 0f (top phone) -> 1f (dock port)
    val speed: Float,
    val xCurveOffset: Float,
    val radiusPx: Float,
    val color: Color
)

@Composable
fun DropParticles(
    filesPerSec: Double,
    isReducedMotion: Boolean = LocalReducedMotion.current,
    currentFileName: String = "",
    reverseDirection: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = DockTheme.colors

    if (isReducedMotion) {
        // Reduced motion fallback (DESIGN.md §5.3): clean tabular monospace counter
        val formattedSpeed = String.format(Locale.US, "%.1f", filesPerSec)
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(80.dp)
                .semantics {
                    contentDescription = "Transfer speed: $formattedSpeed files per second"
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$formattedSpeed files/s",
                style = DockMono.copy(fontFeatureSettings = "tnum"),
                color = colors.textSecondary
            )
        }
        return
    }

    val currentSpeed by rememberUpdatedState(filesPerSec)
    val currentFile by rememberUpdatedState(currentFileName)

    val particles = remember { mutableStateListOf<Particle>() }
    var nextId by remember { mutableStateOf(0L) }
    var lastEmitTimeMs by remember { mutableStateOf(0L) }

    LaunchedEffect(isReducedMotion) {
        var lastFrameTime = 0L
        while (true) {
            withInfiniteAnimationFrameMillis { frameTimeMs ->
                val dt = if (lastFrameTime == 0L) 0.016f else (frameTimeMs - lastFrameTime) / 1000f
                lastFrameTime = frameTimeMs

                val speed = currentSpeed
                // Real-world files/sec log scale clamped to 2..24 particles (DESIGN.md §5.2.2 & §9)
                val targetCount = if (speed <= 0.05) 0 else {
                    (2 + (speed * 1.5).toInt()).coerceIn(2, 24)
                }

                // Spawn new particle if below target count and emission interval passed
                val emitIntervalMs = if (speed > 0) (1000.0 / speed).coerceIn(80.0, 1500.0).toLong() else 2000L
                if (particles.size < targetCount && frameTimeMs - lastEmitTimeMs >= emitIntervalMs) {
                    lastEmitTimeMs = frameTimeMs
                    val ext = currentFile.substringAfterLast(".", "").lowercase()
                    val tint = when (ext) {
                        "jpg", "jpeg", "png", "heic", "webp", "dng" -> colors.categoryPhoto
                        "mp4", "mov", "mkv", "avi" -> colors.categoryVideo
                        "pdf", "doc", "docx", "txt", "zip" -> colors.categoryDocument
                        "mp3", "wav", "flac", "m4a", "aac", "ogg" -> colors.categoryAudio
                        else -> colors.categoryOther
                    }
                    particles.add(
                        Particle(
                            id = nextId++,
                            progress = 0f,
                            speed = Random.nextFloat() * 0.4f + 0.6f, // 1.5 - 2.5s duration
                            xCurveOffset = (Random.nextFloat() - 0.5f) * 40f,
                            radiusPx = Random.nextFloat() * 4f + 5f,
                            color = tint.copy(alpha = 0.75f)
                        )
                    )
                }

                // Update particle positions
                val it = particles.iterator()
                while (it.hasNext()) {
                    val p = it.next()
                    p.progress += dt * p.speed
                    if (p.progress >= 1f) {
                        it.remove()
                    }
                }
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .graphicsLayer()
            .clearAndSetSemantics { } // Decorative particles; telemetry read by status headline
    ) {
        val startX = size.width / 2f
        val startY = 8.dp.toPx()
        val endX = size.width / 2f
        val endY = size.height - 8.dp.toPx()

        for (p in particles) {
            val prog = p.progress
            // Gentle cubic bezier-like S-curve
            val currentY = if (reverseDirection) endY - (endY - startY) * prog else startY + (endY - startY) * prog
            val wave = sin(prog * Math.PI.toFloat()) * p.xCurveOffset
            val currentX = startX + wave

            // Shrink as particle enters port/destination (last 15% of progress)
            val scale = if (prog > 0.85f) ((1f - prog) / 0.15f).coerceIn(0.2f, 1f) else 1f

            drawCircle(
                color = p.color,
                radius = p.radiusPx * scale,
                center = Offset(currentX, currentY)
            )
        }
    }
}
