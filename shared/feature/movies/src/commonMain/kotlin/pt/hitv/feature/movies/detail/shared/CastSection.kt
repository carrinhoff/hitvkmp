package pt.hitv.feature.movies.detail.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import pt.hitv.core.designsystem.theme.getThemeColors
import pt.hitv.core.model.cast.Cast

@Composable
fun CastSection(castList: List<Cast>) {
    if (castList.isEmpty()) return

    Column {
        Text(
            text = "Cast",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = getThemeColors().textColor,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(castList.take(10)) { cast ->
                CastItem(cast = cast)
            }
        }
    }
}

@Composable
private fun CastItem(cast: Cast) {
    Column(
        modifier = Modifier.width(100.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            AsyncImage(
                model = "https://image.tmdb.org/t/p/w200${cast.profilePath}",
                contentDescription = cast.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = cast.name ?: "",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = getThemeColors().textColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            fontSize = 12.sp
        )
    }
}
