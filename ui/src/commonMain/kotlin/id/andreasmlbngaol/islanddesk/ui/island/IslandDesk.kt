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
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.andreasmlbngaol.islanddesk.core.IslandConfig
import id.andreasmlbngaol.islanddesk.core.IslandState
import id.andreasmlbngaol.islanddesk.core.IslandStateMachine
import id.andreasmlbngaol.islanddesk.core.IslandStateRequest
import id.andreasmlbngaol.islanddesk.core.geometry.IslandGeometry
import id.andreasmlbngaol.islanddesk.ui.modules.ModuleCatalog
import id.andreasmlbngaol.islanddesk.ui.services.IslandServices
import id.andreasmlbngaol.islanddesk.ui.theme.LocalReduceMotion
import kotlinx.coroutines.delay

/**
 * The island itself: a single morphing surface with three disclosure levels.
 *
 * All system access is injected through [services]; the host only needs to
 * react to [onStateChange] (window resize) and [onGeometryChanged] (input
 * region shaping).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun IslandDesk(
    services: IslandServices,
    modifier: Modifier = Modifier,
    stateRequest: IslandStateRequest? = null,
    onStateChange: (IslandState) -> Unit = {},
    onGeometryChanged: (IslandGeometry) -> Unit = {},
) {
    val settings = services.settings.current
    val config = remember(settings.hoverIntentMs, settings.peekToExpandedMs, settings.collapseDelayMs) {
        IslandConfig.from(settings)
    }
    val machine = remember { IslandStateMachine(config) }
    LaunchedEffect(config) { machine.config = config }
    var state by remember { mutableStateOf(machine.state) }
    var hoverEpoch by remember { mutableIntStateOf(0) }
    var activeModuleId by remember { mutableStateOf<String?>(null) }

    val allModules = remember(services) { ModuleCatalog.create(services) }
    val modules = remember(
        allModules,
        settings.mediaEnabled,
        settings.clipboardEnabled,
        settings.shelfEnabled,
        settings.controlsEnabled,
    ) { ModuleCatalog.filter(allModules, settings).sortedByDescending { it.priority } }
    val activeModule = modules.firstOrNull { it.id == activeModuleId } ?: modules.firstOrNull()

    LaunchedEffect(modules) {
        if (modules.none { it.id == activeModuleId }) {
            activeModuleId = modules.firstOrNull()?.id
        }
    }

    LaunchedEffect(state, activeModule?.id) {
        if (state == IslandState.Collapsed) {
            allModules.forEach { it.onHidden() }
        } else {
            activeModule?.onShown()
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    fun publish() {
        state = machine.state
        onStateChange(machine.state)
    }

    LaunchedEffect(Unit) {
        onStateChange(state)
    }

    LaunchedEffect(stateRequest?.id) {
        val request = stateRequest ?: return@LaunchedEffect
        when (request.target) {
            IslandState.Collapsed -> machine.onDismiss()
            IslandState.Expanded -> machine.onActivate()
            IslandState.Peek -> machine.onActivate()
        }
        publish()
        hoverEpoch++
    }

    LaunchedEffect(settings.doNotDisturb) {
        if (settings.doNotDisturb) {
            machine.onDismiss()
            publish()
        }
    }

    LaunchedEffect(isHovered) {
        val now = System.currentTimeMillis()
        if (isHovered && !services.settings.current.doNotDisturb) {
            machine.onPointerEnter(now)
        } else if (!isHovered) {
            machine.onPointerExit(now)
        }
        publish()
        hoverEpoch++
    }

    LaunchedEffect(state, hoverEpoch) {
        while (true) {
            if (!machine.isTransitionPending(System.currentTimeMillis())) break
            delay(16L)
            machine.onTick(System.currentTimeMillis())
            if (machine.state != state) {
                publish()
            }
        }
    }

    val target = IslandDimens.forState(state)
    val motion = MaterialTheme.motionScheme
    val reduceMotion = LocalReduceMotion.current || settings.reduceMotion
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

    val density = LocalDensity.current
    LaunchedEffect(state) {
        val size = IslandDimens.forState(state)
        val windowWidth = with(density) { IslandDimens.windowWidth(size).roundToPx() }
        val windowHeight = with(density) { IslandDimens.windowHeight(size).roundToPx() }
        onGeometryChanged(
            IslandGeometry(
                windowWidthPx = windowWidth,
                windowHeightPx = windowHeight,
                islandWidthPx = with(density) { size.width.roundToPx() },
                islandHeightPx = with(density) { size.height.roundToPx() },
                bottomRadiusPx = with(density) { size.bottomRadius.toPx() },
                paddingPx = with(density) { IslandDimens.WindowHorizontalPadding.roundToPx() },
            )
        )
    }

    CompositionLocalProvider(LocalReduceMotion provides reduceMotion) {
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
                    animationSpec = if (reduceMotion) snap() else tween(160),
                    label = "islandState",
                ) { current ->
                    when (current) {
                        IslandState.Collapsed -> CollapsedContent(services = services)
                        IslandState.Peek -> PeekContent(
                            activeModule = activeModule,
                            services = services,
                        )
                        IslandState.Expanded -> ExpandedContent(
                            modules = modules,
                            activeModule = activeModule,
                            onSelectModule = { activeModuleId = it },
                            services = services,
                        )
                    }
                }
            }
        }
    }
}
