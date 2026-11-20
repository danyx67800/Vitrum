package com.vitrum.tv.core

import com.vitrum.tv.data.AppInfo
import com.vitrum.tv.data.SortOrder
import org.junit.Assert.assertEquals
import org.junit.Test

class AppLogicTest {

    private fun app(pkg: String, label: String, system: Boolean = false) =
        AppInfo(packageName = pkg, label = label, activityName = "$pkg.Main", isSystem = system)

    private val apps = listOf(
        app("org.zed", "Zed Player"),
        app("com.all", "AllCast"),
        app("com.sys", "System Tool", system = true),
        app("net.mix", "Mix"),
        app("net.move", "Move"),
    )

    @Test
    fun filtersHiddenSystemAndQuery() {
        val visible = AppLogic.filter(
            apps = apps,
            query = "",
            hidden = setOf("net.mix"),
            showSystem = false,
        )
        assertEquals(listOf("AllCast", "Move", "Zed Player"), visible.map { it.label }.sorted())

        val query = AppLogic.filter(apps, query = "move", hidden = emptySet(), showSystem = false)
        assertEquals(listOf("Move"), query.map { it.label })

        val withSystem = AppLogic.filter(apps, hidden = emptySet(), showSystem = true)
        assertEquals(5, withSystem.size)
    }

    @Test
    fun sortsAlphabeticallyCaseInsensitive() {
        val sorted = AppLogic.sorted(apps, SortOrder.AZ)
        assertEquals(listOf("AllCast", "Mix", "Move", "System Tool", "Zed Player"), sorted.map { it.label })
    }

    @Test
    fun sortsRecentFirstThenAlphabetically() {
        val sorted = AppLogic.sorted(apps, SortOrder.RECENT, recent = listOf("net.move", "com.sys"))
        assertEquals("Move", sorted[0].label)
        assertEquals("System Tool", sorted[1].label)
        assertEquals("AllCast", sorted[2].label)
    }

    @Test
    fun manualOrderApplies() {
        val sorted = AppLogic.sorted(apps, SortOrder.MANUAL, manual = listOf("org.zed", "com.all"))
        assertEquals("Zed Player", sorted[0].label)
        assertEquals("AllCast", sorted[1].label)
        assertEquals("Mix", sorted[2].label)
    }

    @Test
    fun pushRecentDeduplicatesAndLimits() {
        val recent = listOf("a", "b", "c")
        assertEquals(listOf("c", "a", "b"), AppLogic.pushRecent(recent, "c"))
        assertEquals(listOf("d", "a"), AppLogic.pushRecent(listOf("a"), "d", limit = 2))
        assertEquals(emptyList<String>(), AppLogic.pushRecent(recent, "a", limit = 0))
    }

    @Test
    fun moveItemHandlesBounds() {
        val list = listOf("a", "b", "c")
        assertEquals(listOf("b", "a", "c"), AppLogic.moveItem(list, 0, 1))
        assertEquals(listOf("a", "c", "b"), AppLogic.moveItem(list, 2, 1))
        assertEquals(list, AppLogic.moveItem(list, 1, 1))
        assertEquals(list, AppLogic.moveItem(list, 9, 1))
    }
}
