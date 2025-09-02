package org.example.app

/**
 * PUBLIC_INTERFACE
 * CalculationHistory provides an in-memory, application-wide persistent store
 * for calculator expressions and results, intended for display in a modern UI panel.
 */
object CalculationHistory {
    private val maxEntries = 100
    // Data class to hold history entry with timestamp
    data class HistoryEntry(
        val expression: String,
        val result: String,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    private val entries = mutableListOf<HistoryEntry>()

    // PUBLIC_INTERFACE
    /** Adds a calculation to the history (expression, result). Oldest entries are dropped if over max limit. */
    fun add(expression: String, result: String) {
        if (expression.isBlank()) return
        entries.add(0, HistoryEntry(expression, result))
        if (entries.size > maxEntries) entries.removeLast()
    }

    // PUBLIC_INTERFACE
    /** Returns a copy of the current calculation history, most recent first. */
    fun getHistory(): List<HistoryEntry> = entries.toList()

    // PUBLIC_INTERFACE
    /** Removes all entries from history. */
    fun clear() {
        entries.clear()
    }
}
