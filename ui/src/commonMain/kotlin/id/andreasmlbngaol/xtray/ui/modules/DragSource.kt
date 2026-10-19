package id.andreasmlbngaol.xtray.ui.modules

import androidx.compose.ui.Modifier

/** Lets an item be dragged out of the island as a real file (XDND copy). */
expect fun Modifier.fileDragSource(path: String): Modifier
