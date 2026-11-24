package com.example.uth_todo.model

enum class ThemeMode {
    SYSTEM, LIGHT, DARK;

    fun usesDarkColors(systemDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemDark
        LIGHT -> false
        DARK -> true
    }

    companion object {
        fun fromStored(value: String?): ThemeMode = entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}
