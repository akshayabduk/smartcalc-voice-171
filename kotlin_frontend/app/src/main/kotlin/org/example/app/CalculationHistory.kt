package org.example.app

/**
 * PUBLIC_INTERFACE
 * CalculationHistory provides an in-memory, application-wide persistent store
 * for calculator expressions and results, intended for display in a modern UI panel.
 */
object CalculationHistory {
    private val maxEntries = 100
    // Data class to hold history entry with timestamp and favorite status
    data class HistoryEntry(
        val expression: String,
        val result: String,
        val timestamp: Long = System.currentTimeMillis(),
        var isFavorite: Boolean = false
    )
    
    private val entries = mutableListOf<HistoryEntry>()
    private var showOnlyFavorites = false

    // PUBLIC_INTERFACE
    /** Toggles favorite status for an entry at the specified position. */
    fun toggleFavorite(position: Int): Boolean {
        if (position in 0 until entries.size) {
            entries[position] = entries[position].copy(isFavorite = !entries[position].isFavorite)
            return true
        }
        return false
    }

    // PUBLIC_INTERFACE
    /** Sets filter to show only favorites or all entries. */
    fun setShowOnlyFavorites(onlyFavorites: Boolean) {
        showOnlyFavorites = onlyFavorites
    }

    // PUBLIC_INTERFACE
    /** Adds a calculation to the history (expression, result). Oldest entries are dropped if over max limit. */
    fun add(expression: String, result: String) {
        if (expression.isBlank()) return
        entries.add(0, HistoryEntry(expression, result))
        if (entries.size > maxEntries) entries.removeLast()
    }

    // PUBLIC_INTERFACE
    /** Returns a copy of the current calculation history, most recent first.
     * If showOnlyFavorites is true, returns only favorite entries. */
    fun getHistory(): List<HistoryEntry> = 
        if (showOnlyFavorites) entries.filter { it.isFavorite }.toList()
        else entries.toList()

    // PUBLIC_INTERFACE
    /** Removes an entry at the specified position. */
    fun removeAt(position: Int) {
        if (position in 0 until entries.size) {
            entries.removeAt(position)
        }
    }

    // PUBLIC_INTERFACE
    /** Removes all entries from history. */
    fun clear() {
        entries.clear()
    }
}
