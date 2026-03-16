package pt.hitv.core.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

/**
 * iOS implementation of [DatabaseDriverFactory].
 * Uses NativeSqliteDriver backed by SQLite C library.
 */
actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = HitvDatabase.Schema,
            name = "hitv.db"
        )
    }
}
