package pt.hitv.feature.series.detail.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pt.hitv.core.designsystem.theme.getThemeColors
import pt.hitv.core.model.seriesInfo.SeriesInfo

@Composable
fun SeriesMetadata(seriesInfo: SeriesInfo) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        seriesInfo.releaseDate?.takeIf { it.isNotEmpty() }?.let { releaseDate ->
            MetadataRow("Release Date", releaseDate)
        }

        seriesInfo.genre?.takeIf { it.isNotEmpty() }?.let { genre ->
            MetadataRow("Genre", genre)
        }

        seriesInfo.director?.takeIf { it.isNotEmpty() }?.let { director ->
            MetadataRow("Director", director)
        }

        seriesInfo.cast?.takeIf { it.isNotEmpty() }?.let { cast ->
            MetadataRow("Cast", cast)
        }

        seriesInfo.episodeRunTime?.takeIf { it.isNotEmpty() }?.let { runtime ->
            MetadataRow("Episode Runtime", "${runtime} min")
        }
    }
}

@Composable
fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "$label:",
            color = getThemeColors().textColor.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = value,
            color = getThemeColors().textColor,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
    }
}
