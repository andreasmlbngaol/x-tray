package id.andreasmlbngaol.islanddesk.core

/** Immutable registry ordered by descending priority. */
class ModuleRegistry(modules: List<IslandModule>) {
    val modules: List<IslandModule> = modules.sortedByDescending { it.priority }

    fun find(id: String): IslandModule? = modules.firstOrNull { it.id == id }

    fun first(): IslandModule? = modules.firstOrNull()
}
