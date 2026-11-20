package com.vitrum.tv.core

import com.vitrum.tv.data.AppInfo
import com.vitrum.tv.data.SortOrder

/** Pure app list filtering and ordering, unit-tested on the JVM. */
object AppLogic {

    fun filter(
        apps: List<AppInfo>,
        query: String = "",
        hidden: Set<String> = emptySet(),
        showSystem: Boolean = false,
    ): List<AppInfo> {
        val trimmed = query.trim()
        return apps.asSequence()
            .filter { it.packageName !in hidden }
            .filter { showSystem || !it.isSystem }
            .filter {
                trimmed.isEmpty() ||
                    it.label.contains(trimmed, ignoreCase = true) ||
                    it.packageName.contains(trimmed, ignoreCase = true)
            }
            .toList()
    }

    fun sorted(
        apps: List<AppInfo>,
        order: SortOrder,
        recent: List<String> = emptyList(),
        manual: List<String> = emptyList(),
    ): List<AppInfo> {
        val alphabetical = Comparator<AppInfo> { first, second ->
            String.CASE_INSENSITIVE_ORDER.compare(first.label, second.label)
        }
        return when (order) {
            SortOrder.AZ -> apps.sortedWith(alphabetical)
            SortOrder.RECENT -> {
                val ranks = recent.withIndex().associate { (index, pkg) -> pkg to index }
                apps.sortedWith(
                    compareBy<AppInfo> { ranks[it.packageName] ?: Int.MAX_VALUE }
                        .then(alphabetical),
                )
            }
            SortOrder.MANUAL -> {
                val ranks = manual.withIndex().associate { (index, pkg) -> pkg to index }
                apps.sortedWith(
                    compareBy<AppInfo> { ranks[it.packageName] ?: Int.MAX_VALUE }
                        .then(alphabetical),
                )
            }
        }
    }

    /** Keeps the most recent package first, without duplicates. */
    fun pushRecent(recent: List<String>, packageName: String, limit: Int = 20): List<String> {
        if (limit <= 0) return emptyList()
        return (listOf(packageName) + recent.filterNot { it == packageName }).take(limit)
    }

    fun moveItem(list: List<String>, from: Int, to: Int): List<String> {
        if (from == to || from !in list.indices || to !in list.indices) return list
        val mutable = list.toMutableList()
        val item = mutable.removeAt(from)
        mutable.add(to, item)
        return mutable
    }
}
