package id.andreasmlbngaol.xtray.ui.modules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.andreasmlbngaol.xtray.core.IslandModule
import id.andreasmlbngaol.xtray.core.calendar.CalendarCell
import id.andreasmlbngaol.xtray.core.calendar.CalendarMath
import id.andreasmlbngaol.xtray.ui.island.rememberClockText

private const val FirstYear = 1970
private const val LastYear = 2100

/**
 * Month calendar. It replaces the calendar that used to live in the desktop
 * panel and is reachable from the rail and from the date in the header.
 *
 * Month and year are both selectable: stepping one month at a time across a
 * year boundary was the wrong way to get from December to January.
 */
class CalendarModule : IslandModule {
    override val id = "calendar"
    override val title = "Calendar"
    override val icon: ImageVector = Icons.Rounded.CalendarMonth
    /** Right after Controls, before the clipboard. */
    override val priority = 105

    @Composable
    override fun ExpandedContent() {
        val clock = rememberClockText()
        var year by remember { mutableIntStateOf(clock.year) }
        var month by remember { mutableIntStateOf(clock.month) }
        val calendar = remember(year, month) { CalendarMath.month(year, month) }

        fun step(months: Int) {
            val (nextYear, nextMonth) = CalendarMath.shift(year, month, months)
            year = nextYear
            month = nextMonth
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MonthPicker(month = month, onSelect = { month = it })
                YearPicker(year = year, onSelect = { year = it })
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { step(-1) }) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronLeft,
                        contentDescription = "Previous month",
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(onClick = { step(1) }) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = "Next month",
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                CalendarMath.weekdayLabels.forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                calendar.weeks.forEach { week ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        week.forEach { cell ->
                            DayCell(
                                cell = cell,
                                today = cell.inMonth &&
                                    year == clock.year &&
                                    month == clock.month &&
                                    cell.dayOfMonth == clock.dayOfMonth,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthPicker(month: Int, onSelect: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        PickerLabel(
            text = CalendarMath.monthNames[month - 1],
            open = open,
            onClick = { open = true },
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            CalendarMath.monthNames.forEachIndexed { index, name ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = name,
                            color = if (index == month - 1) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    },
                    onClick = {
                        onSelect(index + 1)
                        open = false
                    },
                )
            }
        }
    }
}

@Composable
private fun YearPicker(year: Int, onSelect: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        PickerLabel(
            text = year.toString(),
            open = open,
            onClick = { open = true },
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            val listState = rememberLazyListState()
            // Open with the selected year in view instead of at 1970.
            LaunchedEffect(Unit) {
                val index = (year - FirstYear).coerceIn(0, LastYear - FirstYear)
                listState.scrollToItem((index - 2).coerceAtLeast(0))
            }
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .width(132.dp)
                    .height(260.dp),
            ) {
                items(count = LastYear - FirstYear + 1) { index ->
                    val candidate = FirstYear + index
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = candidate.toString(),
                                color = if (candidate == year) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            )
                        },
                        onClick = {
                            onSelect(candidate)
                            open = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PickerLabel(text: String, open: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (open) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMediumEmphasized,
            )
            Icon(
                imageVector = Icons.Rounded.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DayCell(
    cell: CalendarCell,
    today: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxHeight(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (today) MaterialTheme.colorScheme.primary else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = cell.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                color = when {
                    today -> MaterialTheme.colorScheme.onPrimary
                    cell.inMonth -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                },
            )
        }
    }
}
