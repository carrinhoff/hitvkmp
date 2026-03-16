package pt.hitv.core.database

import app.cash.sqldelight.db.SqlDriver

/**
 * Platform-specific factory for creating SQLDelight database drivers.
 * Each platform (Android, iOS) provides its own implementation.
 */
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}
