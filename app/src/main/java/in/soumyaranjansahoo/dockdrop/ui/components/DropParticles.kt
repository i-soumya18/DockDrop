package `in`.soumyaranjansahoo.dockdrop.ui.components

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockMono
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTheme
import `in`.soumyaranjansahoo.dockdrop.ui.theme.ParticleDocument
import `in`.soumyaranjansahoo.dockdrop.ui.theme.ParticleOther
import `in`.soumyaranjansahoo.dockdrop.ui.theme.ParticlePhoto
import `in`.soumyaranjansahoo.dockdrop.ui.theme.ParticleVideo
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
    isReducedMotion: Boolean = false,
    currentFileName: String = "",
    reverseDirection: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = DockTheme.colors

    if (isReducedMotion) {
        // Reduced motion fallback (DESIGN.md §5.3): clean tabular monospace counter
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(80.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${String.format("%.1f", filesPerSec)} files/s",
                style = DockMono,
                color = colors.textSecondary
            )
        }
        return
    }

    val particles = remember { mutableStateListOf<Particle>() }
    var nextId by remember { mutableStateOf(0L) }
    var lastEmitTimeMs by remember { mutableStateOf(0L) }

    // Derive target active particles count (2 to 24) based on real files/sec log scale
    val targetCount = if (filesPerSec <= 0.05) 0 else {
        (2 + (filesPerSec * 1.5).toInt()).coerceIn(2, 24)
    }

    LaunchedEffect(filesPerSec) {
        var lastFrameTime = 0L
        while (true) {
            withInfiniteAnimationFrameMillis { frameTimeMs ->
                val dt = if (lastFrameTime == 0L) 0.016f else (frameTimeMs - lastFrameTime) / 1000f
                lastFrameTime = frameTimeMs

                // Spawn new particle if below target count and emission interval passed
                val emitIntervalMs = if (filesPerSec > 0) (1000.0 / filesPerSec).coerceIn(80.0, 1500.0).toLong() else 2000L
                if (particles.size < targetCount && frameTimeMs - lastEmitTimeMs >= emitIntervalMs) {
                    lastEmitTimeMs = frameTimeMs
                    val ext = currentFileName.substringAfterLast(".", "").lowercase()
                    val tint = when (ext) {
                        "jpg", "jpeg", "png", "heic", "webp", "dng" -> ParticlePhoto
                        "mp4", "mov", "mkv", "avi" -> ParticleVideo
                        "pdf", "doc", "docx", "txt", "zip" -> ParticleDocument
                        else -> ParticleOther
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
