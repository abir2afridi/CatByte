package com.example.engine

/**
 * Editor keyboard mapping for tablets, Chromebooks and DeX. The Compose layer
 * only translates platform key events into [Key] values; the decision of what a
 * shortcut means lives here so it can be tested without a UI.
 */
object Shortcuts {

    enum class Key {
        SPACE, S, DELETE, BACKSPACE, Z, Y, PLUS, MINUS, LEFT, RIGHT, M, ESCAPE, UNKNOWN,
    }

    enum class Command {
        PLAY_PAUSE, SPLIT, DELETE, UNDO, REDO, ZOOM_IN, ZOOM_OUT, PREV_FRAME, NEXT_FRAME, TOGGLE_MUTE,
    }

    fun resolve(
        key: Key,
        ctrl: Boolean = false,
        shift: Boolean = false,
        alt: Boolean = false,
    ): Command? {
        if (alt) return null
        if (ctrl) {
            return when (key) {
                Key.Z -> if (shift) Command.REDO else Command.UNDO
                Key.Y -> Command.REDO
                Key.S -> Command.SPLIT
                Key.PLUS, Key.MINUS -> null
                else -> null
            }
        }
        return when (key) {
            Key.SPACE -> Command.PLAY_PAUSE
            Key.S -> Command.SPLIT
            Key.DELETE, Key.BACKSPACE -> Command.DELETE
            Key.PLUS -> Command.ZOOM_IN
            Key.MINUS -> Command.ZOOM_OUT
            Key.LEFT -> Command.PREV_FRAME
            Key.RIGHT -> Command.NEXT_FRAME
            Key.M -> Command.TOGGLE_MUTE
            Key.ESCAPE, Key.Z, Key.Y, Key.UNKNOWN -> null
        }
    }

    /** Human-readable hint for menus and tooltips, e.g. "Ctrl+Shift+Z". */
    fun hint(command: Command): String? = when (command) {
        Command.PLAY_PAUSE -> "Space"
        Command.SPLIT -> "S"
        Command.DELETE -> "Del"
        Command.UNDO -> "Ctrl+Z"
        Command.REDO -> "Ctrl+Shift+Z"
        Command.ZOOM_IN -> "+"
        Command.ZOOM_OUT -> "-"
        Command.PREV_FRAME -> "Left"
        Command.NEXT_FRAME -> "Right"
        Command.TOGGLE_MUTE -> "M"
    }
}
