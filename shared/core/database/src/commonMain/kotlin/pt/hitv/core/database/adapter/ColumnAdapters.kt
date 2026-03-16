package pt.hitv.core.database.adapter

import app.cash.sqldelight.ColumnAdapter

/**
 * Column adapter for Boolean values stored as INTEGER (0/1) in SQLite.
 * SQLDelight handles this natively with `INTEGER AS Boolean`, but this adapter
 * is provided for explicit use cases.
 */
val booleanAdapter = object : ColumnAdapter<Boolean, Long> {
    override fun decode(databaseValue: Long): Boolean = databaseValue != 0L
    override fun encode(value: Boolean): Long = if (value) 1L else 0L
}

/**
 * Column adapter for Int values stored as INTEGER in SQLite.
 */
val intAdapter = object : ColumnAdapter<Int, Long> {
    override fun decode(databaseValue: Long): Int = databaseValue.toInt()
    override fun encode(value: Int): Long = value.toLong()
}

/**
 * Column adapter for List<String> stored as JSON TEXT in SQLite.
 * Uses a simple comma-separated format for basic lists,
 * or JSON array format for complex strings.
 */
val stringListAdapter = object : ColumnAdapter<List<String>, String> {
    override fun decode(databaseValue: String): List<String> {
        if (databaseValue.isBlank()) return emptyList()
        // Handle JSON array format: ["item1","item2"]
        if (databaseValue.startsWith("[") && databaseValue.endsWith("]")) {
            return databaseValue
                .removePrefix("[")
                .removeSuffix("]")
                .split(",")
                .map { it.trim().removeSurrounding("\"") }
                .filter { it.isNotEmpty() }
        }
        // Fallback: comma-separated
        return databaseValue.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    override fun encode(value: List<String>): String {
        return value.joinToString(",") { "\"$it\"" }.let { "[$it]" }
    }
}
