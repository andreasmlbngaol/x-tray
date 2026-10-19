package id.andreasmlbngaol.islanddesk.ui.island

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.draganddrop.dragAndDropTarget
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * The island itself: a single morphing surface with three disclosure levels.
 *
 * All system access is injected through [services]; the host only needs to
 * react to [onStateChange] (window resize) and [onGeometryChanged] (input
 * region shaping).
 */
@OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class,
)
@Composable
fun IslandDesk(
    services: IslandServices,
    modifier: Modifier = Modifier,
    stateRequest: IslandStateRequest? = null,
    initialModuleId: String? = null,
    onStateChange: (IslandState) -> Unit = {},
    onGeometryChanged: (IslandGeometry) -> Unit = {},
) {
    val settings = services.settings.current
    val config = remember(settings.hoverIntentMs, settings.collapseDelayMs) {
        IslandConfig.from(settings)
    }
    val machine = remember { IslandStateMachine(config) }
    LaunchedEffect(config) { machine.config = config }
    var state by remember { mutableStateOf(machine.state) }
    var hoverEpoch by remember { mutableIntStateOf(0) }
    var activeModuleId by remember { mutableStateOf(initialModuleId) }

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
    var isDragOver by remember { mutableStateOf(false) }

    fun publish() {
        state = machine.state
        onStateChange(machine.state)
    }

    LaunchedEffect(Unit) {
        onStateChange(state)
    }

    val dropTarget = remember(services) {
        object : DragAndDropTarget {
            override fun onEntered(event: DragAndDropEvent) {
                if (event.dragData() is DragData.FilesList) isDragOver = true
            }

            override fun onExited(event: DragAndDropEvent) {
                isDragOver = false
            }

            override fun onEnded(event: DragAndDropEvent) {
                isDragOver = false
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                isDragOver = false
                val files = (event.dragData() as? DragData.FilesList)?.readFiles().orEmpty()
                if (files.isEmpty()) return false
                services.shelf.add(files, System.currentTimeMillis())
                services.bump()
                activeModuleId = "shelf"
                machine.onActivate()
                publish()
                return true
            }
        }
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

    LaunchedEffect(state) {
        if (state == IslandState.Collapsed) return@LaunchedEffect
        while (true) {
            services.mediaTrack = withContext(Dispatchers.Default) {
                runCatching { services.media.snapshot() }.getOrNull()
            }
            delay(1_500L)
        }
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
    val isDarkSurface = MaterialTheme.colorScheme.surface.luminance() < 0.5f

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
                        elevation = when {
                            state == IslandState.Collapsed -> if (isDarkSurface) 6.dp else 10.dp
                            else -> if (isDarkSurface) 16.dp else 22.dp
                        },
                        shape = shape,
                        clip = false,
                    )
                    .clip(shape)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.98f),
                                MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.99f),
                            )
                        )
                    )
                    .border(
                        width = if (isDragOver) 2.dp else 1.dp,
                        color = if (isDragOver) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant.copy(
                                alpha = if (isDarkSurface) 0.35f else 0.65f,
                            )
                        },
                        shape = shape,
                    )
                    .dragAndDropTarget(
                        shouldStartDragAndDrop = { event -> event.dragData() is DragData.FilesList },
                        target = dropTarget,
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
                if (reduceMotion) {
                    Crossfade(
                        targetState = state,
                        animationSpec = snap(),
                        label = "islandState",
                    ) { current ->
                        IslandStateContent(
                            state = current,
                            services = services,
                            modules = modules,
                            activeModule = activeModule,
                            onSelectModule = { activeModuleId = it },
                        )
                    }
                } else {
                    AnimatedContent(
                        targetState = state,
                        transitionSpec = {
                            (fadeIn(tween(180)) + scaleIn(initialScale = 0.97f)) togetherWith
                                (fadeOut(tween(120)) + scaleOut(targetScale = 0.99f))
                        },
                        label = "islandState",
                    ) { current ->
                        IslandStateContent(
                            state = current,
                            services = services,
                            modules = modules,
                            activeModule = activeModule,
                            onSelectModule = { activeModuleId = it },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IslandStateContent(
    state: IslandState,
    services: IslandServices,
    modules: List<id.andreasmlbngaol.islanddesk.core.IslandModule>,
    activeModule: id.andreasmlbngaol.islanddesk.core.IslandModule?,
    onSelectModule: (String) -> Unit,
) {
    when (state) {
        IslandState.Collapsed -> CollapsedContent(services = services)
        IslandState.Peek -> PeekContent(services = services)
        IslandState.Expanded -> ExpandedContent(
            modules = modules,
            activeModule = activeModule,
            onSelectModule = onSelectModule,
            services = services,
        )
    }
}
