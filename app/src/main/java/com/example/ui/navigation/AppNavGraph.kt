package com.example.ui.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.data.export.ExportFormat
import com.example.data.export.ExportScope
import com.example.data.export.FileExportHelper
import com.example.data.export.SaveDownloadResult
import com.example.domain.DateKey
import com.example.domain.DateUtil
import com.example.ui.components.ExportSheet
import com.example.ui.components.WeightEntrySheet
import com.example.ui.screens.CalendarHomeScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DayDetailScreen
import com.example.ui.viewmodel.DayDetailData
import com.example.ui.viewmodel.FitnessViewModel
import kotlinx.coroutines.launch

const val ROUTE_CALENDAR = "calendar_home"
const val ROUTE_DASHBOARD = "dashboard"
const val ROUTE_DAY_DETAIL = "day_detail/{dateIso}"

private fun dayDetailRoute(date: DateKey): String = "day_detail/${date.toIsoString()}"

private data class PendingSafExport(
    val fileName: String,
    val content: String,
)

@Composable
fun FitCalendarApp(
    viewModel: FitnessViewModel,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val calendarState by viewModel.calendarUiState.collectAsStateWithLifecycle()
    val dashboardState by viewModel.dashboardUiState.collectAsStateWithLifecycle()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showPrimaryNav = currentRoute == ROUTE_CALENDAR || currentRoute == ROUTE_DASHBOARD

    // Weight entry modal sheet state (targetDate = null means "today/now", non-null means specific date)
    var showWeightSheet by remember { mutableStateOf(false) }
    var weightSheetTargetDate by remember { mutableStateOf<DateKey?>(null) }

    // Export modal sheet state
    var showExportSheet by remember { mutableStateOf(false) }
    var pendingSafExport by remember { mutableStateOf<PendingSafExport?>(null) }

    // SAF fallback launcher for API 24-28 saving without storage permissions
    val safCreateDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*"),
    ) { uri ->
        val pending = pendingSafExport
        pendingSafExport = null
        if (uri != null && pending != null) {
            val result = FileExportHelper.writeToSafUri(context, uri, pending.content)
            scope.launch {
                if (result.isSuccess) {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.export_success_saved, pending.fileName),
                    )
                } else {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.export_error_generic, result.exceptionOrNull()?.localizedMessage ?: ""),
                    )
                }
            }
        }
    }

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun handleExportSaveToDownloads(exportScope: ExportScope, format: ExportFormat) {
        showExportSheet = false
        scope.launch {
            val payload = viewModel.buildExportPayload(exportScope, format)
            if (payload.recordCount == 0) {
                snackbarHostState.showSnackbar(context.getString(R.string.export_empty_data))
                return@launch
            }
            when (
                val res = FileExportHelper.saveToDownloads(
                    context = context,
                    fileName = payload.fileName,
                    mimeType = payload.mimeType,
                    content = payload.content,
                )
            ) {
                is SaveDownloadResult.Saved -> {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.export_success_saved, res.fileName),
                    )
                }
                is SaveDownloadResult.RequiresSaf -> {
                    pendingSafExport = PendingSafExport(res.fileName, res.content)
                    safCreateDocumentLauncher.launch(res.fileName)
                }
                is SaveDownloadResult.Failed -> {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.export_error_generic, res.reason),
                    )
                }
            }
        }
    }

    fun handleExportShare(exportScope: ExportScope, format: ExportFormat) {
        showExportSheet = false
        scope.launch {
            val payload = viewModel.buildExportPayload(exportScope, format)
            if (payload.recordCount == 0) {
                snackbarHostState.showSnackbar(context.getString(R.string.export_empty_data))
                return@launch
            }
            val result = FileExportHelper.shareExportFile(
                context = context,
                fileName = payload.fileName,
                mimeType = payload.mimeType,
                content = payload.content,
            )
            if (result.isFailure) {
                snackbarHostState.showSnackbar(
                    context.getString(R.string.export_error_generic, result.exceptionOrNull()?.localizedMessage ?: ""),
                )
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 840.dp

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (showPrimaryNav && !useNavigationRail) {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars,
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                    ) {
                        val calSelected = currentRoute == ROUTE_CALENDAR
                        val dashSelected = currentRoute == ROUTE_DASHBOARD
                        NavigationBarItem(
                            selected = calSelected,
                            onClick = { navigateTopLevel(ROUTE_CALENDAR) },
                            icon = {
                                Icon(
                                    imageVector = if (calSelected) Icons.Rounded.CalendarMonth else Icons.Outlined.CalendarMonth,
                                    contentDescription = stringResource(R.string.nav_calendar),
                                )
                            },
                            label = { Text(stringResource(R.string.nav_calendar)) },
                            modifier = Modifier.testTag("nav_calendar_tab"),
                        )
                        NavigationBarItem(
                            selected = dashSelected,
                            onClick = { navigateTopLevel(ROUTE_DASHBOARD) },
                            icon = {
                                Icon(
                                    imageVector = if (dashSelected) Icons.Rounded.BarChart else Icons.Outlined.BarChart,
                                    contentDescription = stringResource(R.string.nav_dashboard),
                                )
                            },
                            label = { Text(stringResource(R.string.nav_dashboard)) },
                            modifier = Modifier.testTag("nav_dashboard_tab"),
                        )
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (showPrimaryNav && useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        val calSelected = currentRoute == ROUTE_CALENDAR
                        val dashSelected = currentRoute == ROUTE_DASHBOARD
                        NavigationRailItem(
                            selected = calSelected,
                            onClick = { navigateTopLevel(ROUTE_CALENDAR) },
                            icon = {
                                Icon(
                                    imageVector = if (calSelected) Icons.Rounded.CalendarMonth else Icons.Outlined.CalendarMonth,
                                    contentDescription = stringResource(R.string.nav_calendar),
                                )
                            },
                            label = { Text(stringResource(R.string.nav_calendar)) },
                        )
                        NavigationRailItem(
                            selected = dashSelected,
                            onClick = { navigateTopLevel(ROUTE_DASHBOARD) },
                            icon = {
                                Icon(
                                    imageVector = if (dashSelected) Icons.Rounded.BarChart else Icons.Outlined.BarChart,
                                    contentDescription = stringResource(R.string.nav_dashboard),
                                )
                            },
                            label = { Text(stringResource(R.string.nav_dashboard)) },
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_CALENDAR,
                    ) {
                        composable(ROUTE_CALENDAR) {
                            CalendarHomeScreen(
                                uiState = calendarState,
                                onPreviousMonth = viewModel::previousMonth,
                                onNextMonth = viewModel::nextMonth,
                                onJumpToCurrentMonth = viewModel::jumpToCurrentMonth,
                                onDateClick = { date ->
                                    navController.navigate(dayDetailRoute(date))
                                },
                                onQuickLogWeightClick = {
                                    weightSheetTargetDate = null
                                    showWeightSheet = true
                                },
                            )
                        }

                        composable(ROUTE_DASHBOARD) {
                            DashboardScreen(
                                uiState = dashboardState,
                                onPeriodTypeChange = viewModel::setPeriodType,
                                onPreviousPeriod = viewModel::previousPeriod,
                                onNextPeriod = viewModel::nextPeriod,
                                onJumpToCurrentPeriod = viewModel::jumpToCurrentPeriod,
                                onSelectTrendMetric = viewModel::selectTrendMetric,
                                onOpenExportSheet = { showExportSheet = true },
                                onQuickLogWeightClick = {
                                    weightSheetTargetDate = null
                                    showWeightSheet = true
                                },
                            )
                        }

                        composable(
                            route = ROUTE_DAY_DETAIL,
                            arguments = listOf(navArgument("dateIso") { type = NavType.StringType }),
                        ) { entry ->
                            val dateIso = entry.arguments?.getString("dateIso") ?: DateUtil.today().toIsoString()
                            val dateKey = remember(dateIso) { DateKey.parse(dateIso) }
                            val dayDetailFlow = remember(dateIso) { viewModel.observeDayDetail(dateIso) }
                            val dayDetail by dayDetailFlow.collectAsStateWithLifecycle(initialValue = DayDetailData())

                            DayDetailScreen(
                                date = dateKey,
                                dayData = dayDetail,
                                onNavigateBack = { navController.popBackStack() },
                                onSaveWorkout = { u, l, c, s, km ->
                                    viewModel.saveDailyWorkout(dateIso, u, l, c, s, km)
                                },
                                onClearWorkout = {
                                    viewModel.clearDailyWorkout(dateIso)
                                },
                                onAddWeightClick = {
                                    weightSheetTargetDate = dateKey
                                    showWeightSheet = true
                                },
                                onDeleteWeight = { id ->
                                    viewModel.deleteWeightRecord(id)
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showWeightSheet) {
        WeightEntrySheet(
            targetDate = weightSheetTargetDate,
            suggestedWeightKg = calendarState.latestWeight?.weightKg,
            onDismiss = { showWeightSheet = false },
            onSave = { weightKg, hour, minute, note ->
                val target = weightSheetTargetDate ?: DateUtil.today()
                viewModel.addWeightForDate(target, weightKg, hour, minute, note)
                showWeightSheet = false
                scope.launch {
                    snackbarHostState.showSnackbar("已记录体重 ${String.format(java.util.Locale.US, "%.1f", weightKg)} kg")
                }
            },
        )
    }

    if (showExportSheet) {
        ExportSheet(
            currentPeriodLabel = dashboardState.periodLabel,
            onDismiss = { showExportSheet = false },
            onSaveToDownloads = ::handleExportSaveToDownloads,
            onShare = ::handleExportShare,
        )
    }
}
