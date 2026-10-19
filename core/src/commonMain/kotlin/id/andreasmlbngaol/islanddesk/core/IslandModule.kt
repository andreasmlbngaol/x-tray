package id.andreasmlbngaol.islanddesk.core

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * A feature that can render itself at each disclosure level.
 *
 * Modules are intentionally UI-light in core: they expose composable slots and
 * lifecycle callbacks, while the actual feature logic lives in its own module.
 * Third-party modules can be added later by registering another implementation.
 */
interface IslandModule {
    val id: String
    val title: String
    val icon: ImageVector

    /** Higher priority modules win the collapsed/peek slot. */
    val priority: Int get() = 0

    @Composable
    fun CollapsedContent() = Unit

    @Composable
    fun PeekContent() = Unit

    @Composable
    fun ExpandedContent() = Unit

    fun onShown() = Unit

    fun onHidden() = Unit
}
