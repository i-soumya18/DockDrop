package `in`.soumyaranjansahoo.dockdrop.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.soumyaranjansahoo.dockdrop.ui.navigation.Routes
import `in`.soumyaranjansahoo.dockdrop.ui.theme.DockTheme
import `in`.soumyaranjansahoo.dockdrop.ui.theme.LocalReducedMotion

/**
 * Dedicated Material 3 Expressive Navigation Bar engineered to Google Product Design standards.
 * Features animated pill capsule indicators, spring-based icon interactions, tactile Pixel haptics,
 * and background activity badging.
 */
@Composable
fun DockBottomBar(
    currentRoute: String,
    onNavigateToHome: () -> Unit,
    onNavigateToRestore: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    hasHomeBadge: Boolean = false,
    hasRestoreBadge: Boolean = false
) {
    val colors = DockTheme.colors
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface)
            .navigationBarsPadding()
    ) {
        // Ultra-fine hairline divider matching Material 3 spec
        HorizontalDivider(
            color = colors.outline.copy(alpha = 0.45f),
            thickness = 0.75.dp
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GoogleNavItem(
                label = "Home",
                selected = currentRoute == Routes.HOME,
                selectedIcon = Icons.Filled.Home,
                unselectedIcon = Icons.Outlined.Home,
                rotationDegrees = 0f,
                hasBadge = hasHomeBadge,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onNavigateToHome()
                },
                modifier = Modifier.weight(1f)
            )

            GoogleNavItem(
                label = "Restore",
                selected = currentRoute == Routes.RESTORE_HUB,
                selectedIcon = Icons.Outlined.History,
                unselectedIcon = Icons.Outlined.History,
                rotationDegrees = -45f,
                hasBadge = hasRestoreBadge,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onNavigateToRestore()
                },
                modifier = Modifier.weight(1f)
            )

            GoogleNavItem(
                label = "Settings",
                selected = currentRoute == Routes.SETTINGS,
                selectedIcon = Icons.Filled.Settings,
                unselectedIcon = Icons.Outlined.Settings,
                rotationDegrees = 45f,
                hasBadge = false,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onNavigateToSettings()
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun GoogleNavItem(
    label: String,
    selected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    rotationDegrees: Float,
    hasBadge: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DockTheme.colors
    val isReducedMotion = LocalReducedMotion.current

    // 1. Google Signature Pill Indicator Animations
    val targetPillWidth = if (selected) 64.dp else 28.dp
    val pillWidth by animateDpAsState(
        targetValue = targetPillWidth,
        animationSpec = if (isReducedMotion) tween(0) else spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "pillWidth"
    )

    val targetPillColor = if (selected) {
        colors.dock.copy(alpha = 0.15f)
    } else {
        Color.Transparent
    }
    val pillColor by animateColorAsState(
        targetValue = targetPillColor,
        animationSpec = if (isReducedMotion) tween(0) else tween(durationMillis = 220),
        label = "pillColor"
    )

    // 2. Icon Spring Scale & Micro-Rotation Animations
    val targetScale = if (selected) 1.0f else 0.94f
    val iconScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = if (isReducedMotion) tween(0) else spring(
            dampingRatio = 0.65f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "iconScale"
    )

    val targetRotation = if (selected) 0f else rotationDegrees
    val iconRotation by animateFloatAsState(
        targetValue = targetRotation,
        animationSpec = if (isReducedMotion) tween(0) else spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "iconRotation"
    )

    // 3. Color & Typography Transitions
    val iconTint by animateColorAsState(
        targetValue = if (selected) colors.dock else colors.textSecondary,
        animationSpec = tween(durationMillis = 180),
        label = "iconTint"
    )

    val labelColor by animateColorAsState(
        targetValue = if (selected) colors.dock else colors.textSecondary,
        animationSpec = tween(durationMillis = 180),
        label = "labelColor"
    )

    val fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium

    Column(
        modifier = modifier
            .semantics {
                this.selected = selected
                this.role = Role.Tab
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 32.dp),
                onClick = onClick
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Pill Capsule Container
        Box(
            modifier = Modifier
                .width(pillWidth)
                .height(32.dp)
                .clip(CircleShape)
                .background(pillColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (selected) selectedIcon else unselectedIcon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier
                    .size(24.dp)
                    .scale(iconScale)
                    .rotate(iconRotation)
            )

            // Activity / Notification Badge Dot
            if (hasBadge) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = (-4).dp, y = 4.dp)
                        .clip(CircleShape)
                        .background(colors.dock)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = fontWeight,
            color = labelColor,
            letterSpacing = 0.2.sp
        )
    }
}
