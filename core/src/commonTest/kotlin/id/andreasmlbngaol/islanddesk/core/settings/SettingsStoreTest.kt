package id.andreasmlbngaol.islanddesk.core.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class MemoryPersistence(var value: String? = null) : SettingsPersistence {
    var saves = 0
    override fun load(): String? = value
    override fun save(json: String) {
        value = json
        saves++
    }
}

class SettingsStoreTest {
    @Test
    fun defaultsAreUsedWhenNothingIsStored() {
        val store = SettingsStore(MemoryPersistence())
        assertEquals(IslandSettings(), store.current)
    }

    @Test
    fun updatePersistsAndReloads() {
        val persistence = MemoryPersistence()
        val store = SettingsStore(persistence)
        store.update { it.copy(hoverIntentMs = 240L, doNotDisturb = true) }
        assertEquals(1, persistence.saves)

        val reloaded = SettingsStore(persistence)
        assertEquals(240L, reloaded.current.hoverIntentMs)
        assertTrue(reloaded.current.doNotDisturb)
    }

    @Test
    fun invalidValuesAreClamped() {
        val store = SettingsStore(MemoryPersistence())
        store.update { it.copy(hoverIntentMs = 5L, clipboardMaxItems = 999, focusMinutes = 0) }
        assertEquals(50L, store.current.hoverIntentMs)
        assertEquals(50, store.current.clipboardMaxItems)
        assertEquals(1, store.current.focusMinutes)
    }

    @Test
    fun brokenJsonFallsBackToDefaults() {
        val store = SettingsStore(MemoryPersistence("{not json"))
        assertEquals(IslandSettings(), store.current)
    }

    @Test
    fun unchangedUpdateDoesNotWrite() {
        val persistence = MemoryPersistence()
        val store = SettingsStore(persistence)
        store.update { it }
        assertEquals(0, persistence.saves)
        assertFalse(persistence.value != null)
    }
}
