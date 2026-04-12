package pt.hitv.feature.movies.detail.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pt.hitv.core.designsystem.theme.getThemeColors
import pt.hitv.core.model.movieInfo.Info

@Composable
fun MovieMetadata(
    movieInfo: Info,
    isCompact: Boolean = false
) {
    Column(verticalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 8.dp)) {
        movieInfo.releasedate?.takeIf { it.isNotBlank() }?.let { releaseDate ->
            MetaRow(label = "Released", value = releaseDate)
        }

        movieInfo.duration?.takeIf { it.isNotBlank() }?.let { duration ->
            MetaRow(label = "Duration", value = duration)
        }

        movieInfo.genre?.takeIf { it.isNotBlank() }?.let { genre ->
            MetaRow(label = "Genre", value = genre)
        }

        movieInfo.director?.takeIf { it.isNotBlank() }?.let { director ->
            MetaRow(label = "Director", value = director)
        }

        movieInfo.rating?.takeIf { it.isNotBlank() }?.let { rating ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "IMDb Rating",
                    tint = getThemeColors().primaryColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "$rating/10",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = getThemeColors().textColor
                )
            }
        }
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = getThemeColors().textColor.copy(alpha = 0.7f),
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = getThemeColors().textColor
        )
    }
}

@Composable
fun MovieDetailRow(label: String, value: String?) {
    if (!value.isNullOrBlank()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = getThemeColors().textColor.copy(alpha = 0.7f),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = getThemeColors().textColor,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
            )
        }
    }
}
