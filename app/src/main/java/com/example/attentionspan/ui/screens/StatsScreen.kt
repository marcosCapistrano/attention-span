package com.example.attentionspan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.attentionspan.R
import com.example.attentionspan.data.local.entity.StudySession
import com.example.attentionspan.ui.viewmodel.FocusBadge
import com.example.attentionspan.ui.viewmodel.OverviewStats
import com.example.attentionspan.ui.viewmodel.SubjectWithStats
import com.example.attentionspan.ui.viewmodel.StudyViewModel
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StudyViewModel,
    onBack: () -> Unit
) {
    val overviewStats by viewModel.overviewStats.collectAsState(initial = OverviewStats())
    val subjectStats by viewModel.subjectStatistics.collectAsState(initial = emptyList())
    var selectedTab by remember { mutableIntStateOf(0) }
    var sessionToDelete by remember { mutableStateOf<StudySession?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.insights)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text(stringResource(R.string.overview), modifier = Modifier.padding(16.dp))
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text(stringResource(R.string.by_subject), modifier = Modifier.padding(16.dp))
                }
            }

            if (selectedTab == 0) {
                OverviewTab(overviewStats, subjectStats)
            } else {
                BySubjectTab(
                    subjectStats = subjectStats,
                    onDeleteSession = { sessionToDelete = it }
                )
            }
        }

        if (sessionToDelete != null) {
            AlertDialog(
                onDismissRequest = { sessionToDelete = null },
                title = { Text(stringResource(R.string.delete_session)) },
                text = { Text(stringResource(R.string.delete_session_confirm)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            sessionToDelete?.let { viewModel.deleteSession(it) }
                            sessionToDelete = null
                        }
                    ) {
                        Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { sessionToDelete = null }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}

@Composable
fun OverviewTab(stats: OverviewStats, subjectStats: List<SubjectWithStats>) {
    val rollingModelProducer = remember { CartesianChartModelProducer() }
    val comparisonModelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(stats.rolling7DayAverage) {
        if (stats.rolling7DayAverage.isNotEmpty()) {
            rollingModelProducer.runTransaction {
                lineSeries { series(stats.rolling7DayAverage.map { it.second.toFloat() }) }
            }
        }
    }

    LaunchedEffect(subjectStats) {
        if (subjectStats.isNotEmpty()) {
            comparisonModelProducer.runTransaction {
                columnSeries { series(subjectStats.map { it.averageMinutes.toFloat() }) }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(stringResource(R.string.avg_focus), "%.1f".format(stats.averageMinutes), stringResource(R.string.min_label), Modifier.weight(1f))
                StatCard(stringResource(R.string.best), "%.0f".format(stats.bestSessionMinutes), stringResource(R.string.min_label), Modifier.weight(1f))
                StatCard(stringResource(R.string.total), "${stats.totalSessions}", stringResource(R.string.sessions), Modifier.weight(1f))
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.rolling_7_day_average), style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (stats.rolling7DayAverage.isNotEmpty()) {
                        CartesianChartHost(
                            chart = rememberCartesianChart(
                                rememberLineCartesianLayer(),
                                startAxis = rememberStartAxis(),
                                bottomAxis = rememberBottomAxis(),
                            ),
                            modelProducer = rollingModelProducer,
                            modifier = Modifier.height(200.dp)
                        )
                    } else {
                        Box(Modifier.height(200.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.no_data))
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.subject_comparison), style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (subjectStats.isNotEmpty()) {
                        CartesianChartHost(
                            chart = rememberCartesianChart(
                                rememberColumnCartesianLayer(),
                                startAxis = rememberStartAxis(),
                                bottomAxis = rememberBottomAxis(),
                            ),
                            modelProducer = comparisonModelProducer,
                            modifier = Modifier.height(200.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BySubjectTab(
    subjectStats: List<SubjectWithStats>,
    onDeleteSession: (StudySession) -> Unit
) {
    var expandedSubjectId by remember { mutableStateOf<Long?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(subjectStats) { stats ->
            SubjectStatItem(
                stats = stats,
                isExpanded = expandedSubjectId == stats.subject.id,
                onClick = {
                    expandedSubjectId = if (expandedSubjectId == stats.subject.id) null else stats.subject.id
                },
                onDeleteSession = onDeleteSession
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SubjectStatItem(
    stats: SubjectWithStats,
    isExpanded: Boolean,
    onClick: () -> Unit,
    onDeleteSession: (StudySession) -> Unit
) {
    val historyModelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(stats.history, isExpanded) {
        if (isExpanded && stats.history.isNotEmpty()) {
            historyModelProducer.runTransaction {
                lineSeries { series(stats.history.takeLast(10).map { it.toFloat() }) }
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(stats.subject.name, style = MaterialTheme.typography.titleMedium)
                    FocusBadgeChip(stats.badge)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("%.1f min".format(stats.averageMinutes), fontWeight = FontWeight.Bold)
                    TrendIcon(stats.trend)
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    DetailStat(stringResource(R.string.best), "%.0f min".format(stats.bestMinutes))
                    DetailStat(stringResource(R.string.total), "${stats.sessionCount}")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(R.string.recent_history), style = MaterialTheme.typography.labelMedium)
                if (stats.history.isNotEmpty()) {
                   CartesianChartHost(
                       chart = rememberCartesianChart(rememberLineCartesianLayer()),
                       modelProducer = historyModelProducer,
                       modifier = Modifier.height(100.dp).fillMaxWidth()
                   )
                }

                if (stats.recentSessions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.recent_sessions_delete_hint), style = MaterialTheme.typography.labelMedium)
                    stats.recentSessions.forEach { session ->
                        val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                        val dateString = dateFormat.format(Date(session.startTime))
                        val durationMin = session.durationMillis / 60000
                        
                        ListItem(
                            headlineContent = { Text("$durationMin min") },
                            supportingContent = { Text(dateString) },
                            modifier = Modifier.combinedClickable(
                                onClick = {},
                                onLongClick = { onDeleteSession(session) }
                            ),
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FocusBadgeChip(badge: FocusBadge) {
    val (color, textRes) = when (badge) {
        FocusBadge.FOCUSING_WELL -> Color(0xFF4CAF50) to R.string.focusing_well
        FocusBadge.AVERAGE -> Color(0xFF2196F3) to R.string.average_focus_badge
        FocusBadge.HARDER_TO_FOCUS -> Color(0xFFF44336) to R.string.harder_to_focus
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = stringResource(textRes),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            fontSize = 10.sp,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TrendIcon(trend: Double) {
    val (icon, color) = when {
        trend > 1.0 -> Icons.AutoMirrored.Filled.TrendingUp to Color(0xFF4CAF50)
        trend < -1.0 -> Icons.AutoMirrored.Filled.TrendingDown to Color(0xFFF44336)
        else -> Icons.AutoMirrored.Filled.TrendingFlat to Color(0xFF9E9E9E)
    }
    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
}

@Composable
fun StatCard(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(unit, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun DetailStat(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
}
