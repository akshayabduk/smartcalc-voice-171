package org.example.app

/**
 * PUBLIC_INTERFACE
 * UndoRedoManager handles the undo/redo stack for calculator input states.
 * Uses two stacks to track the expression history and allow undo/redo operations.
 */
class UndoRedoManager {
    private val undoStack = mutableListOf<String>()
    private val redoStack = mutableListOf<String>()
    
    /**
     * PUBLIC_INTERFACE
     * Adds a new state to the undo history.
     * Clears redo stack as the timeline has diverged.
     */
    fun addState(state: String) {
        if (undoStack.isEmpty() || undoStack.last() != state) {
            undoStack.add(state)
            redoStack.clear()
        }
    }

    /**
     * PUBLIC_INTERFACE
     * Reverts to the previous state if available.
     * Returns null if no previous state exists.
     */
    fun undo(): String? {
        if (undoStack.size <= 1) return null
        val current = undoStack.removeAt(undoStack.lastIndex)
        redoStack.add(current)
        return undoStack.last()
    }

    /**
     * PUBLIC_INTERFACE
     * Restores a previously undone state if available.
     * Returns null if no state to restore.
     */
    fun redo(): String? {
        if (redoStack.isEmpty()) return null
        val state = redoStack.removeAt(redoStack.lastIndex)
        undoStack.add(state)
        return state
    }

    /**
     * PUBLIC_INTERFACE
     * Checks if undo operation is available.
     */
    fun canUndo(): Boolean = undoStack.size > 1

    /**
     * PUBLIC_INTERFACE
     * Checks if redo operation is available.
     */
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    /**
     * PUBLIC_INTERFACE
     * Clears all history stacks.
     */
    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}
