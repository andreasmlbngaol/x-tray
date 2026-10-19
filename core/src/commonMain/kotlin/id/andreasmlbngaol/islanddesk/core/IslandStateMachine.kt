package id.andreasmlbngaol.islanddesk.core

/**
 * Pure, clock-injected state machine for the island.
 *
 * The UI layer is responsible for feeding pointer events and ticking it while
 * a transition is pending. Keeping the logic free of coroutines and Compose
 * makes it deterministic and unit-testable.
 */
class IslandStateMachine(
    config: IslandConfig = IslandConfig(),
) {
    /** Updated live when the user changes timing preferences. */
    var config: IslandConfig = config
    var state: IslandState = IslandState.Collapsed
        private set

    private var pointerInside: Boolean = false
    private var hoverStartedAt: Long? = null
    private var peekStartedAt: Long? = null
    private var exitStartedAt: Long? = null
    private var suppressedUntilExit: Boolean = false

    fun onPointerEnter(nowMs: Long): IslandState {
        pointerInside = true
        exitStartedAt = null
        if (suppressedUntilExit) return state

        when (state) {
            IslandState.Collapsed -> if (hoverStartedAt == null) hoverStartedAt = nowMs
            IslandState.Peek -> if (peekStartedAt == null) peekStartedAt = nowMs
            IslandState.Expanded -> Unit
        }
        return state
    }

    fun onPointerExit(nowMs: Long): IslandState {
        pointerInside = false
        suppressedUntilExit = false
        hoverStartedAt = null
        peekStartedAt = null
        if (state != IslandState.Collapsed) {
            exitStartedAt = nowMs
        }
        return state
    }

    /**
     * Advances time. Call this on every frame while [isTransitionPending] is
     * true, or on a short timer. It is intentionally cheap.
     */
    fun onTick(nowMs: Long): IslandState {
        if (suppressedUntilExit) return state

        if (pointerInside) {
            exitStartedAt = null
            when (state) {
                IslandState.Collapsed -> {
                    val since = hoverStartedAt ?: nowMs.also { hoverStartedAt = it }
                    if (nowMs - since >= config.hoverIntentMs) {
                        state = IslandState.Peek
                        peekStartedAt = nowMs
                    }
                }

                IslandState.Peek -> {
                    val since = peekStartedAt ?: nowMs.also { peekStartedAt = it }
                    if (nowMs - since >= config.peekToExpandedMs) {
                        state = IslandState.Expanded
                        peekStartedAt = null
                        hoverStartedAt = null
                    }
                }

                IslandState.Expanded -> Unit
            }
        } else if (state != IslandState.Collapsed) {
            val since = exitStartedAt ?: nowMs.also { exitStartedAt = it }
            if (nowMs - since >= config.collapseDelayMs) {
                state = IslandState.Collapsed
                hoverStartedAt = null
                peekStartedAt = null
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
        peekStartedAt = null
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
        peekStartedAt = null
        exitStartedAt = null
        return state
    }

    fun isTransitionPending(nowMs: Long): Boolean {
        if (suppressedUntilExit) return false
        return when (state) {
            IslandState.Collapsed -> pointerInside && hoverStartedAt != null
            IslandState.Peek -> pointerInside && peekStartedAt != null
            IslandState.Expanded -> !pointerInside && exitStartedAt != null
        }
    }

    fun reset() {
        state = IslandState.Collapsed
        pointerInside = false
        hoverStartedAt = null
        peekStartedAt = null
        exitStartedAt = null
        suppressedUntilExit = false
    }
}
