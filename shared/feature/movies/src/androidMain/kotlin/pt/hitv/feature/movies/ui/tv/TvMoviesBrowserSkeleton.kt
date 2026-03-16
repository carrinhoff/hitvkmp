package pt.hitv.feature.movies.ui.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pt.hitv.core.designsystem.compose.shimmerBrush
import pt.hitv.core.designsystem.theme.getThemeColors

/**
 * Skeleton for the TV Movies Browser layout.
 * Android-only (TV platform).
 */
@Composable
fun TvMoviesBrowserSkeleton() {
    val themeColors = getThemeColors()
    val brush = shimmerBrush()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(themeColors.backgroundPrimary)
    ) {
        Column(
            modifier = Modifier
                .width(280.dp)
                .fillMaxHeight()
                .padding(vertical = 32.dp, horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(brush, RoundedCornerShape(24.dp))
            )

            Spacer(modifier = Modifier.height(24.dp))

            repeat(6) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(32.dp)
                        .background(brush, RoundedCornerShape(8.dp))
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 32.dp, end = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(30.dp)
                    .background(brush, RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.height(32.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                userScrollEnabled = false
            ) {
                items(12) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(2f / 3f)
                            .background(brush, RoundedCornerShape(12.dp))
                    )
                }
            }
        }
    }
}
