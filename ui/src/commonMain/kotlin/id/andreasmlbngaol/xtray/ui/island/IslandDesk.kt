package id.andreasmlbngaol.xtray.ui.island

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.SizeTransform
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
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.andreasmlbngaol.xtray.core.IslandConfig
import id.andreasmlbngaol.xtray.core.IslandState
import id.andreasmlbngaol.xtray.core.IslandStateMachine
import id.andreasmlbngaol.xtray.core.IslandStateRequest
import id.andreasmlbngaol.xtray.core.geometry.IslandGeometry
import id.andreasmlbngaol.xtray.platform.files.normalizeDroppedPath
import id.andreasmlbngaol.xtray.ui.modules.ModuleCatalog
import id.andreasmlbngaol.xtray.ui.services.IslandServices
import id.andreasmlbngaol.xtray.ui.theme.LocalReduceMotion
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
fun XTray(
    services: IslandServices,
    modifier: Modifier = Modifier,
    stateRequest: IslandStateRequest? = null,
    initialModuleId: String? = null,
    dormant: Boolean = false,
    onStateChange: (IslandState) -> Unit = {},
    onGeometryChanged: (IslandGeometry) -> Unit = {},
    onRequestHide: () -> Unit = {},
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
                val files = (event.dragData() as? DragData.FilesList)
                    ?.readFiles()
                    ?.map { normalizeDroppedPath(it) }
                    ?.filter { it.isNotBlank() }
                    .orEmpty()
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

    // A focused maximized/fullscreen window shrinks the island to a hairline
    // instead of hiding it, so it never covers the window but stays reachable.
    val dormantNow = dormant && state == IslandState.Collapsed && !isDragOver
    // While a file hovers the island it opens peek to show a proper drop zone
    // instead of only tinting the border.
    val visualState = if (isDragOver && state == IslandState.Collapsed) IslandState.Peek else state
    // A notification grows the collapsed bar into a card, like peek, so the
    // sender, title and body all fit.
    val notification = services.notifications.current
    val showNotification = !dormantNow && state == IslandState.Collapsed && notification != null
    val target = when {
        dormantNow -> IslandDimens.Dormant
        showNotification -> IslandDimens.Notification
        else -> IslandDimens.forState(visualState)
    }
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
    LaunchedEffect(target) {
        val size = target
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

    CompositionLocalProvider(
        LocalReduceMotion provides reduceMotion,
        LocalContentColor provides MaterialTheme.colorScheme.onSurface,
    ) {
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
                            dormantNow -> 2.dp
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
                        width = when {
                            dormantNow -> 0.dp
                            isDragOver -> 2.dp
                            else -> 1.dp
                        },
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
                    // Right click tucks the island away; Main brings it back
                    // when the pointer reaches the top edge again.
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                                    onRequestHide()
                                }
                            }
                        }
                    }
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                    ) {
                        machine.onActivate()
                        publish()
                        hoverEpoch++
                    },
            ) {
                val contentKey = when {
                    dormantNow -> "dormant"
                    showNotification -> "notification"
                    else -> visualState.name
                }
                if (reduceMotion) {
                    Crossfade(
                        targetState = contentKey,
                        animationSpec = snap(),
                        label = "islandState",
                    ) { current ->
                        SizedContent(
                            key = current,
                            services = services,
                            modules = modules,
                            activeModule = activeModule,
                            dropActive = isDragOver,
                            onSelectModule = { activeModuleId = it },
                        )
                    }
                } else {
                    AnimatedContent(
                        targetState = contentKey,
                        // A long, eased crossfade with no scale: the previous
                        // 180 ms scale made the leaving content pop out of
                        // existence before the new layout settled.
                        transitionSpec = {
                            (fadeIn(tween(240, delayMillis = 40)) togetherWith fadeOut(tween(150)))
                                .using(SizeTransform(clip = false))
                        },
                        label = "islandState",
                    ) { current ->
                        SizedContent(
                            key = current,
                            services = services,
                            modules = modules,
                            activeModule = activeModule,
                            dropActive = isDragOver,
                            onSelectModule = { activeModuleId = it },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Measures each state at its own designed size instead of the size the window
 * is currently animating through. Without this, the outgoing expanded content
 * (ButtonGroup in Media, fixed-width readouts in collapsed) is measured against
 * a shrinking box and Compose throws "maxWidth(-6) must be >= minWidth(0)".
 * Overflow is clipped by the island shape.
 */
@Composable
private fun SizedContent(
    key: String,
    services: IslandServices,
    modules: List<id.andreasmlbngaol.xtray.core.IslandModule>,
    activeModule: id.andreasmlbngaol.xtray.core.IslandModule?,
    dropActive: Boolean,
    onSelectModule: (String) -> Unit,
) {
    val size = when (key) {
        "dormant" -> IslandDimens.Dormant
        "notification" -> IslandDimens.Notification
        "Collapsed" -> IslandDimens.Collapsed
        "Peek" -> IslandDimens.Peek
        else -> IslandDimens.Expanded
    }
    Box(modifier = Modifier.requiredSize(size.width, size.height)) {
        IslandKeyContent(
            key = key,
            services = services,
            modules = modules,
            activeModule = activeModule,
            dropActive = dropActive,
            onSelectModule = onSelectModule,
        )
    }
}

@Composable
private fun IslandKeyContent(
    key: String,
    services: IslandServices,
    modules: List<id.andreasmlbngaol.xtray.core.IslandModule>,
    activeModule: id.andreasmlbngaol.xtray.core.IslandModule?,
    dropActive: Boolean,
    onSelectModule: (String) -> Unit,
) {
    if (key == "notification") {
        val notification = services.notifications.current
        if (notification != null) {
            NotificationContent(
                notification = notification,
                onDismiss = { services.notifications.dismiss() },
            )
        }
        return
    }
    if (key == "dormant") {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .width(46.dp)
                    .height(2.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                    ),
            )
        }
        return
    }
    IslandStateContent(
        state = IslandState.valueOf(key),
        services = services,
        modules = modules,
        activeModule = activeModule,
        dropActive = dropActive,
        onSelectModule = onSelectModule,
    )
}

@Composable
private fun IslandStateContent(
    state: IslandState,
    services: IslandServices,
    modules: List<id.andreasmlbngaol.xtray.core.IslandModule>,
    activeModule: id.andreasmlbngaol.xtray.core.IslandModule?,
    dropActive: Boolean,
    onSelectModule: (String) -> Unit,
) {
    when (state) {
        IslandState.Collapsed -> CollapsedContent(services = services)
        IslandState.Peek -> PeekContent(services = services, dropActive = dropActive)
        IslandState.Expanded -> ExpandedContent(
            modules = modules,
            activeModule = activeModule,
            onSelectModule = onSelectModule,
            services = services,
        )
    }
}
