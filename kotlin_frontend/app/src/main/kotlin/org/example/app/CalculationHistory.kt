package org.example.app

/**
 * PUBLIC_INTERFACE
 * CalculationHistory provides an in-memory, application-wide persistent store
 * for calculator expressions and results, intended for display in a modern UI panel.
 */
object CalculationHistory {
    private val maxEntries = 100
    private val entries = mutableListOf<Pair<String, String>>() // Pair<expression, result>

    // PUBLIC_INTERFACE
    /** Adds a calculation to the history (expression, result). Oldest entries are dropped if over max limit. */
    fun add(expression: String, result: String) {
        if (expression.isBlank()) return
        entries.add(0, Pair(expression, result))
        if (entries.size > maxEntries) entries.removeLast()
    }

    // PUBLIC_INTERFACE
    /** Returns a copy of the current calculation history, most recent first. */
    fun getHistory(): List<Pair<String, String>> = entries.toList()

    // PUBLIC_INTERFACE
    /** Removes all entries from history. */
    fun clear() {
        entries.clear()
    }
}
