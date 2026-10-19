package id.andreasmlbngaol.islanddesk.platform.files

interface FileOpener {
    fun open(path: String): Boolean
    fun revealInFolder(path: String): Boolean
}
