package id.andreasmlbngaol.xtray.ui.modules

import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropTransferAction
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.DragAndDropTransferable
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.io.File

@OptIn(ExperimentalComposeUiApi::class)
actual fun Modifier.fileDragSource(path: String): Modifier = dragAndDropSource {
    val file = File(path)
    if (!file.exists()) return@dragAndDropSource null
    val transferable = object : Transferable {
        override fun getTransferDataFlavors(): Array<DataFlavor> = arrayOf(DataFlavor.javaFileListFlavor)
        override fun isDataFlavorSupported(flavor: DataFlavor): Boolean =
            flavor == DataFlavor.javaFileListFlavor
        override fun getTransferData(flavor: DataFlavor): Any = listOf(file)
    }
    DragAndDropTransferData(
        transferable = DragAndDropTransferable(transferable),
        supportedActions = listOf(DragAndDropTransferAction.Copy),
    )
}
