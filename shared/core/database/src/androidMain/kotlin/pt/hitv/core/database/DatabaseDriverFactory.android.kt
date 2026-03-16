package pt.hitv.core.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

/**
 * Android implementation of [DatabaseDriverFactory].
 * Uses AndroidSqliteDriver backed by the Android SQLite framework.
 */
actual class DatabaseDriverFactory(
    private val context: Context
) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = HitvDatabase.Schema,
            context = context,
            name = "hitv.db"
        )
    }
}
