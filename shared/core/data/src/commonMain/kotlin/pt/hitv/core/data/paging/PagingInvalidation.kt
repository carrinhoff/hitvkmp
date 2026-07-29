package pt.hitv.core.data.paging

import app.cash.paging.PagingSource
import app.cash.sqldelight.Query
import app.cash.sqldelight.db.SqlDriver

/**
 * Table names SQLDelight emits change notifications under. These are the generated
 * `notifyQueries { emit("Channel") }` keys, which are simply the table names — a typo here fails
 * silently (no notification ever arrives), so they are named once and reused.
 */
internal object PagedTables {
    const val CHANNEL = "Channel"
    const val MOVIE = "Movie"
    const val TV_SHOW = "TvShow"
    const val CUSTOM_GROUP_CHANNEL = "CustomGroupChannel"
    const val PARENTAL_CONTROL = "ParentalControl"
}

/**
 * Invalidates this [PagingSource] whenever any of [tables] changes.
 *
 * Room hands the original this for free: a Room-backed `PagingSource` registers with the
 * `InvalidationTracker`, so a sync that writes to `Channel` causes every visible paged list to
 * reload. SQLDelight has the same capability but does not wire it up, and the port never did
 * either — every paging source here read with a one-shot `executeAsList()` and then never
 * reconsidered.
 *
 * The effect was that a completed sync did not reach the screen. New channels did not appear,
 * removed ones stayed, and a toggled favourite did not show up in the Favourites list until the
 * screen was destroyed and recreated. It is worst exactly where it is hardest to notice on a
 * developer machine: on iOS the background BGTask sync finishes while the app is suspended, so the
 * user foregrounds the app to a list that silently predates the sync.
 *
 * The listener is removed via [PagingSource.registerInvalidatedCallback], so each source cleans up
 * after itself — Paging creates a fresh source (and thus a fresh registration) after every
 * invalidation, and leaking these would accumulate one dead listener per refresh for the life of
 * the process.
 */
internal fun PagingSource<*, *>.invalidateOnChangeTo(driver: SqlDriver, vararg tables: String) {
    val listener = Query.Listener { invalidate() }
    driver.addListener(queryKeys = tables, listener = listener)
    registerInvalidatedCallback {
        driver.removeListener(queryKeys = tables, listener = listener)
    }
}
