package com.quiting.app.ui

import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip

enum class AppStyle(val label: String, val hint: String) {
    MATERIAL_YOU("Material You", "Adapts to your wallpaper on Android 12 and newer"),
    LIQUID_GLASS("Liquid Glass", "Translucent layers over a drifting light field"),
    MINIMAL("Minimalism", "Monochrome, flat and quiet"),
    NEON("Neon Holo", "Cyan and magenta on deep black")
}

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

@Composable
fun QuitingTheme(style: AppStyle, mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current

    val scheme = when (style) {
        AppStyle.MATERIAL_YOU ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (dark) darkColorScheme() else lightColorScheme()
            }

        AppStyle.LIQUID_GLASS ->
            if (dark) darkColorScheme(
                primary = Color(0xFF9FB8FF),
                secondary = Color(0xFF7FE7D9),
                tertiary = Color(0xFFD6A8FF),
                background = Color(0xFF080B16),
                surface = Color(0xFF151B2E),
                onBackground = Color(0xFFEFF3FF),
                onSurface = Color(0xFFEFF3FF),
                surfaceVariant = Color(0xFF1C2440),
                onSurfaceVariant = Color(0xFFB7C0D8),
                outline = Color(0x40FFFFFF)
            ) else lightColorScheme(
                primary = Color(0xFF3A5BD9),
                secondary = Color(0xFF1F9C8F),
                tertiary = Color(0xFF8B4FD6),
                background = Color(0xFFE9EEF9),
                surface = Color(0xFFFFFFFF),
                onBackground = Color(0xFF0E1530),
                onSurface = Color(0xFF0E1530),
                surfaceVariant = Color(0xFFF2F5FC),
                onSurfaceVariant = Color(0xFF4A5577),
                outline = Color(0x33000000)
            )

        AppStyle.MINIMAL ->
            if (dark) darkColorScheme(
                primary = Color.White,
                onPrimary = Color.Black,
                background = Color.Black,
                surface = Color(0xFF121212),
                onBackground = Color.White,
                onSurface = Color.White,
                surfaceVariant = Color(0xFF1C1C1C),
                onSurfaceVariant = Color(0xFF9A9A9A),
                outline = Color(0xFF2A2A2A)
            ) else lightColorScheme(
                primary = Color.Black,
                onPrimary = Color.White,
                background = Color.White,
                surface = Color(0xFFF5F5F5),
                onBackground = Color.Black,
                onSurface = Color.Black,
                surfaceVariant = Color(0xFFEDEDED),
                onSurfaceVariant = Color(0xFF6B6B6B),
                outline = Color(0xFFDADADA)
            )

        AppStyle.NEON ->
            if (dark) darkColorScheme(
                primary = Color(0xFF3EF0FF),
                secondary = Color(0xFFFF3EC8),
                tertiary = Color(0xFF8B5CFF),
                background = Color(0xFF04060C),
                surface = Color(0xFF0C1422),
                onBackground = Color(0xFFE6F4FF),
                onSurface = Color(0xFFE6F4FF),
                surfaceVariant = Color(0xFF13203A),
                onSurfaceVariant = Color(0xFF7D91A8),
                outline = Color(0x333EF0FF)
            ) else lightColorScheme(
                primary = Color(0xFF0097A7),
                secondary = Color(0xFFD81B8C),
                tertiary = Color(0xFF6A3FD0),
                background = Color(0xFFF4F8FF),
                surface = Color(0xFFFFFFFF),
                onBackground = Color(0xFF0A1424),
                onSurface = Color(0xFF0A1424),
                surfaceVariant = Color(0xFFE6F0FA),
                onSurfaceVariant = Color(0xFF4F6078),
                outline = Color(0x330097A7)
            )
    }

    MaterialTheme(
        colorScheme = scheme,
        shapes = shapesFor(style),
        content = content
    )
}

private fun shapesFor(style: AppStyle): Shapes = when (style) {
    AppStyle.LIQUID_GLASS -> Shapes(
        small = RoundedCornerShape(16.dp),
        medium = RoundedCornerShape(24.dp),
        large = RoundedCornerShape(32.dp)
    )
    AppStyle.MINIMAL -> Shapes(
        small = RoundedCornerShape(2.dp),
        medium = RoundedCornerShape(4.dp),
        large = RoundedCornerShape(4.dp)
    )
    AppStyle.NEON -> Shapes(
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(16.dp)
    )
    AppStyle.MATERIAL_YOU -> Shapes(
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(20.dp),
        large = RoundedCornerShape(28.dp)
    )
}

/** Animated backdrop: drifting light for Liquid Glass, a moving grid for Neon, flat for the rest. */
@Composable
fun StyledBackground(style: AppStyle, content: @Composable () -> Unit) {
    val c = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "bg")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "t"
    )

    Box(Modifier.fillMaxSize().background(c.background)) {
        when (style) {
            AppStyle.LIQUID_GLASS -> Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                fun blob(color: Color, cx: Float, cy: Float, r: Float) {
                    drawCircle(
                        brush = Brush.radialGradient(listOf(color, Color.Transparent), center = Offset(cx, cy), radius = r),
                        radius = r,
                        center = Offset(cx, cy)
                    )
                }
                blob(c.primary.copy(alpha = 0.45f), w * (0.15f + 0.45f * t), h * 0.22f, w * 0.7f)
                blob(c.secondary.copy(alpha = 0.35f), w * (0.9f - 0.5f * t), h * 0.68f, w * 0.65f)
                blob(c.tertiary.copy(alpha = 0.30f), w * 0.5f, h * (0.45f + 0.25f * t), w * 0.6f)
            }

            AppStyle.NEON -> Canvas(Modifier.fillMaxSize()) {
                val step = size.width / 14f
                val shift = t * step
                var x = 0f
                while (x <= size.width) {
                    drawLine(c.primary.copy(alpha = 0.08f), Offset(x, 0f), Offset(x, size.height), 1f)
                    x += step
                }
                var y = shift
                while (y <= size.height) {
                    drawLine(c.secondary.copy(alpha = 0.07f), Offset(0f, y), Offset(size.width, y), 1f)
                    y += step
                }
            }

            else -> Unit
        }
        content()
    }
}

/** Card surface that changes its look per style. */
@Composable
fun Modifier.glassCard(style: AppStyle, shape: Shape = RoundedCornerShape(24.dp)): Modifier {
    val c = MaterialTheme.colorScheme
    val isDark = c.background.luminance() < 0.5f
    return when (style) {
        AppStyle.LIQUID_GLASS -> {
            val tint = if (isDark) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.55f)
            this.clip(shape)
                .background(Brush.verticalGradient(listOf(tint, tint.copy(alpha = tint.alpha * 0.35f))))
                .border(
                    1.dp,
                    Brush.linearGradient(listOf(Color.White.copy(alpha = 0.45f), Color.Transparent)),
                    shape
                )
        }
        AppStyle.NEON -> this.clip(shape)
            .background(c.surface)
            .border(1.dp, c.primary.copy(alpha = 0.45f), shape)
        AppStyle.MINIMAL -> this.clip(shape).background(c.surface)
        AppStyle.MATERIAL_YOU -> this.clip(shape).background(c.surfaceVariant)
    }
}
