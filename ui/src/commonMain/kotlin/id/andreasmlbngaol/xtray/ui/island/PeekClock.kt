package id.andreasmlbngaol.xtray.ui.island

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.andreasmlbngaol.xtray.platform.ClockText

/**
 * HH / MM / ss stacked, with the seconds deliberately smaller. This is the
 * visual anchor of the peek level and keeps the digits legible at 420 px.
 */
@Composable
fun PeekClock(clock: ClockText, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = clock.hours,
            style = MaterialTheme.typography.titleLargeEmphasized.copy(
                fontSize = 26.sp,
                lineHeight = 26.sp,
                fontFeatureSettings = "tnum",
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = clock.minutes,
            style = MaterialTheme.typography.titleLargeEmphasized.copy(
                fontSize = 26.sp,
                lineHeight = 26.sp,
                fontFeatureSettings = "tnum",
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(1.dp))
        Text(
            text = clock.seconds,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 12.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFeatureSettings = "tnum",
            ),
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = clock.date,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
