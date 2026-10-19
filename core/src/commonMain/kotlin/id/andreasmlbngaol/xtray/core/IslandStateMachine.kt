package id.andreasmlbngaol.xtray.core

/**
 * Pure, clock-injected state machine for the island.
 *
 * Behaviour:
 * - Hovering the collapsed island opens the peek after [IslandConfig.hoverIntentMs].
 * - Peek never opens by itself; a click is required to expand.
 * - Leaving peek collapses it after [IslandConfig.collapseDelayMs].
 * - Expanded ignores pointer movement; it closes on explicit dismiss (Esc,
 *   click outside, focus loss).
 *
 * The UI feeds pointer events and ticks it while a transition is pending.
 */
class IslandStateMachine(
    config: IslandConfig = IslandConfig(),
) {
    var config: IslandConfig = config

    var state: IslandState = IslandState.Collapsed
        private set

    private var pointerInside: Boolean = false
    private var hoverStartedAt: Long? = null
    private var exitStartedAt: Long? = null
    private var suppressedUntilExit: Boolean = false

    fun onPointerEnter(nowMs: Long): IslandState {
        pointerInside = true
        exitStartedAt = null
        if (suppressedUntilExit) return state

        if (state == IslandState.Collapsed && hoverStartedAt == null) {
            hoverStartedAt = nowMs
        }
        return state
    }

    fun onPointerExit(nowMs: Long): IslandState {
        pointerInside = false
        suppressedUntilExit = false
        hoverStartedAt = null
        if (state == IslandState.Peek) {
            exitStartedAt = nowMs
        }
        return state
    }

    /**
     * Advances time. Call this on every frame while [isTransitionPending] is
     * true. It is intentionally cheap.
     */
    fun onTick(nowMs: Long): IslandState {
        if (suppressedUntilExit) return state

        if (pointerInside) {
            exitStartedAt = null
            if (state == IslandState.Collapsed) {
                val since = hoverStartedAt ?: nowMs.also { hoverStartedAt = it }
                if (nowMs - since >= config.hoverIntentMs) {
                    state = IslandState.Peek
                    hoverStartedAt = null
                }
            }
        } else if (state == IslandState.Peek) {
            val since = exitStartedAt ?: nowMs.also { exitStartedAt = it }
            if (nowMs - since >= config.collapseDelayMs) {
                state = IslandState.Collapsed
                exitStartedAt = null
            }
        }

        return state
    }

    /** Explicit activation (click / Enter) opens the panel immediately. */
    fun onActivate(): IslandState {
        suppressedUntilExit = false
        state = IslandState.Expanded
        hoverStartedAt = null
        exitStartedAt = null
        return state
    }

    /**
     * Esc or a click outside the island collapses it. While the pointer is
     * still inside, hover expansion stays suppressed until it leaves.
     */
    fun onDismiss(): IslandState {
        suppressedUntilExit = pointerInside
        state = IslandState.Collapsed
        hoverStartedAt = null
        exitStartedAt = null
        return state
    }

    fun isTransitionPending(nowMs: Long): Boolean {
        if (suppressedUntilExit) return false
        return when (state) {
            IslandState.Collapsed -> pointerInside && hoverStartedAt != null
            IslandState.Peek -> !pointerInside && exitStartedAt != null
            IslandState.Expanded -> false
        }
    }

    fun reset() {
        state = IslandState.Collapsed
        pointerInside = false
        hoverStartedAt = null
        exitStartedAt = null
        suppressedUntilExit = false
    }
}
