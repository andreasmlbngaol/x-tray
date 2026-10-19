package id.andreasmlbngaol.islanddesk.ui.island

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.andreasmlbngaol.islanddesk.core.IslandConfig
import id.andreasmlbngaol.islanddesk.core.IslandState
import id.andreasmlbngaol.islanddesk.core.IslandStateMachine
import id.andreasmlbngaol.islanddesk.core.ModuleRegistry
import id.andreasmlbngaol.islanddesk.ui.theme.LocalReduceMotion
import kotlinx.coroutines.delay

/**
 * The island itself: a single morphing surface with three disclosure levels.
 *
 * The host (desktop window) only needs to react to [onStateChange] so it can
 * keep the native window tight around the visual. Pointer intent, timers and
 * keyboard dismissal live here.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun IslandDesk(
    registry: ModuleRegistry,
    modifier: Modifier = Modifier,
    config: IslandConfig = IslandConfig(),
    collapseSignal: Int = 0,
    activateSignal: Int = 0,
    onStateChange: (IslandState) -> Unit = {},
) {
    val machine = remember(config) { IslandStateMachine(config) }
    var state by remember { mutableStateOf(machine.state) }
    var hoverEpoch by remember { mutableIntStateOf(0) }
    var activeModuleId by remember { mutableStateOf(registry.first()?.id) }

    val activeModule = registry.modules.firstOrNull { it.id == activeModuleId } ?: registry.first()
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    fun publish() {
        state = machine.state
        onStateChange(machine.state)
    }

    LaunchedEffect(Unit) {
        onStateChange(state)
    }

    LaunchedEffect(collapseSignal) {
        if (collapseSignal > 0) {
            machine.onDismiss()
            publish()
            hoverEpoch++
        }
    }

    LaunchedEffect(activateSignal) {
        if (activateSignal > 0) {
            machine.onActivate()
            publish()
            hoverEpoch++
        }
    }

    LaunchedEffect(isHovered) {
        val now = System.nanoTime() / 1_000_000L
        if (isHovered) {
            machine.onPointerEnter(now)
        } else {
            machine.onPointerExit(now)
        }
        publish()
        hoverEpoch++
    }

    LaunchedEffect(state, hoverEpoch) {
        while (true) {
            if (!machine.isTransitionPending(System.nanoTime() / 1_000_000L)) break
            delay(16L)
            machine.onTick(System.nanoTime() / 1_000_000L)
            if (machine.state != state) {
                publish()
            }
        }
    }

    val target = IslandDimens.forState(state)
    val motion = MaterialTheme.motionScheme
    val reduceMotion = LocalReduceMotion.current
    val spatialSpec = remember(motion, state, reduceMotion) {
        when {
            reduceMotion -> snap<Dp>()
            state == IslandState.Collapsed -> motion.fastSpatialSpec<Dp>()
            else -> motion.defaultSpatialSpec<Dp>()
        }
    }

    val width by animateDpAsState(
        targetValue = target.width,
        animationSpec = spatialSpec,
        label = "islandWidth",
    )
    val height by animateDpAsState(
        targetValue = target.height,
        animationSpec = spatialSpec,
        label = "islandHeight",
    )
    val bottomRadius by animateDpAsState(
        targetValue = target.bottomRadius,
        animationSpec = spatialSpec,
        label = "islandBottomRadius",
    )
    val filletRadius by animateDpAsState(
        targetValue = target.filletRadius,
        animationSpec = spatialSpec,
        label = "islandFillet",
    )

    val shape = remember(bottomRadius, filletRadius) {
        IslandShape(bottomRadius = bottomRadius, filletRadius = filletRadius)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .width(width)
                .height(height)
                .shadow(
                    elevation = if (state == IslandState.Collapsed) 6.dp else 16.dp,
                    shape = shape,
                    clip = false,
                )
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.98f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    shape = shape,
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                ) {
                    machine.onActivate()
                    publish()
                    hoverEpoch++
                },
        ) {
            Crossfade(
                targetState = state,
                animationSpec = tween(160),
                label = "islandState",
            ) { current ->
                when (current) {
                    IslandState.Collapsed -> CollapsedContent()
                    IslandState.Peek -> PeekContent(activeModule = activeModule)
                    IslandState.Expanded -> ExpandedContent(
                        modules = registry.modules,
                        activeModule = activeModule,
                        onSelectModule = { activeModuleId = it },
                    )
                }
            }
        }
    }
}
