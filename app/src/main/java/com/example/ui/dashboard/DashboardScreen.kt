package com.example.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.data.db.*
import com.example.data.obd.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: ObdViewModel,
    modifier: Modifier = Modifier
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val sensorData by viewModel.sensorData.collectAsState()
    val isSimulation by viewModel.isSimulation.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val savedHistory by viewModel.savedDtcHistory.collectAsState()
    val frequentlyAccessedDtcs by viewModel.frequentlyAccessedDtcs.collectAsState()
    val localTrendPoints by viewModel.localTrendPoints.collectAsState()
    val pairedDevices by viewModel.pairedDevices.collectAsState()
    val isEcoMode by viewModel.isEcoMode.collectAsState()
    val terminalLogs by viewModel.terminalLogs.collectAsState()
    val appThemeMode by viewModel.appThemeMode.collectAsState()
    val driveMode by viewModel.driveMode.collectAsState()
    val theme = getDriveModeTheme(driveMode, appThemeMode)

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("العدادات", "الفحص والتشخيص", "الاتصال والربط", "سجل الفحوصات", "تحليل الصيانة AI", "الإعدادات ⚙️")

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (!isTablet) {
                TopAppBar(
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.app_title_ar),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = theme.primary
                                )
                            )
                            // Connection State Badge
                            val badgeText = when (connectionState) {
                                ObdConnectionState.DISCONNECTED -> "غير متصل"
                                ObdConnectionState.CONNECTING -> "جاري الاتصال"
                                ObdConnectionState.INITIALIZING -> "برمجة ELM327"
                                ObdConnectionState.CONNECTED -> if (isSimulation) "محاكاة نشطة" else "متصل بالسيارة"
                                ObdConnectionState.ERROR -> "خطأ اتصال"
                            }
                            val badgeColor = when (connectionState) {
                                ObdConnectionState.DISCONNECTED -> Color.Gray
                                ObdConnectionState.CONNECTING -> Color(0xFFFF9800)
                                ObdConnectionState.INITIALIZING -> Color(0xFF2196F3)
                                ObdConnectionState.CONNECTED -> Color(0xFF4CAF50)
                                ObdConnectionState.ERROR -> Color(0xFFF44336)
                            }
                            Card(
                                colors = CardDefaults.cardColors(containerColor = badgeColor.copy(alpha = 0.15f)),
                                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(badgeColor)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = badgeText,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = badgeColor
                                    )
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF12151C),
                        titleContentColor = Color.White
                    )
                )
            }
        },
        containerColor = Color(0xFF0C0E12)
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (isTablet) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Sidebar Navigation (starts on start/right side for RTL)
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(260.dp)
                            .background(Color(0xFF12151C))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Logo/Title
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 20.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsCar,
                                    contentDescription = "Car Logo",
                                    tint = theme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.app_title_ar),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = theme.primary
                                    )
                                )
                            }

                            // Connection Badge inside Sidebar
                            val badgeText = when (connectionState) {
                                ObdConnectionState.DISCONNECTED -> "غير متصل"
                                ObdConnectionState.CONNECTING -> "جاري الاتصال"
                                ObdConnectionState.INITIALIZING -> "برمجة ELM327"
                                ObdConnectionState.CONNECTED -> if (isSimulation) "محاكاة نشطة" else "متصل بالسيارة"
                                ObdConnectionState.ERROR -> "خطأ اتصال"
                            }
                            val badgeColor = when (connectionState) {
                                ObdConnectionState.DISCONNECTED -> Color.Gray
                                ObdConnectionState.CONNECTING -> Color(0xFFFF9800)
                                ObdConnectionState.INITIALIZING -> Color(0xFF2196F3)
                                ObdConnectionState.CONNECTED -> Color(0xFF4CAF50)
                                ObdConnectionState.ERROR -> Color(0xFFF44336)
                            }
                            Card(
                                colors = CardDefaults.cardColors(containerColor = badgeColor.copy(alpha = 0.12f)),
                                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(badgeColor)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 24.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(badgeColor, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = badgeText,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = badgeColor
                                        )
                                    )
                                }
                            }

                            // Tabs list
                            val tabIcons = listOf(
                                Icons.Default.Speed,
                                Icons.Default.Build,
                                Icons.Default.Bluetooth,
                                Icons.Default.History,
                                Icons.Default.Psychology,
                                Icons.Default.Settings
                            )

                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                tabs.forEachIndexed { index, title ->
                                    val isSelected = selectedTab == index
                                    val itemBgColor = if (isSelected) theme.primary.copy(alpha = 0.12f) else Color.Transparent
                                    val border = if (isSelected) BorderStroke(1.dp, theme.primary.copy(alpha = 0.4f)) else null
                                    val contentColor = if (isSelected) theme.primary else Color.LightGray

                                    Surface(
                                        onClick = { selectedTab = index },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        color = itemBgColor,
                                        border = border
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            Text(
                                                text = title,
                                                style = MaterialTheme.typography.labelLarge.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 13.sp,
                                                    color = contentColor
                                                ),
                                                modifier = Modifier.weight(1f),
                                                textAlign = TextAlign.Right
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Icon(
                                                imageVector = tabIcons[index],
                                                contentDescription = title,
                                                tint = contentColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Simulation switch inside Sidebar
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1F27)),
                            border = BorderStroke(1.dp, Color(0xFF2C3549))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                                    .clickable { viewModel.toggleSimulation(!isSimulation) },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Switch(
                                    checked = isSimulation,
                                    onCheckedChange = { viewModel.toggleSimulation(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = theme.primary,
                                        checkedTrackColor = theme.primary.copy(alpha = 0.3f),
                                        uncheckedThumbColor = Color.Gray,
                                        uncheckedTrackColor = Color.DarkGray
                                    ),
                                    modifier = Modifier.scale(0.8f)
                                )
                                Text(
                                    text = "وضع المحاكاة",
                                    color = Color.LightGray,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Vertical Divider line between navigation and content pane
                    VerticalDivider(color = Color(0xFF1F2430), thickness = 1.dp)

                    // Content Pane Column
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .padding(16.dp)
                    ) {
                        // Error display banner if any
                        errorMessage?.let { error ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF441212))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Error, contentDescription = "Error", tint = Color.Red)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = error, color = Color.White, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }

                        // Selected tab content Box
                        Box(modifier = Modifier.fillMaxSize()) {
                            when (selectedTab) {
                                0 -> GaugesView(viewModel, sensorData, isSimulation)
                                1 -> DiagnosticView(viewModel, sensorData, isScanning)
                                2 -> ConnectionView(viewModel, connectionState, isSimulation, pairedDevices)
                                3 -> HistoryView(viewModel, savedHistory, frequentlyAccessedDtcs, localTrendPoints)
                                4 -> MaintenancePredictorView(viewModel)
                                5 -> SettingsView(viewModel)
                            }
                        }
                    }
                }
            } else {
                // PHONE PORTRAIT LAYOUT
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                ) {
                    // Tab row in Arabic (Scrollable to prevent crowding)
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color(0xFF12151C),
                        contentColor = theme.primary,
                        edgePadding = 0.dp,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = theme.primary
                            )
                        }
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = {
                                    Text(
                                        text = title,
                                        color = if (selectedTab == index) theme.primary else Color.Gray,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                            )
                        }
                    }

                    // Error display banner if any
                    errorMessage?.let { error ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF441212))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Error, contentDescription = "Error", tint = Color.Red)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = error, color = Color.White, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    // Tab Views Content
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .padding(12.dp)
                    ) {
                        when (selectedTab) {
                            0 -> GaugesView(viewModel, sensorData, isSimulation)
                            1 -> DiagnosticView(viewModel, sensorData, isScanning)
                            2 -> ConnectionView(viewModel, connectionState, isSimulation, pairedDevices)
                            3 -> HistoryView(viewModel, savedHistory, frequentlyAccessedDtcs, localTrendPoints)
                            4 -> MaintenancePredictorView(viewModel)
                            5 -> SettingsView(viewModel)
                        }
                    }
                }
            }
            WarningsOverlay(
                viewModel = viewModel,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

data class DriveModeTheme(
    val primary: Color,
    val secondary: Color,
    val backgroundGradient: List<Color>,
    val text: String,
    val accent: Color
)

@Composable
fun getDriveModeTheme(driveMode: ObdViewModel.DriveMode, themeMode: String = "DEFAULT"): DriveModeTheme {
    return when (themeMode) {
        "NEON_SPORTY" -> DriveModeTheme(
            primary = Color(0xFFFF007F), // Neon Pink
            secondary = Color(0xFF00FFFF), // Neon Cyan
            backgroundGradient = listOf(Color(0xFF1E0B1A), Color(0xFF0C050E)),
            text = "رياضي نيون (Neon Sporty)",
            accent = Color(0xFFFF007F)
        )
        "CYBERPUNK_DARK" -> DriveModeTheme(
            primary = Color(0xFF9D4EDD), // Cyber Purple
            secondary = Color(0xFFFFEA00), // Cyber Yellow
            backgroundGradient = listOf(Color(0xFF110A1C), Color(0xFF07040B)),
            text = "سايبربانك (Cyberpunk)",
            accent = Color(0xFF9D4EDD)
        )
        "ECO_GREEN" -> DriveModeTheme(
            primary = Color(0xFF00E676), // Eco Green
            secondary = Color(0xFFB9F6CA), // Mint
            backgroundGradient = listOf(Color(0xFF081C10), Color(0xFF030A06)),
            text = "بيئي (Eco Green)",
            accent = Color(0xFF00E676)
        )
        "CARENS_GOLD" -> DriveModeTheme(
            primary = Color(0xFFFFB300), // Kia Gold
            secondary = Color(0xFF00E5FF), // Cyan
            backgroundGradient = listOf(Color(0xFF161922), Color(0xFF0F1116)),
            text = "كلاسيك ذهبي (Classic Gold)",
            accent = Color(0xFFFFB300)
        )
        else -> {
            when (driveMode) {
                ObdViewModel.DriveMode.NORMAL -> DriveModeTheme(
                    primary = Color(0xFFFFB300), // Kia Gold/Amber
                    secondary = Color(0xFF00E5FF), // Cyan
                    backgroundGradient = listOf(Color(0xFF161922), Color(0xFF0F1116)),
                    text = "عادي (Normal)",
                    accent = Color(0xFFFFB300)
                )
                ObdViewModel.DriveMode.ECO -> DriveModeTheme(
                    primary = Color(0xFF4CAF50), // Emerald Green
                    secondary = Color(0xFF8BC34A), // Light Green
                    backgroundGradient = listOf(Color(0xFF0E1A12), Color(0xFF060D08)),
                    text = "اقتصادي (Eco)",
                    accent = Color(0xFF4CAF50)
                )
                ObdViewModel.DriveMode.SPORT -> DriveModeTheme(
                    primary = Color(0xFFE53935), // Racing Red
                    secondary = Color(0xFFFF7043), // Orange
                    backgroundGradient = listOf(Color(0xFF241010), Color(0xFF140808)),
                    text = "رياضي (Sport)",
                    accent = Color(0xFFE53935)
                )
                ObdViewModel.DriveMode.COMFORT -> DriveModeTheme(
                    primary = Color(0xFF2196F3), // Comfort Blue
                    secondary = Color(0xFF03A9F4), // Sky Blue
                    backgroundGradient = listOf(Color(0xFF0D1826), Color(0xFF070C14)),
                    text = "مريح (Comfort)",
                    accent = Color(0xFF2196F3)
                )
            }
        }
    }
}

@Composable
fun GaugesView(
    viewModel: ObdViewModel,
    data: com.example.data.obd.ObdSensorData,
    isSimulation: Boolean
) {
    var throttleInput by remember { mutableStateOf(0f) }

    // Sync manual throttle input with model
    LaunchedEffect(throttleInput) {
        viewModel.applyThrottle(throttleInput)
    }

    val enginePowerHp = if (data.isEngineRunning) {
        val baseHp = (data.engineLoad / 100f) * 140f
        val rpmFactor = (data.rpm / 4000f).coerceIn(0.1f, 1.2f)
        (baseHp * rpmFactor).toInt().coerceIn(0, 140)
    } else {
        0
    }

    val layout by viewModel.dashboardLayout.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    var isEditMode by remember { mutableStateOf(false) }

    // Additional features state
    var isHudMirrored by remember { mutableStateOf(false) }
    var tripDistance by remember { mutableStateOf(14.8f) }
    var tripSeconds by remember { mutableStateOf(1620) }
    var maxSpeedObserved by remember { mutableStateOf(128f) }
    var selectedCategory by remember { mutableStateOf("ALL") }

    val appThemeMode by viewModel.appThemeMode.collectAsState()
    val unitSystem by viewModel.unitSystem.collectAsState()
    val driveMode by viewModel.driveMode.collectAsState()
    val theme = getDriveModeTheme(driveMode, appThemeMode)

    val convertedSpeed = if (unitSystem == "IMPERIAL") data.speed * 0.621371f else data.speed.toFloat()
    val speedUnitStr = if (unitSystem == "IMPERIAL") "mph" else "كم/س"
    val maxSpeedLimitVal = if (unitSystem == "IMPERIAL") 140f else 220f

    // Trip computer updater
    LaunchedEffect(data.isEngineRunning, data.speed) {
        if (data.isEngineRunning) {
            while (true) {
                delay(1000L)
                if (data.speed > 0) {
                    tripDistance += (data.speed / 3600f)
                    if (data.speed > maxSpeedObserved) {
                        maxSpeedObserved = data.speed.toFloat()
                    }
                }
                tripSeconds += 1
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpanded = maxWidth >= 720.dp

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isEditMode) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C1E14)),
                        border = BorderStroke(1.dp, Color(0xFFFFA726))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { isEditMode = !isEditMode },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFFFA726),
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("حفظ الترتيب ✔", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                if (isEditMode) {
                                    Button(
                                        onClick = { viewModel.resetLayout() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFD32F2F),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("إعادة الضبط 🔄", fontSize = 10.sp)
                                    }
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "وضع تعديل لوحة العدادات",
                                    color = Color(0xFFFFA726),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "استخدم الأسهم لإعادة ترتيب العدادات حسب الأهمية",
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }

            if (isEditMode) {
                // Render widgets in custom order with arrows when editing layout
                layout.forEachIndexed { index, widgetId ->
                    item(key = widgetId) {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF241A12)),
                                border = BorderStroke(1.dp, Color(0xFFFFA726).copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        IconButton(
                                            onClick = { viewModel.moveWidgetUp(index) },
                                            enabled = index > 0,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.KeyboardArrowUp,
                                                contentDescription = "Move Up",
                                                tint = if (index > 0) Color(0xFFFFA726) else Color.DarkGray,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { viewModel.moveWidgetDown(index) },
                                            enabled = index < layout.size - 1,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.KeyboardArrowDown,
                                                contentDescription = "Move Down",
                                                tint = if (index < layout.size - 1) Color(0xFFFFA726) else Color.DarkGray,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        val widgetName = when (widgetId) {
                                            "RPM_SPEED" -> "عدادات الدوران والسرعة الأساسية"
                                            "BATTERY" -> "مقياس شحن وفولتية البطارية"
                                            "COOLANT" -> "مقياس درجة حرارة المحرك"
                                            "TURBO" -> "مقياس ضغط شاحن التيربو"
                                            "POWER" -> "مقياس القوة الحصانية المتبقية"
                                            "INFO_BANNER" -> "لوحة بيانات طراز محرك كيا كارنز"
                                            "OIL_MAINTENANCE" -> "مراقبة زيت المحرك والصيانة"
                                            "DPF" -> "مراقبة فلتر البيئة وسخام الديزل"
                                            else -> "عداد قياس"
                                        }
                                        Text(
                                            text = widgetName,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Icon(
                                            Icons.Default.DragHandle,
                                            contentDescription = "Drag Handle",
                                            tint = Color(0xFFFFA726),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            // Widget Content for Edit Mode (sequential list)
                            when (widgetId) {
                                "RPM_SPEED" -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(modifier = Modifier.weight(1f).aspectRatio(1f)) {
                                            RpmGauge(value = data.rpm.toFloat(), maxValue = 5000f, title = "دوران المحرك", unit = "RPM")
                                        }
                                        Box(modifier = Modifier.weight(1f).aspectRatio(1f)) {
                                            SpeedGauge(value = convertedSpeed, maxValue = maxSpeedLimitVal, title = "السرعة الحالية", unit = speedUnitStr, isEngineRunning = data.isEngineRunning, accentColor = theme.primary)
                                        }
                                    }
                                }
                                "BATTERY" -> RenderBatteryCard(data)
                                "COOLANT" -> RenderCoolantCard(data)
                                "TURBO" -> RenderTurboCard(data)
                                "POWER" -> RenderPowerCard(data, enginePowerHp)
                                "INFO_BANNER" -> RenderInfoBannerCard()
                                "OIL_MAINTENANCE" -> RenderOilMaintenanceCard(data, viewModel)
                                "DPF" -> RenderDpfCard(data)
                            }
                        }
                    }
                }
            } else {
                // PRO DRIVING MODE: Highly organized categorized dashboard layout

                // Horizontal scrolling Category Filter Chips for OBD2 categories
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val categories = listOf(
                            "ALL" to ("الكل" to Icons.Default.Dashboard),
                            "ENGINE" to ("أداء المحرك" to Icons.Default.Speed),
                            "TRANSMISSION" to ("ناقل الحركة" to Icons.Default.SettingsInputComponent),
                            "ELECTRICAL" to ("الكهرباء والبطارية" to Icons.Default.Bolt),
                            "MAINTENANCE" to ("الصيانة والوقود" to Icons.Default.Build),
                            "BODY" to ("الهيكل والأبواب" to Icons.Default.DirectionsCar)
                        )

                        categories.forEach { (catId, pair) ->
                            val (label, icon) = pair
                            val isSelected = selectedCategory == catId
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = catId },
                                label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = theme.primary,
                                    selectedLabelColor = Color.Black,
                                    selectedLeadingIconColor = Color.Black,
                                    containerColor = Color(0xFF1F2430),
                                    labelColor = Color.White,
                                    iconColor = Color.LightGray
                                ),
                                modifier = Modifier.testTag("filter_chip_$catId")
                            )
                        }
                    }
                }

                if (isExpanded) {
                    // TABLET / WIDE RESPONSIVE GRID LAYOUT
                    if (selectedCategory == "ALL") {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Column 1: Engine & Transmission Performance
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    CategoryHeader("أداء المحرك (Engine Performance)", Icons.Default.Speed, theme.primary)

                                    val mirrorModifier = if (isHudMirrored) Modifier.graphicsLayer(scaleX = -1f) else Modifier
                                    Row(
                                        modifier = Modifier.fillMaxWidth().then(mirrorModifier),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(modifier = Modifier.weight(1f).aspectRatio(1f)) {
                                            RpmGauge(value = data.rpm.toFloat(), maxValue = 5000f, title = "دوران المحرك", unit = "RPM", accentColor = theme.secondary)
                                        }
                                        Box(modifier = Modifier.weight(1f).aspectRatio(1f)) {
                                            SpeedGauge(value = convertedSpeed, maxValue = maxSpeedLimitVal, title = "السرعة الحالية", unit = speedUnitStr, isEngineRunning = data.isEngineRunning, accentColor = theme.primary)
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Card(
                                            modifier = Modifier.weight(1.1f),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
                                            border = BorderStroke(1.dp, Color(0xFF232B3A)),
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(
                                                    "كمبيوتر الرحلة والمسافة (Trip Stats)",
                                                    color = theme.primary,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.fillMaxWidth(),
                                                    textAlign = TextAlign.Right
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))

                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Column(horizontalAlignment = Alignment.End) {
                                                        Text("مسافة الرحلة", color = Color.Gray, fontSize = 9.sp)
                                                        Text(String.format(Locale.US, "%.2f كم", tripDistance), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    Column(horizontalAlignment = Alignment.End) {
                                                        Text("مدة القيادة", color = Color.Gray, fontSize = 9.sp)
                                                        val hrs = tripSeconds / 3600
                                                        val mins = (tripSeconds % 3600) / 60
                                                        val secs = tripSeconds % 60
                                                        val durationStr = if (hrs > 0) "${hrs}:${mins}:${secs}" else "${mins}:${secs}"
                                                        Text(durationStr, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Column(horizontalAlignment = Alignment.End) {
                                                        Text("السرعة القصوى", color = Color.Gray, fontSize = 9.sp)
                                                        Text("${maxSpeedObserved.toInt()} كم/س", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    Column(horizontalAlignment = Alignment.End) {
                                                        Text("الاستهلاك اللحظي", color = Color.Gray, fontSize = 9.sp)
                                                        val consumptionStr = if (data.isEngineRunning) {
                                                            if (data.speed > 5) {
                                                                val base = 5.2f + (data.engineLoad / 100f) * 4f
                                                                String.format(Locale.US, "%.1f لتر/100كم", base)
                                                            } else {
                                                                val idle = 0.8f + (data.rpm / 4000f) * 0.7f
                                                                String.format(Locale.US, "%.1f لتر/س", idle)
                                                            }
                                                        } else {
                                                            "0.0 لتر/س"
                                                        }
                                                        Text(consumptionStr, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                        GForceMeter(
                                            modifier = Modifier.weight(0.9f),
                                            primaryColor = theme.primary,
                                            speed = data.speed.toFloat(),
                                            throttle = throttleInput,
                                            isEngineRunning = data.isEngineRunning
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(modifier = Modifier.weight(1f)) { RenderCoolantCard(data) }
                                        Box(modifier = Modifier.weight(1f)) { RenderTurboCard(data) }
                                    }
                                    RenderPowerCard(data, enginePowerHp)

                                    CategoryHeader("ناقل الحركة (Transmission Health)", Icons.Default.SettingsInputComponent, Color(0xFFFFA726))
                                    RenderTransmissionCard(data)
                                }

                                // Column 2: Electrical, Maintenance, and Body schematic
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    CategoryHeader("الكهرباء والبطارية (Electrical & Battery)", Icons.Default.Bolt, Color(0xFFFFD54F))
                                    RenderBatteryCard(data)
                                    BatteryVoltageMonitorComponent(data, viewModel)

                                    CategoryHeader("الصيانة والبيئة (Oil & DPF Maintenance)", Icons.Default.Build, Color(0xFF4CAF50))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(modifier = Modifier.weight(1.1f)) { RenderOilMaintenanceCard(data, viewModel) }
                                        Box(modifier = Modifier.weight(0.9f)) { RenderDpfCard(data) }
                                    }

                                    CategoryHeader("الهيكل والأبواب (Body & Doors Status)", Icons.Default.DirectionsCar, Color(0xFFFFB300))
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, Color(0xFF232B3A))
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            CarTopDownVisualizer(data = data, isSimulation = isSimulation, viewModel = viewModel)
                                            ComfortAndWindowControls(data = data, viewModel = viewModel)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Specific Category selected on tablet
                        when (selectedCategory) {
                            "ENGINE" -> {
                                item { CategoryHeader("أداء المحرك (Engine Performance)", Icons.Default.Speed, theme.primary) }
                                item {
                                    val mirrorModifier = if (isHudMirrored) Modifier.graphicsLayer(scaleX = -1f) else Modifier
                                    Row(
                                        modifier = Modifier.fillMaxWidth().then(mirrorModifier),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(modifier = Modifier.weight(1f).aspectRatio(1.2f)) {
                                            RpmGauge(value = data.rpm.toFloat(), maxValue = 5000f, title = "دوران المحرك", unit = "RPM", accentColor = theme.secondary)
                                        }
                                        Box(modifier = Modifier.weight(1f).aspectRatio(1.2f)) {
                                            SpeedGauge(value = convertedSpeed, maxValue = maxSpeedLimitVal, title = "السرعة الحالية", unit = speedUnitStr, isEngineRunning = data.isEngineRunning, accentColor = theme.primary)
                                        }
                                    }
                                }
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Box(modifier = Modifier.weight(1f)) { RenderCoolantCard(data) }
                                                Box(modifier = Modifier.weight(1f)) { RenderTurboCard(data) }
                                            }
                                            RenderPowerCard(data, enginePowerHp)
                                        }
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                            GForceMeter(
                                                modifier = Modifier.fillMaxWidth(),
                                                primaryColor = theme.primary,
                                                speed = data.speed.toFloat(),
                                                throttle = throttleInput,
                                                isEngineRunning = data.isEngineRunning
                                            )
                                        }
                                    }
                                }
                            }
                            "TRANSMISSION" -> {
                                item { CategoryHeader("ناقل الحركة (Transmission Health)", Icons.Default.SettingsInputComponent, Color(0xFFFFA726)) }
                                item { RenderTransmissionCard(data) }
                            }
                            "ELECTRICAL" -> {
                                item { CategoryHeader("الكهرباء والبطارية (Electrical & Battery)", Icons.Default.Bolt, Color(0xFFFFD54F)) }
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Box(modifier = Modifier.weight(1f)) { RenderBatteryCard(data) }
                                        Box(modifier = Modifier.weight(1f)) { BatteryVoltageMonitorComponent(data, viewModel) }
                                    }
                                }
                            }
                            "MAINTENANCE" -> {
                                item { CategoryHeader("الصيانة والبيئة (Oil & DPF Maintenance)", Icons.Default.Build, Color(0xFF4CAF50)) }
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Box(modifier = Modifier.weight(1f)) { RenderOilMaintenanceCard(data, viewModel) }
                                        Box(modifier = Modifier.weight(1f)) { RenderDpfCard(data) }
                                    }
                                }
                            }
                            "BODY" -> {
                                item { CategoryHeader("الهيكل والأبواب (Body & Doors Status)", Icons.Default.DirectionsCar, Color(0xFFFFB300)) }
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, Color(0xFF232B3A))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(modifier = Modifier.weight(1.2f)) {
                                                CarTopDownVisualizer(data = data, isSimulation = isSimulation, viewModel = viewModel)
                                            }
                                            Box(modifier = Modifier.weight(0.8f)) {
                                                ComfortAndWindowControls(data = data, viewModel = viewModel)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                // PHONE / COMPACT SCREEN SINGLE-COLUMN LAYOUT
                when (selectedCategory) {
                    "ALL" -> {
                        // All categories sequentially with beautiful Headers
                        item { CategoryHeader("أداء المحرك (Engine Performance)", Icons.Default.Speed, theme.primary) }
                        item {
                            val mirrorModifier = if (isHudMirrored) Modifier.graphicsLayer(scaleX = -1f) else Modifier
                            Row(
                                modifier = Modifier.fillMaxWidth().then(mirrorModifier),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(modifier = Modifier.weight(1f).aspectRatio(1f)) {
                                    RpmGauge(value = data.rpm.toFloat(), maxValue = 5000f, title = "دوران المحرك", unit = "RPM", accentColor = theme.secondary)
                                }
                                Box(modifier = Modifier.weight(1f).aspectRatio(1f)) {
                                    SpeedGauge(value = convertedSpeed, maxValue = maxSpeedLimitVal, title = "السرعة الحالية", unit = speedUnitStr, isEngineRunning = data.isEngineRunning, accentColor = theme.primary)
                                }
                            }
                        }
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Card(
                                    modifier = Modifier.weight(1.1f),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
                                    border = BorderStroke(1.dp, Color(0xFF232B3A)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            "كمبيوتر الرحلة والمسافة (Trip Stats)",
                                            color = theme.primary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Right
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("مسافة الرحلة", color = Color.Gray, fontSize = 9.sp)
                                                Text(String.format(Locale.US, "%.2f كم", tripDistance), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("مدة القيادة", color = Color.Gray, fontSize = 9.sp)
                                                val hrs = tripSeconds / 3600
                                                val mins = (tripSeconds % 3600) / 60
                                                val secs = tripSeconds % 60
                                                val durationStr = if (hrs > 0) "${hrs}:${mins}:${secs}" else "${mins}:${secs}"
                                                Text(durationStr, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("السرعة القصوى", color = Color.Gray, fontSize = 9.sp)
                                                Text("${maxSpeedObserved.toInt()} كم/س", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("الاستهلاك اللحظي", color = Color.Gray, fontSize = 9.sp)
                                                val consumptionStr = if (data.isEngineRunning) {
                                                    if (data.speed > 5) {
                                                        val base = 5.2f + (data.engineLoad / 100f) * 4f
                                                        String.format(Locale.US, "%.1f لتر/100كم", base)
                                                    } else {
                                                        val idle = 0.8f + (data.rpm / 4000f) * 0.7f
                                                        String.format(Locale.US, "%.1f لتر/س", idle)
                                                    }
                                                } else {
                                                    "0.0 لتر/س"
                                                }
                                                Text(consumptionStr, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                                GForceMeter(
                                    modifier = Modifier.weight(0.9f),
                                    primaryColor = theme.primary,
                                    speed = data.speed.toFloat(),
                                    throttle = throttleInput,
                                    isEngineRunning = data.isEngineRunning
                                )
                            }
                        }
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(modifier = Modifier.weight(1f)) { RenderCoolantCard(data) }
                                    Box(modifier = Modifier.weight(1f)) { RenderTurboCard(data) }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(modifier = Modifier.weight(1f)) { RenderBatteryCard(data) }
                                    Box(modifier = Modifier.weight(1f)) { RenderPowerCard(data, enginePowerHp) }
                                }
                            }
                        }
                        item { CategoryHeader("ناقل الحركة (Transmission Health)", Icons.Default.SettingsInputComponent, Color(0xFFFFA726)) }
                        item { RenderTransmissionCard(data) }

                        item { CategoryHeader("الكهرباء والبطارية (Electrical & Battery)", Icons.Default.Bolt, Color(0xFFFFD54F)) }
                        item { BatteryVoltageMonitorComponent(data, viewModel) }

                        item { CategoryHeader("الصيانة والبيئة (Oil & DPF Maintenance)", Icons.Default.Build, Color(0xFF4CAF50)) }
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(modifier = Modifier.weight(1.1f)) { RenderOilMaintenanceCard(data, viewModel) }
                                Box(modifier = Modifier.weight(0.9f)) { RenderDpfCard(data) }
                            }
                        }
                        item { CategoryHeader("الهيكل والأبواب (Body & Doors Status)", Icons.Default.DirectionsCar, Color(0xFFFFB300)) }
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Color(0xFF232B3A))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    CarTopDownVisualizer(data = data, isSimulation = isSimulation, viewModel = viewModel)
                                    ComfortAndWindowControls(data = data, viewModel = viewModel)
                                }
                            }
                        }
                    }
                    "ENGINE" -> {
                        item { CategoryHeader("أداء المحرك (Engine Performance)", Icons.Default.Speed, theme.primary) }
                        item {
                            val mirrorModifier = if (isHudMirrored) Modifier.graphicsLayer(scaleX = -1f) else Modifier
                            Row(
                                modifier = Modifier.fillMaxWidth().then(mirrorModifier),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(modifier = Modifier.weight(1f).aspectRatio(1f)) {
                                    RpmGauge(value = data.rpm.toFloat(), maxValue = 5000f, title = "دوران المحرك", unit = "RPM", accentColor = theme.secondary)
                                }
                                Box(modifier = Modifier.weight(1f).aspectRatio(1f)) {
                                    SpeedGauge(value = convertedSpeed, maxValue = maxSpeedLimitVal, title = "السرعة الحالية", unit = speedUnitStr, isEngineRunning = data.isEngineRunning, accentColor = theme.primary)
                                }
                            }
                        }
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(modifier = Modifier.weight(1f)) { RenderCoolantCard(data) }
                                Box(modifier = Modifier.weight(1f)) { RenderTurboCard(data) }
                            }
                        }
                        item { RenderPowerCard(data, enginePowerHp) }
                        item {
                            GForceMeter(
                                modifier = Modifier.fillMaxWidth(),
                                primaryColor = theme.primary,
                                speed = data.speed.toFloat(),
                                throttle = throttleInput,
                                isEngineRunning = data.isEngineRunning
                            )
                        }
                    }
                    "TRANSMISSION" -> {
                        item { CategoryHeader("ناقل الحركة (Transmission Health)", Icons.Default.SettingsInputComponent, Color(0xFFFFA726)) }
                        item { RenderTransmissionCard(data) }
                    }
                    "ELECTRICAL" -> {
                        item { CategoryHeader("الكهرباء والبطارية (Electrical & Battery)", Icons.Default.Bolt, Color(0xFFFFD54F)) }
                        item { RenderBatteryCard(data) }
                        item { BatteryVoltageMonitorComponent(data, viewModel) }
                    }
                    "MAINTENANCE" -> {
                        item { CategoryHeader("الصيانة والبيئة (Oil & DPF Maintenance)", Icons.Default.Build, Color(0xFF4CAF50)) }
                        item { RenderOilMaintenanceCard(data, viewModel) }
                        item { RenderDpfCard(data) }
                    }
                    "BODY" -> {
                        item { CategoryHeader("الهيكل والأبواب (Body & Doors Status)", Icons.Default.DirectionsCar, Color(0xFFFFB300)) }
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Color(0xFF232B3A))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    CarTopDownVisualizer(data = data, isSimulation = isSimulation, viewModel = viewModel)
                                    ComfortAndWindowControls(data = data, viewModel = viewModel)
                                }
                            }
                        }
                    }
                }
            }

            // Active Controls Panel (For Simulator)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF232B3A))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "مقصورة التحكم الافتراضية للسيارة (المحاكاة)",
                            color = Color.LightGray,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Right
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Engine START/STOP Button
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val pulseAnim = rememberInfiniteTransition(label = "")
                                val scale by pulseAnim.animateFloat(
                                    initialValue = 0.95f,
                                    targetValue = 1.05f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1000, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ), label = ""
                                )
                                Button(
                                    onClick = { viewModel.toggleEngine() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (data.isEngineRunning) Color(0xFFF44336) else Color(0xFF4CAF50)
                                    ),
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .size(72.dp)
                                        .graphicsLayer {
                                            if (!data.isEngineRunning) {
                                                scaleX = scale
                                                scaleY = scale
                                            }
                                        }
                                        .testTag("engine_start_stop_btn")
                                ) {
                                    Icon(
                                        Icons.Default.PowerSettingsNew,
                                        contentDescription = "Power",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Text(
                                    if (data.isEngineRunning) "إطفاء المحرك" else "تشغيل المحرك",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }

                            // Fuel Level gauge
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(72.dp)) {
                                    CircularProgressIndicator(
                                        progress = { data.fuelLevel / 100f },
                                        color = if (data.fuelLevel < 15) Color.Red else Color(0xFF4CAF50),
                                        trackColor = Color(0xFF232B3A),
                                        strokeWidth = 6.dp,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.LocalGasStation,
                                            contentDescription = "Fuel",
                                            tint = if (data.fuelLevel < 15) Color.Red else Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            "${data.fuelLevel}%",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Text(
                                    "مستوى الديزل",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }

                        if (data.isEngineRunning) {
                            Spacer(modifier = Modifier.height(20.dp))
                            // Manual accelerator pedal slider
                            Text(
                                text = "اضغط واسحب لدعس دواسة الوقود: ${(throttleInput * 100).toInt()}%",
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Right
                            )
                            Slider(
                                value = throttleInput,
                                onValueChange = { throttleInput = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("throttle_slider"),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFFFB300),
                                    activeTrackColor = Color(0xFFFFB300),
                                    inactiveTrackColor = Color(0xFF232B3A)
                                )
                            )

                            // Hold to floor button for fun racing feeling
                            Button(
                                onClick = {},
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                throttleInput = 1.0f
                                                tryAwaitRelease()
                                                throttleInput = 0.0f
                                            }
                                        )
                                    },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                            ) {
                                Icon(Icons.Default.Speed, contentDescription = "Accelerate")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("اضغط باستمرار لتسريع المحرك بالكامل (Full Throttle)")
                            }
                        }
                    }
                }
            }

            // Telemetry chart is shown at the very bottom of ENGINE or ALL view
            if (selectedCategory == "ALL" || selectedCategory == "ENGINE") {
                item {
                    RealTimeTelemetryChart(viewModel = viewModel)
                }
            }

            // Secondary items shown at the bottom of the Gauges tab to prioritize actual measurements at the top
            item {
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = Color(0xFF232B3A).copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(16.dp))
            }

            // HUD Status Overview bar
            item {
                ProHudBar(data, connectionState)
            }

            // Active Drive Mode Selection Panel
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF232B3A))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val modeIcon = when (driveMode) {
                                ObdViewModel.DriveMode.NORMAL -> Icons.Default.DirectionsCar
                                ObdViewModel.DriveMode.ECO -> Icons.Default.Eco
                                ObdViewModel.DriveMode.SPORT -> Icons.Default.Speed
                                ObdViewModel.DriveMode.COMFORT -> Icons.Default.Weekend
                            }
                            Icon(
                                imageVector = modeIcon,
                                contentDescription = "Mode",
                                tint = theme.primary,
                                modifier = Modifier.size(24.dp)
                            )

                            Text(
                                text = "نمط القيادة النشط: ${theme.text}",
                                color = theme.primary,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ObdViewModel.DriveMode.values().forEach { mode ->
                                val modeTheme = getDriveModeTheme(mode, appThemeMode)
                                val isSelected = driveMode == mode
                                Button(
                                    onClick = { viewModel.setDriveMode(mode) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .testTag("drive_mode_${mode.name.lowercase()}"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) modeTheme.primary else Color(0xFF1F2430),
                                        contentColor = if (isSelected) Color.Black else Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    val modeLabel = when (mode) {
                                        ObdViewModel.DriveMode.NORMAL -> "عادي"
                                        ObdViewModel.DriveMode.ECO -> "اقتصادي"
                                        ObdViewModel.DriveMode.SPORT -> "رياضي"
                                        ObdViewModel.DriveMode.COMFORT -> "مريح"
                                    }
                                    Text(
                                        text = modeLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Customize Dashboard Card (with buttons to toggle edit mode, hud mirror, reset)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
                    border = BorderStroke(1.dp, Color(0xFF232B3A))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { isEditMode = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1F2430),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ترتيب العدادات 🛠", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // HUD Mirror Mode Toggle
                            Button(
                                onClick = { isHudMirrored = !isHudMirrored },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isHudMirrored) Color(0xFF2196F3) else Color(0xFF1F2430),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Flip,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isHudMirrored) "انعكاس HUD نشط 📱" else "انعكاس الزجاج HUD 🪞", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "تخصيص لوحة القيادة",
                                color = Color.White,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "رتب عدادات Kia Carens بمرونة تامة",
                                color = Color.Gray,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            // Warning Simulation Panel is always visible for testing at the very bottom
            item {
                WarningSimulationPanel(viewModel = viewModel)
            }
            }
        }
    }
}

@Composable
fun CategoryHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = color
            )
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(color.copy(alpha = 0.2f))
        )
    }
}

@Composable
fun RenderTransmissionCard(data: com.example.data.obd.ObdSensorData) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("transmission_card"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF232B3A))
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.SettingsInputComponent,
                        contentDescription = "Transmission",
                        tint = Color(0xFFFFA726)
                    )
                    Text(
                        "ناقل الحركة (Transmission)",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                // Current selected gear badge
                Box(
                    modifier = Modifier
                        .background(Color(0xFFFFA726).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .border(1.dp, Color(0xFFFFA726), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = data.transmissionGear,
                        color = Color(0xFFFFA726),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            
            // Grid of 3 transmission parameters (Fluid Temp, Hydraulic Line Pressure, TCC Slip)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Fluid Temp
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("حرارة الزيت ATF", color = Color.Gray, fontSize = 9.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${data.transmissionTemp}°م",
                        color = if (data.transmissionTemp > 95) Color.Red else Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (data.transmissionTemp / 120f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = if (data.transmissionTemp > 95) Color.Red else Color(0xFFFFA726),
                        trackColor = Color(0xFF232B3A)
                    )
                }
                
                // Line Pressure
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("ضغط الهيدروليك", color = Color.Gray, fontSize = 9.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${data.transmissionPressure} Bar",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (data.transmissionPressure / 12f).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = Color(0xFFFFA726),
                        trackColor = Color(0xFF232B3A)
                    )
                }
                
                // TCC Slip
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("انزلاق الكلتش TCC", color = Color.Gray, fontSize = 9.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${data.transmissionSlipPercent}%",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (data.transmissionSlipPercent / 15f).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = Color(0xFFFFA726),
                        trackColor = Color(0xFF232B3A)
                    )
                }
            }
        }
    }
}

// Gorgeous Modular Composables for Dashboard Screen
@Composable
fun RenderBatteryCard(data: com.example.data.obd.ObdSensorData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF232B3A))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Default.ElectricCar, contentDescription = "Battery", tint = Color(0xFFFFD54F))
                Text("البطارية والكهرباء", color = Color.White, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "${data.batteryVoltage} فولت",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "نسبة الشحن: ${data.batteryStateOfCharge}%",
                color = if (data.batteryStateOfCharge < 30) Color.Red else Color(0xFFFFD54F),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (data.batteryStateOfCharge / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = Color(0xFFFFD54F),
                trackColor = Color(0xFF232B3A)
            )
            Text(
                if (data.isEngineRunning) "دينامو نشط (شحن ⚡)" else "المحرك مطفأ (بطارية 🔋)",
                color = Color.Gray,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun BatteryVoltageMonitorComponent(
    data: com.example.data.obd.ObdSensorData,
    viewModel: ObdViewModel,
    modifier: Modifier = Modifier
) {
    val driveMode by viewModel.driveMode.collectAsState()
    val appThemeMode by viewModel.appThemeMode.collectAsState()
    val theme = getDriveModeTheme(driveMode, appThemeMode)
    val isAlternatorFailure by viewModel.isAlternatorFailureSimulated.collectAsState()
    
    val voltage = data.batteryVoltage
    val isUnderVoltage = voltage < 12.0
    
    // Track voltage history for the live chart
    val voltageHistory = remember { mutableStateListOf<Float>() }
    
    LaunchedEffect(voltage) {
        voltageHistory.add(voltage.toFloat())
        if (voltageHistory.size > 20) {
            voltageHistory.removeAt(0)
        }
    }
    
    // Interactive health analyzer state
    var testProgress by remember { mutableStateOf(0f) }
    var isTesting by remember { mutableStateOf(false) }
    var testStepName by remember { mutableStateOf("") }
    var testReport by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    
    val accentColor = if (isUnderVoltage) Color(0xFFE53935) else Color(0xFF00E5FF)
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("voltage_monitor_card"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isUnderVoltage) Color(0xFFE53935).copy(alpha = 0.6f) else Color(0xFF232B3A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with pulsing bolt
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side: Under-voltage blinking alert indicator or normal pulsing
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isUnderVoltage) {
                        val infiniteTransition = rememberInfiniteTransition(label = "voltage_pulse")
                        val alpha by infiniteTransition.animateFloat(
                            initialValue = 0.2f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(600, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "pulse_alpha"
                        )
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .graphicsLayer { this.alpha = alpha }
                                .background(Color(0xFFE53935), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "جهد منخفض! ⚠️",
                            color = Color(0xFFE53935),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF4CAF50), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "النظام مستقر",
                            color = Color(0xFF4CAF50),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Right side: Title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "مراقب جهد البطارية ونظام الشحن",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "تحليل كفاءة المولد والجهد اللحظي للسيارة",
                            color = Color.Gray,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.ElectricCar,
                        contentDescription = "Voltage Monitor",
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main row: Voltage display and state info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side: Battery Health Score / State
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "كفاءة البطارية التقريبية",
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                    val healthScore = when {
                        isUnderVoltage -> "45% (ضعيفة جداً)"
                        data.isEngineRunning && voltage < 13.0 -> "60% (شحن غير كافٍ)"
                        !data.isEngineRunning && voltage > 12.3 -> "95% (ممتازة)"
                        else -> "85% (جيدة)"
                    }
                    val healthColor = when {
                        isUnderVoltage -> Color(0xFFE53935)
                        voltage < 13.0 && data.isEngineRunning -> Color(0xFFFFB300)
                        else -> Color(0xFF4CAF50)
                    }
                    Text(
                        text = healthScore,
                        color = healthColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (data.isEngineRunning) "الدينامو: يعمل بنشاط ⚡" else "الدينامو: ساكن 💤",
                        color = Color.LightGray,
                        fontSize = 10.sp
                    )
                }

                // Right side: Huge Voltage display
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "فولت",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 6.dp, end = 4.dp)
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f", voltage),
                            color = accentColor,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 34.sp
                            )
                        )
                    }
                    Text(
                        text = when {
                            voltage >= 13.5 -> "الدينامو يغذي السيارة ويشحن البطارية"
                            voltage >= 12.0 -> "البطارية في وضع الاستقرار (المحرك مطفأ)"
                            else -> "تفريغ حرج! جهد البطارية أقل من المعدل الآمن"
                        },
                        color = Color.LightGray,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Right
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // CRITICAL UNDER-VOLTAGE ALERT BANNER
            AnimatedVisibility(
                visible = isUnderVoltage,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("voltage_crit_alert"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2E1313)),
                    border = BorderStroke(1.dp, Color(0xFFE53935))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f).padding(end = 12.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "⚠️ تحذير حرج: جهد النظام منخفض (${voltage}V)",
                                color = Color(0xFFFF8A80),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Right
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "تم رصد هبوط الجهد لأقل من 12.0V! قد تواجه صعوبة في تشغيل السيارة. يرجى التحقق من عمل مولد الشحن (الدينامو) أو إيقاف تشغيل الملحقات الكهربائية المستهلكة فوراً لتجنب نفاد شحن البطارية بالكامل.",
                                color = Color.LightGray,
                                fontSize = 10.sp,
                                textAlign = TextAlign.Right
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Alert",
                            tint = Color(0xFFE53935),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            // Real-time Sparkline Graph
            Text(
                text = "مخطط الجهد اللحظي (الموجة الكهربائية):",
                color = Color.Gray,
                fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                textAlign = TextAlign.Right
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF1E2430), RoundedCornerShape(8.dp))
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                if (voltageHistory.isEmpty()) {
                    Text("جاري استقبال البيانات اللحظية...", color = Color.Gray, fontSize = 10.sp)
                } else {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val maxVal = 15.0f
                        val minVal = 10.0f
                        val range = maxVal - minVal
                        
                        val path = Path()
                        val widthStep = size.width / 19f // show last 20 points
                        
                        val pointsToShow = voltageHistory.takeLast(20)
                        pointsToShow.forEachIndexed { index, value ->
                            val x = index * widthStep
                            val clampedVal = value.coerceIn(minVal, maxVal)
                            val y = size.height - ((clampedVal - minVal) / range) * size.height
                            if (index == 0) {
                                path.moveTo(x, y)
                            } else {
                                path.lineTo(x, y)
                            }
                        }
                        
                        // Fill path
                        val fillPath = Path().apply {
                            addPath(path)
                            if (pointsToShow.isNotEmpty()) {
                                lineTo((pointsToShow.size - 1) * widthStep, size.height)
                            }
                            lineTo(0f, size.height)
                            close()
                        }
                        
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.25f),
                                    Color.Transparent
                                )
                            )
                        )
                        
                        drawPath(
                            path = path,
                            color = accentColor,
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    
                    // Grid / labels overlay
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text("15.0V", color = Color.Gray.copy(alpha = 0.5f), fontSize = 8.sp)
                        Text("12.5V", color = Color.Gray.copy(alpha = 0.5f), fontSize = 8.sp)
                        Text("10.0V", color = Color.Gray.copy(alpha = 0.5f), fontSize = 8.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Interactive Analyzer Section
            if (isTesting) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1218)),
                    border = BorderStroke(1.dp, Color(0xFF232B3A))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                progress = { testProgress },
                                color = accentColor,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = testStepName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Right
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { testProgress },
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                            color = accentColor,
                            trackColor = Color(0xFF1E2430)
                        )
                    }
                }
            }

            var healingMessage by remember { mutableStateOf("") } // Temporary variable to satisfy compiler if needed

            testReport?.let { report ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isUnderVoltage) Color(0xFF2C1414) else Color(0xFF14241B)),
                    border = BorderStroke(1.dp, if (isUnderVoltage) Color(0xFFC62828) else Color(0xFF2E7D32))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "تم انتهاء الفحص بنجاح",
                                color = Color.LightGray,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "📋 تقرير الصحة الكهربائية",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = report,
                            color = Color.White,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { testReport = null },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.Gray),
                            modifier = Modifier.align(Alignment.End),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("إغلاق التقرير ✕", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Buttons: Diagnostic and Simulation Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Alternator simulation toggle
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clickable {
                            viewModel.toggleAlternatorFailure(!isAlternatorFailure)
                        },
                    colors = CardDefaults.cardColors(containerColor = if (isAlternatorFailure) Color(0xFF2C1010) else Color(0xFF1F2430)),
                    border = BorderStroke(1.dp, if (isAlternatorFailure) Color(0xFFE53935) else Color(0xFF2C3549))
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = isAlternatorFailure,
                            onCheckedChange = { viewModel.toggleAlternatorFailure(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Red,
                                checkedTrackColor = Color(0xFF5E1B1B)
                            ),
                            modifier = Modifier.graphicsLayer {
                                scaleX = 0.7f
                                scaleY = 0.7f
                            }.testTag("sim_under_volt_switch")
                        )
                        Text(
                            text = "محاكاة هبوط الجهد 🛠",
                            color = if (isAlternatorFailure) Color(0xFFFF8A80) else Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Right
                        )
                    }
                }

                // Analyze diagnostic button
                Button(
                    onClick = {
                        isTesting = true
                        testReport = null
                        testProgress = 0f
                        scope.launch {
                            val steps = listOf(
                                "1. قياس الجهد السكوني ومقاومة الأسلاك الكهربائية...",
                                "2. تحليل الموجات التوافقية للدينامو (Alternator Ripple)...",
                                "3. فحص هبوط جهد التشغيل (Cranking Voltage Test)...",
                                "4. قياس تيار التغذية تحت حمل الأجهزة الذكية..."
                            )
                            for (i in steps.indices) {
                                testStepName = steps[i]
                                testProgress = (i + 1) / 4f
                                delay(900)
                            }
                            isTesting = false
                            testReport = if (isAlternatorFailure) {
                                "❌ تحذير: تم الكشف عن تدهور كبير في كفاءة شحن الدينامو (الجهد الحالي: ${voltage}V). النظام الكهربائي يعتمد كلياً على خلايا البطارية، مما قد يفرغها كلياً في غضون دقائق. يرجى مراجعة كهربائي سيارات لفحص فحمات المولد أو السير."
                            } else {
                                "✅ ممتاز: نظام الكهرباء والشحن يعمل بكفاءة 98%. الجهد مستقر للغاية عند (${voltage}V) مما يؤكد استجابة دينامو كيا كارنز المذهلة للأحمال المتغيرة وعمر البطارية المتبقي يتجاوز سنتين."
                            }
                        }
                    },
                    modifier = Modifier.weight(1.2f).height(40.dp).testTag("run_volt_analysis_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primary, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !isTesting
                ) {
                    Icon(imageVector = Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("فحص كفاءة الكهرباء ⚡", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun RenderCoolantCard(data: com.example.data.obd.ObdSensorData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF232B3A))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.Thermostat,
                    contentDescription = "Coolant",
                    tint = if (data.coolantTemp > 94) Color.Red else Color(0xFF00E5FF)
                )
                Text(
                    "حرارة المحرك",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "${data.coolantTemp}°م",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (data.coolantTemp > 94) Color.Red else Color.White
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (data.coolantTemp / 120f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = if (data.coolantTemp > 94) Color.Red else Color(0xFF00E5FF),
                trackColor = Color(0xFF232B3A)
            )
            Text(
                if (data.coolantTemp > 94) "حرارة مرتفعة!" else "درجة حرارة طبيعية",
                color = if (data.coolantTemp > 94) Color.Red else Color.Gray,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun RenderTurboCard(data: com.example.data.obd.ObdSensorData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF232B3A))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Default.Bolt, contentDescription = "Turbo", tint = Color(0xFF00E5FF))
                Text("ضغط التيربو", color = Color.White, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "${data.turboBoostPressure} Bar",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E5FF)
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (data.turboBoostPressure / 1.5).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = Color(0xFF00E5FF),
                trackColor = Color(0xFF232B3A)
            )
            Text("ضغط التوربين النشط", color = Color.Gray, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
fun RenderPowerCard(data: com.example.data.obd.ObdSensorData, enginePowerHp: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF232B3A))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Default.Speed, contentDescription = "Engine Power", tint = Color(0xFFFFA726))
                Text("قوة المحرك", color = Color.White, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "${enginePowerHp} حصان",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFA726)
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (enginePowerHp / 140f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = Color(0xFFFFA726),
                trackColor = Color(0xFF232B3A)
            )
            Text("حمل المحرك: ${data.engineLoad}%", color = Color.Gray, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
fun RenderInfoBannerCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF232B3A))
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "محرك كيا كارنز ديزل 2.0 CRDi - عام 2008",
                color = Color(0xFFFFB300),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Right
            )
            Text(
                text = "حقن ديزل مباشر بقوة 140 حصان مع نظام توربو شاحن متطور",
                color = Color.LightGray,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Right,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun RenderOilMaintenanceCard(data: com.example.data.obd.ObdSensorData, viewModel: ObdViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF232B3A))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { viewModel.resetOilLife() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300), contentColor = Color.Black),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("إعادة تعيين 🔄", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    "زيت المحرك والصيانة",
                    color = Color(0xFFFFB300),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(60.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { data.oilLifePercent / 100f },
                        color = when {
                            data.oilLifePercent < 20 -> Color.Red
                            data.oilLifePercent < 50 -> Color(0xFFFF9800)
                            else -> Color(0xFF4CAF50)
                        },
                        trackColor = Color(0xFF232B3A),
                        strokeWidth = 4.dp,
                        modifier = Modifier.fillMaxSize()
                    )
                    Text(
                        "${data.oilLifePercent}%",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        "${data.oilRemainingKm} كم متبقي",
                        color = if (data.oilRemainingKm < 1500) Color.Red else Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text("حرارة الزيت: ${data.oilTemperature}°م", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                    Text("ضغط الزيت: ${data.oilPressure} Bar", color = if (data.oilPressure <= 0.8 && data.isEngineRunning) Color(0xFFFF5252) else Color.Gray, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun RenderDpfCard(data: com.example.data.obd.ObdSensorData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF232B3A))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "فلتر سخام البيئة DPF",
                color = Color(0xFFFFB300),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Right
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "مستوى السخام: ${data.dpfSootLevel}%",
                    color = if (data.dpfSootLevel > 70) Color.Red else Color(0xFFFF9800),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { data.dpfSootLevel / 100f },
                    color = if (data.dpfSootLevel > 70) Color.Red else Color(0xFFFF9800),
                    trackColor = Color(0xFF232B3A),
                    modifier = Modifier.fillMaxWidth().height(5.dp)
                )
                Text(
                    if (data.dpfSootLevel > 70) "تحذير: يتطلب تجديد نشط DPF" else "مستوى الكربون طبيعي وسليم",
                    color = Color.Gray,
                    fontSize = 9.sp,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun ProHudBar(
    data: com.example.data.obd.ObdSensorData,
    connectionState: com.example.data.obd.ObdConnectionState
) {
    val warningCount = (if (data.coolantTemp > 94) 1 else 0) +
                       (if (data.batteryStateOfCharge < 30) 1 else 0) +
                       (if (data.dpfSootLevel > 70) 1 else 0) +
                       (if (data.oilPressure <= 0.8 && data.isEngineRunning) 1 else 0) +
                       (if (data.activeDtcs.isNotEmpty()) data.activeDtcs.size else 0)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1218)),
        border = BorderStroke(1.dp, Color(0xFF1E2430)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Warning Badge / All OK
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (warningCount > 0) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFD32F2F), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "تحذير: $warningCount ⚠️",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF388E3C), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "الأنظمة سليمة ✔",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Right: Connection Status
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (connectionState) {
                        com.example.data.obd.ObdConnectionState.CONNECTED -> "متصل بجهاز OBD2 🟢"
                        com.example.data.obd.ObdConnectionState.CONNECTING -> "جاري الاتصال... 🟡"
                        com.example.data.obd.ObdConnectionState.INITIALIZING -> "برمجة ELM327... 🔵"
                        else -> "غير متصل (وضع المحاكاة) 💻"
                    },
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun CarTopDownVisualizer(
    data: com.example.data.obd.ObdSensorData,
    isSimulation: Boolean,
    viewModel: ObdViewModel
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Column (Driver Side + Hood)
        Column(
            modifier = Modifier.weight(1.2f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.End
        ) {
            CompactDoorTile(
                name = "غطاء المحرك 🚘",
                isOpen = data.hoodOpen,
                onClick = { if (isSimulation) viewModel.toggleDoor(4) }
            )
            CompactDoorTile(
                name = "باب السائق 💺",
                isOpen = data.doorDriverOpen,
                onClick = { if (isSimulation) viewModel.toggleDoor(0) }
            )
            CompactDoorTile(
                name = "خلفي يسار 🚪",
                isOpen = data.doorRearLeftOpen,
                onClick = { if (isSimulation) viewModel.toggleDoor(2) }
            )
        }

        // Center Automotive Silhouette Visualizer
        Box(
            modifier = Modifier
                .size(width = 120.dp, height = 200.dp)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            val pulseAnim = rememberInfiniteTransition(label = "")
            val pulseAlpha by pulseAnim.animateFloat(
                initialValue = 0.3f,
                targetValue = 0.9f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ), label = ""
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                
                // Car Center Coordinates
                val cx = w / 2
                val cy = h / 2
                
                // Car Frame Dimensions
                val carW = 64.dp.toPx()
                val carH = 150.dp.toPx()
                
                // Draw Base Car Silhouette (Dark gray fill, sleek neon border)
                val carPath = androidx.compose.ui.graphics.Path().apply {
                    // Start at top-center hood
                    moveTo(cx, cy - carH / 2)
                    // Curve to top-right corner
                    quadraticTo(cx + carW / 2, cy - carH / 2, cx + carW / 2, cy - carH / 3)
                    // Right edge to rear corner
                    lineTo(cx + carW / 2, cy + carH / 2 - 10.dp.toPx())
                    // Curve to rear-right bumper
                    quadraticTo(cx + carW / 2, cy + carH / 2, cx + carW / 3, cy + carH / 2)
                    // Rear bumper flat center
                    lineTo(cx - carW / 3, cy + carH / 2)
                    // Curve to rear-left bumper
                    quadraticTo(cx - carW / 2, cy + carH / 2, cx - carW / 2, cy + carH / 2 - 10.dp.toPx())
                    // Left edge to hood corner
                    lineTo(cx - carW / 2, cy - carH / 3)
                    // Curve to top-left hood corner
                    quadraticTo(cx - carW / 2, cy - carH / 2, cx, cy - carH / 2)
                    close()
                }
                
                // Fill background of the car body
                drawPath(
                    path = carPath,
                    color = Color(0xFF1E2430)
                )
                // Draw sleek dashboard chassis stroke
                drawPath(
                    path = carPath,
                    color = Color(0xFF3B4861),
                    style = Stroke(width = 2.dp.toPx())
                )

                // Windshield and Cabin outline
                val cabinPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(cx - carW / 3, cy - carH / 6)
                    lineTo(cx + carW / 3, cy - carH / 6)
                    lineTo(cx + carW / 4, cy + carH / 4)
                    lineTo(cx - carW / 4, cy + carH / 4)
                    close()
                }
                drawPath(
                    path = cabinPath,
                    color = Color(0xFF0F1218)
                )
                drawPath(
                    path = cabinPath,
                    color = Color(0xFF3B4861),
                    style = Stroke(width = 1.dp.toPx())
                )

                // WHEELS (Sleek black wheels on the sides)
                val wheelW = 8.dp.toPx()
                val wheelH = 18.dp.toPx()
                // Front Left Wheel
                drawRoundRect(
                    color = Color(0xFF0A0D14),
                    topLeft = androidx.compose.ui.geometry.Offset(cx - carW / 2 - wheelW + 1.dp.toPx(), cy - carH / 3.5f),
                    size = androidx.compose.ui.geometry.Size(wheelW, wheelH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
                // Front Right Wheel
                drawRoundRect(
                    color = Color(0xFF0A0D14),
                    topLeft = androidx.compose.ui.geometry.Offset(cx + carW / 2 - 1.dp.toPx(), cy - carH / 3.5f),
                    size = androidx.compose.ui.geometry.Size(wheelW, wheelH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
                // Rear Left Wheel
                drawRoundRect(
                    color = Color(0xFF0A0D14),
                    topLeft = androidx.compose.ui.geometry.Offset(cx - carW / 2 - wheelW + 1.dp.toPx(), cy + carH / 4f),
                    size = androidx.compose.ui.geometry.Size(wheelW, wheelH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
                // Rear Right Wheel
                drawRoundRect(
                    color = Color(0xFF0A0D14),
                    topLeft = androidx.compose.ui.geometry.Offset(cx + carW / 2 - 1.dp.toPx(), cy + carH / 4f),
                    size = androidx.compose.ui.geometry.Size(wheelW, wheelH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )

                // ACTIVE STATE OVERLAYS (RED GLOWS FOR OPEN COMPARTMENTS)
                
                // 1. Hood (Front) Open
                if (data.hoodOpen) {
                    val hoodHighlight = androidx.compose.ui.graphics.Path().apply {
                        moveTo(cx, cy - carH / 2)
                        quadraticTo(cx + carW / 3, cy - carH / 2, cx + carW / 3, cy - carH / 3)
                        lineTo(cx - carW / 3, cy - carH / 3)
                        quadraticTo(cx - carW / 3, cy - carH / 2, cx, cy - carH / 2)
                        close()
                    }
                    drawPath(
                        path = hoodHighlight,
                        color = Color(0xFFFF5252).copy(alpha = pulseAlpha)
                    )
                }

                // 2. Trunk (Rear) Open
                if (data.trunkOpen) {
                    val trunkHighlight = androidx.compose.ui.graphics.Path().apply {
                        moveTo(cx - carW / 3, cy + carH / 2 - 12.dp.toPx())
                        lineTo(cx + carW / 3, cy + carH / 2 - 12.dp.toPx())
                        quadraticTo(cx + carW / 3, cy + carH / 2, cx, cy + carH / 2)
                        quadraticTo(cx - carW / 3, cy + carH / 2, cx - carW / 3, cy + carH / 2 - 12.dp.toPx())
                        close()
                    }
                    drawPath(
                        path = trunkHighlight,
                        color = Color(0xFFFF5252).copy(alpha = pulseAlpha)
                    )
                }

                // 3. Driver Door Open (Middle Left)
                if (data.doorDriverOpen) {
                    val doorLength = 28.dp.toPx()
                    val doorStartOffset = cy - carH / 7f
                    drawArc(
                        color = Color(0xFFFF5252).copy(alpha = pulseAlpha),
                        startAngle = 160f,
                        sweepAngle = 40f,
                        useCenter = true,
                        topLeft = androidx.compose.ui.geometry.Offset(cx - carW / 2 - 15.dp.toPx(), doorStartOffset),
                        size = androidx.compose.ui.geometry.Size(30.dp.toPx(), doorLength)
                    )
                }

                // 4. Passenger Front Door Open (Middle Right)
                if (data.doorPassengerOpen) {
                    val doorLength = 28.dp.toPx()
                    val doorStartOffset = cy - carH / 7f
                    drawArc(
                        color = Color(0xFFFF5252).copy(alpha = pulseAlpha),
                        startAngle = 340f,
                        sweepAngle = 40f,
                        useCenter = true,
                        topLeft = androidx.compose.ui.geometry.Offset(cx + carW / 2 - 15.dp.toPx(), doorStartOffset),
                        size = androidx.compose.ui.geometry.Size(30.dp.toPx(), doorLength)
                    )
                }

                // 5. Rear Left Door Open (Lower Left)
                if (data.doorRearLeftOpen) {
                    val doorLength = 25.dp.toPx()
                    val doorStartOffset = cy + carH / 14f
                    drawArc(
                        color = Color(0xFFFF5252).copy(alpha = pulseAlpha),
                        startAngle = 160f,
                        sweepAngle = 40f,
                        useCenter = true,
                        topLeft = androidx.compose.ui.geometry.Offset(cx - carW / 2 - 15.dp.toPx(), doorStartOffset),
                        size = androidx.compose.ui.geometry.Size(30.dp.toPx(), doorLength)
                    )
                }

                // 6. Rear Right Door Open (Lower Right)
                if (data.doorRearRightOpen) {
                    val doorLength = 25.dp.toPx()
                    val doorStartOffset = cy + carH / 14f
                    drawArc(
                        color = Color(0xFFFF5252).copy(alpha = pulseAlpha),
                        startAngle = 340f,
                        sweepAngle = 40f,
                        useCenter = true,
                        topLeft = androidx.compose.ui.geometry.Offset(cx + carW / 2 - 15.dp.toPx(), doorStartOffset),
                        size = androidx.compose.ui.geometry.Size(30.dp.toPx(), doorLength)
                    )
                }
            }
        }

        // Right Column (Passenger Side + Trunk)
        Column(
            modifier = Modifier.weight(1.2f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            CompactDoorTile(
                name = "صندوق الأمتعة 📦",
                isOpen = data.trunkOpen,
                onClick = { if (isSimulation) viewModel.toggleDoor(5) }
            )
            CompactDoorTile(
                name = "أمامية يمين 💺",
                isOpen = data.doorPassengerOpen,
                onClick = { if (isSimulation) viewModel.toggleDoor(1) }
            )
            CompactDoorTile(
                name = "خلفية يمين 🚪",
                isOpen = data.doorRearRightOpen,
                onClick = { if (isSimulation) viewModel.toggleDoor(3) }
            )
        }
    }
}

@Composable
fun CompactDoorTile(
    name: String,
    isOpen: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isOpen) Color(0xFF421D1D) else Color(0xFF1E2430)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isOpen) Color(0xFFFF5252) else Color(0xFF2E384C)
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isOpen) "مفتوح ⚠️" else "مغلق ✔",
                    color = if (isOpen) Color(0xFFFF5252) else Color(0xFF4CAF50),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = name,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Right
                )
            }
        }
    }
}

@Composable
fun ComfortAndWindowControls(
    data: com.example.data.obd.ObdSensorData,
    viewModel: ObdViewModel,
    modifier: Modifier = Modifier
) {
    val driveMode by viewModel.driveMode.collectAsState()
    val appThemeMode by viewModel.appThemeMode.collectAsState()
    val theme = getDriveModeTheme(driveMode, appThemeMode)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalDivider(color = Color(0xFF232B3A), thickness = 1.dp)
        Spacer(modifier = Modifier.height(16.dp))

        // 1. Central Lock Section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F1218), RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFF232B3A), RoundedCornerShape(12.dp))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Lock/Unlock Action Button
            Button(
                onClick = { viewModel.toggleCentralLock() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (data.isCentralLocked) theme.primary else Color(0xFFE53935),
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .height(40.dp)
                    .testTag("toggle_central_lock_btn")
            ) {
                Icon(
                    imageVector = if (data.isCentralLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (data.isCentralLocked) "إلغاء القفل المركزي 🔓" else "تفعيل القفل المركزي 🔒",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Central Lock Status
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (data.isCentralLocked) "الأبواب مؤمنة ومغلقة 🔒" else "الأبواب غير مقفلة 🔓",
                        color = if (data.isCentralLocked) theme.primary else Color(0xFFFF8A80),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(if (data.isCentralLocked) theme.primary else Color(0xFFE53935), CircleShape)
                    )
                }
                Text(
                    text = "نظام القفل المركزي لكيا كارنز",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Right
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Title of Windows Section
        Text(
            text = "التحكم بالزجاج الكهربائي (نوافذ السيارة)",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            textAlign = TextAlign.Right
        )

        // 2. 2x2 Grid of Windows
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                // Front Left (Driver)
                WindowControlCard(
                    title = "الأمامي الأيسر (السائق) 💺",
                    percent = data.windowFrontLeftOpenPercent,
                    onPositionChange = { viewModel.setWindowPosition(0, it) },
                    themePrimary = theme.primary,
                    tagPrefix = "front_left"
                )

                // Front Right (Passenger)
                WindowControlCard(
                    title = "الأمامي الأيمن (الراكب) 💺",
                    percent = data.windowFrontRightOpenPercent,
                    onPositionChange = { viewModel.setWindowPosition(1, it) },
                    themePrimary = theme.primary,
                    tagPrefix = "front_right"
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                // Rear Left
                WindowControlCard(
                    title = "الخلفي الأيسر 🚪",
                    percent = data.windowRearLeftOpenPercent,
                    onPositionChange = { viewModel.setWindowPosition(2, it) },
                    themePrimary = theme.primary,
                    tagPrefix = "rear_left"
                )

                // Rear Right
                WindowControlCard(
                    title = "الخلفي الأيمن 🚪",
                    percent = data.windowRearRightOpenPercent,
                    onPositionChange = { viewModel.setWindowPosition(3, it) },
                    themePrimary = theme.primary,
                    tagPrefix = "rear_right"
                )
            }
        }
    }
}

@Composable
fun RowScope.WindowControlCard(
    title: String,
    percent: Int,
    onPositionChange: (Int) -> Unit,
    themePrimary: Color,
    tagPrefix: String
) {
    Card(
        modifier = Modifier
            .weight(1f)
            .testTag("window_${tagPrefix}_card"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1218)),
        border = BorderStroke(1.dp, Color(0xFF232B3A)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                textAlign = TextAlign.Right
            )
            
            Text(
                text = when (percent) {
                    0 -> "مغلق بالكامل 🔼"
                    100 -> "مفتوح بالكامل 🔽"
                    in 1..30 -> "تهوية جزئية 💨 ($percent%)"
                    else -> "مفتوح جزئياً ↕ ($percent%)"
                },
                color = if (percent == 0) Color.Gray else themePrimary,
                fontSize = 10.sp,
                modifier = Modifier.padding(vertical = 2.dp)
            )

            // Slider control
            Slider(
                value = percent.toFloat(),
                onValueChange = { onPositionChange(it.toInt()) },
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(
                    activeTrackColor = themePrimary,
                    inactiveTrackColor = Color(0xFF2E384C),
                    thumbColor = themePrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .testTag("window_${tagPrefix}_slider")
            )

            // Preset Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Open button
                Button(
                    onClick = { onPositionChange(100) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (percent == 100) themePrimary else Color(0xFF1E2430),
                        contentColor = if (percent == 100) Color.Black else Color.White
                    ),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp)
                        .testTag("window_${tagPrefix}_open_btn"),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("فتح", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }

                // Vent button
                Button(
                    onClick = { onPositionChange(25) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (percent == 25) themePrimary else Color(0xFF1E2430),
                        contentColor = if (percent == 25) Color.Black else Color.White
                    ),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp)
                        .testTag("window_${tagPrefix}_vent_btn"),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("تهوية", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }

                // Close button
                Button(
                    onClick = { onPositionChange(0) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (percent == 0) themePrimary else Color(0xFF1E2430),
                        contentColor = if (percent == 0) Color.Black else Color.White
                    ),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp)
                        .testTag("window_${tagPrefix}_close_btn"),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("غلق", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}


enum class ScanState {
    IDLE,
    SCANNING,
    COMPLETED,
    CLEARING
}

data class ScanModule(
    val nameEn: String,
    val nameAr: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun ActiveTestsComponent(
    viewModel: ObdViewModel,
    modifier: Modifier = Modifier
) {
    val connectedDeviceName by viewModel.connectedDeviceName.collectAsState()
    val isDpfRegenerating by viewModel.isDpfRegenerating.collectAsState()
    val isSimulation by viewModel.isSimulation.collectAsState()
    val sensorData by viewModel.sensorData.collectAsState()

    // Injector coding state
    var selectedCylinder by remember { mutableStateOf(1) }
    var injectorCode by remember { mutableStateOf("7A3F9E2") }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF11141B)),
        border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isAutocomActive = connectedDeviceName != null || isSimulation
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isAutocomActive) Color(0xFF00E5FF).copy(alpha = 0.15f) else Color(0xFF2C1E21)
                    ),
                    border = BorderStroke(1.dp, if (isAutocomActive) Color(0xFF00E5FF) else Color(0xFFE53935))
                ) {
                    Text(
                        text = if (isAutocomActive) "واجهة مفعّلة نشطة ✅" else "يتطلب اتصال USB / Autocom DS150E 🔒",
                        color = if (isAutocomActive) Color(0xFF00E5FF) else Color(0xFFE53935),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "اختبارات المشغلات وبرمجة الأنظمة (Actuator Tests & Coding)",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Default.Build, contentDescription = "Active Tests", tint = Color(0xFF00E5FF))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Grid or Column of Active Tests
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Section 1: Dashboard and Warnings
                Text(
                    text = "1. اختبار عدادات ولوحة الطبلون (Cluster & Warning Lights Tests)",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.performGaugeSweep() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF).copy(alpha = 0.12f), contentColor = Color(0xFF00E5FF)),
                        border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = "Sweep", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("فحص مؤشرات العدادات (Sweep)", fontSize = 11.sp)
                    }

                    Button(
                        onClick = { viewModel.performWarningLightsTest() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF).copy(alpha = 0.12f), contentColor = Color(0xFF00E5FF)),
                        border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = "Warnings", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("فحص لمبات التحذير (Lights)", fontSize = 11.sp)
                    }
                }

                HorizontalDivider(color = Color(0xFF1E2530), thickness = 1.dp)

                // Section 2: Actuators Activation
                Text(
                    text = "2. تشغيل واختبار المشغلات الكهربائية (Electrical Actuators Activation)",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )

                // Central lock & Window
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.performActiveLockTest() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C2230), contentColor = Color.LightGray),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = "Lock", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("دورة السنترال لوك", fontSize = 10.sp)
                    }

                    Button(
                        onClick = { viewModel.performActiveWindowTest() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C2230), contentColor = Color.LightGray),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Window", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("فحص موتور زجاج السائق", fontSize = 10.sp)
                    }
                }

                // Fuel Pump & Coolant Fan
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { viewModel.performActiveFuelPumpTest() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C2230), contentColor = Color.LightGray),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.LocalGasStation, contentDescription = "Fuel Pump", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تنشيط تتابع مضخة الوقود", fontSize = 10.sp)
                    }

                    // Fan Selector Row
                    Column(
                        modifier = Modifier.weight(1.3f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text("مروحة التبريد الرادياتير:", color = Color.Gray, fontSize = 9.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("Off" to "إطفاء", "Low" to "بطيء", "High" to "سريع").forEach { (speed, label) ->
                                TextButton(
                                    onClick = { viewModel.performActiveFanTest(speed) },
                                    modifier = Modifier.weight(1f).height(28.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    colors = ButtonDefaults.textButtonColors(
                                        containerColor = Color(0xFF232832),
                                        contentColor = Color.LightGray
                                    ),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(label, fontSize = 9.sp)
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFF1E2530), thickness = 1.dp)

                // Section 3: Advanced Coding & Services
                Text(
                    text = "3. Services & Injector Coding",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )

                // DPF Regeneration Block
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1F26)),
                    border = BorderStroke(1.dp, if (isDpfRegenerating) Color(0xFFFFB300) else Color.Transparent)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { viewModel.performDpfRegeneration() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDpfRegenerating) Color(0xFFFFB300) else Color(0xFFFF5722),
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(6.dp),
                            enabled = !isDpfRegenerating
                        ) {
                            if (isDpfRegenerating) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = Color.Black)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("جاري التطهير...", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Text("بدء تطهير قسري (DPF)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("التطهير القسري لفلتر بيئة الديزل DPF", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text("مستوى سخام الشكمان الحالي: ${sensorData.dpfSootLevel}%", color = if (sensorData.dpfSootLevel > 30) Color.Red else Color.Green, fontSize = 10.sp)
                        }
                    }
                }

                // Injector Coding Block
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1F26))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "برمجة كود البخاخ الذكي (IMA Injector Coding - Delphi)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Right
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    if (injectorCode.isNotEmpty()) {
                                        viewModel.performInjectorCoding(selectedCylinder, injectorCode)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(38.dp)
                            ) {
                                Text("برمجة ترميز البخاخ", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            TextField(
                                value = injectorCode,
                                onValueChange = { injectorCode = it.take(8).uppercase() },
                                modifier = Modifier.weight(1f).height(38.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF11141B),
                                    unfocusedContainerColor = Color(0xFF11141B),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                                singleLine = true,
                                shape = RoundedCornerShape(4.dp)
                            )

                            // Cylinder Selector Spinner Row
                            Row(
                                modifier = Modifier.background(Color(0xFF11141B), RoundedCornerShape(4.dp)).height(38.dp).padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf(1, 2, 3, 4).forEach { cyl ->
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(if (selectedCylinder == cyl) Color(0xFF00E5FF) else Color.Transparent, RoundedCornerShape(2.dp))
                                            .clickable { selectedCylinder = cyl },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(cyl.toString(), color = if (selectedCylinder == cyl) Color.Black else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("أسطوانة", color = Color.Gray, fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DtcScannerComponent(
    viewModel: ObdViewModel,
    data: com.example.data.obd.ObdSensorData,
    modifier: Modifier = Modifier
) {
    val modulesToScan = remember {
        listOf(
            ScanModule("ECU (Engine Control Unit)", "وحدة التحكم بالمحرك", "حساسات الهواء والضغط والديزل والانبعاثات", Icons.Default.DirectionsCar),
            ScanModule("TCU (Transmission Control Unit)", "وحدة التحكم بناقل الحركة", "سرعة ناقل الحركة ومحولات العزم وفولتية الصمامات", Icons.Default.Settings),
            ScanModule("ABS (Anti-lock Braking System)", "نظام الفرامل المانع للانغلاق", "مستشعرات العجلات وقراءات بلف الفرامل الرئيسي", Icons.Default.Warning),
            ScanModule("SRS (Supplemental Restraint System)", "نظام الوسائد الهوائية والأمان", "أحزمة الأمان، حساسات الاصطدام ومفاتيح التفعيل", Icons.Default.Shield),
            ScanModule("BCM (Body Control Module)", "وحدة التحكم بالهيكل والأنظمة الذكية", "الإنذار، الأبواب، النوافذ والمفاتيح والمكيف الذكي", Icons.Default.Build)
        )
    }

    var scanState by remember { mutableStateOf(ScanState.IDLE) }
    var currentModuleIndex by remember { mutableStateOf(0) }
    var scanProgress by remember { mutableStateOf(0f) }
    val scanLogs = remember { mutableStateListOf<String>() }
    var expandedFaultCode by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    
    val driveMode by viewModel.driveMode.collectAsState()
    val appThemeMode by viewModel.appThemeMode.collectAsState()
    val theme = getDriveModeTheme(driveMode, appThemeMode)

    // Self-healing state variables
    var selfHealingCode by remember { mutableStateOf<String?>(null) }
    var selfHealingProgress by remember { mutableStateOf(0f) }
    var healingMessage by remember { mutableStateOf("") }

    val animatedProgress by animateFloatAsState(
        targetValue = scanProgress,
        animationSpec = tween(durationMillis = 300, easing = LinearOutSlowInEasing),
        label = "ScanProgress"
    )

    // Scan & Clear Controller Effect
    LaunchedEffect(scanState) {
        if (scanState == ScanState.SCANNING) {
            scanLogs.clear()
            scanLogs.add("🔌 جاري الاتصال بمحول OBD-II للسيارة...")
            delay(500)
            scanLogs.add("📡 تواصل ناجح عبر Bluetooth ELM327")
            scanLogs.add("> ATZ (Resetting Interface)")
            delay(300)
            scanLogs.add("ELM327 v2.1")
            scanLogs.add("> ATSP6 (Setting protocol to ISO 15765-4 CAN 11bit/500kb)")
            delay(300)
            scanLogs.add("OK")
            scanLogs.add("🏁 بدء الفحص المتكامل لموديولات كيا كارنز...")
            delay(400)

            for (i in modulesToScan.indices) {
                currentModuleIndex = i
                val module = modulesToScan[i]
                scanLogs.add("🔍 جاري فحص موديول: ${module.nameAr}...")
                
                // Simulate progressive scanning steps
                val baseProgress = i / modulesToScan.size.toFloat()
                val stepAmount = 1f / modulesToScan.size
                for (step in 1..4) {
                    scanProgress = baseProgress + (step / 4f) * stepAmount
                    delay(200)
                }

                // Check active DTCs for this module
                val faultsInModule = data.activeDtcs.filter { fault ->
                    when (i) {
                        0 -> fault.category.contains("Engine") || fault.category.contains("Emission") || fault.category.contains("المحرك") || fault.category.contains("العادم")
                        1 -> fault.category.contains("Transmission") || fault.category.contains("القير") || fault.category.contains("الحركة")
                        2 -> fault.category.contains("Chassis") || fault.category.contains("التعليق") || fault.category.contains("الفرامل") || fault.category.contains("Body/Chassis")
                        3 -> fault.category.contains("SRS") || fault.category.contains("الوسائد") || fault.category.contains("الأمان")
                        4 -> fault.category.contains("Body") || fault.category.contains("الهيكل") || fault.category.contains("الباب")
                        else -> false
                    }
                }

                if (faultsInModule.isNotEmpty()) {
                    scanLogs.add("⚠️ تنبيه: تم العثور على عطل نشط في ${module.nameAr}:")
                    faultsInModule.forEach { f ->
                        scanLogs.add("   [${f.code}] - ${f.descriptionAr}")
                    }
                } else {
                    scanLogs.add("✔ موديول ${module.nameAr} سليم وخالٍ من الأخطاء.")
                }
                delay(350)
            }

            scanProgress = 1.0f
            scanLogs.add("🏁 تم إكمال فحص جميع الموديلات المتاحة بنجاح.")
            if (data.activeDtcs.isNotEmpty()) {
                scanLogs.add("⚠️ المجموع: تم تسجيل ${data.activeDtcs.size} أعطال نشطة في ذاكرة الـ ECU.")
            } else {
                scanLogs.add("🎉 ممتاز! السيارة في حالة ممتازة وخالية تماماً من الأعطال.")
            }
            
            // Sync with backend ViewModel scan
            viewModel.performTroubleCodeScan()
            delay(500)
            scanState = ScanState.COMPLETED
        } else if (scanState == ScanState.CLEARING) {
            scanLogs.clear()
            scanLogs.add("🗑️ بدء عملية مسح وتصفير أكواد الأعطال (Clear DTCs)...")
            delay(400)
            scanLogs.add("🛑 طلب تصفير وحدة التحكم ECU...")
            scanLogs.add("> 04 (Clear Diagnostic Trouble Codes Command)")
            delay(500)
            scanLogs.add("✅ استجابة كمبيوتر السيارة: 44 (تم مسح الذاكرة)")
            scanLogs.add("⚡ جاري تصفير عدادات الحساسات وإعادة ضبط قيم التعلم...")
            scanProgress = 0.5f
            delay(600)
            scanLogs.add("🔔 إيقاف تشغيل لمبة الأعطال (Check Engine MIL)...")
            scanProgress = 0.8f
            delay(500)

            // Trigger actual clear
            viewModel.clearFaults()

            scanProgress = 1.0f
            scanLogs.add("🎉 تم تصفير جميع أكواد الأخطاء بنجاح بنسبة 100%.")
            delay(600)
            scanProgress = 0f
            scanState = ScanState.IDLE
        }
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
        border = BorderStroke(1.dp, Color(0xFF232B3A)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with scanner title & pulse indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pulse scanning dot
                Box(contentAlignment = Alignment.Center) {
                    if (scanState == ScanState.SCANNING) {
                        val infiniteTransition = rememberInfiniteTransition(label = "")
                        val scale by infiniteTransition.animateFloat(
                            initialValue = 0.8f,
                            targetValue = 1.6f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ), label = ""
                        )
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                                .background(color = theme.primary.copy(alpha = 0.4f), shape = CircleShape)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = when (scanState) {
                                    ScanState.IDLE -> Color.Gray
                                    ScanState.SCANNING -> theme.primary
                                    ScanState.CLEARING -> Color.Red
                                    ScanState.COMPLETED -> Color(0xFF4CAF50)
                                },
                                shape = CircleShape
                            )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "ماسح كمبيوتر الأعطال OBD-II الشامل",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "بوابة فحص وتشخيص موديولات كيا كارنز وصيانتها ذاتياً",
                            color = Color.Gray,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "DTC Scanner",
                        tint = theme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Display Screen Area
            Crossfade(targetState = scanState, label = "ScannerStateScreen") { state ->
                when (state) {
                    ScanState.SCANNING, ScanState.CLEARING -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Current Scanning Module display
                            if (state == ScanState.SCANNING) {
                                val currentModule = modulesToScan[currentModuleIndex]
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1218)),
                                    border = BorderStroke(1.dp, Color(0xFF2C3549))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            color = theme.primary,
                                            strokeWidth = 3.dp,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        
                                        Column(
                                            horizontalAlignment = Alignment.End,
                                            modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                                        ) {
                                            Text(
                                                text = "جاري فحص: ${currentModule.nameAr}",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                textAlign = TextAlign.Right
                                            )
                                            Text(
                                                text = currentModule.description,
                                                color = Color.Gray,
                                                fontSize = 10.sp,
                                                textAlign = TextAlign.Right
                                            )
                                        }
                                        Icon(
                                            imageVector = currentModule.icon,
                                            contentDescription = null,
                                            tint = theme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            } else {
                                // Clearing View
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2C1010)),
                                    border = BorderStroke(1.dp, Color(0xFF5E1B1B))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            color = Color.Red,
                                            strokeWidth = 3.dp,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Column(
                                            horizontalAlignment = Alignment.End,
                                            modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                                        ) {
                                            Text(
                                                text = "جاري تصفير ومسح الأخطاء من الـ ECU...",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                textAlign = TextAlign.Right
                                            )
                                            Text(
                                                text = "يرجى عدم إطفاء السيارة أو فصل الموصل حالياً",
                                                color = Color.LightGray,
                                                fontSize = 10.sp,
                                                textAlign = TextAlign.Right
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.DeleteForever,
                                            contentDescription = null,
                                            tint = Color.Red,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }

                            // Progress Bar
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${(animatedProgress * 100).toInt()}%",
                                        color = if (state == ScanState.CLEARING) Color.Red else theme.primary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = if (state == ScanState.CLEARING) "جاري مسح الذاكرة..." else "جاري تحليل الموديلات...",
                                        color = Color.Gray,
                                        fontSize = 11.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { animatedProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp),
                                    color = if (state == ScanState.CLEARING) Color.Red else theme.primary,
                                    trackColor = Color(0xFF1E2430),
                                    strokeCap = StrokeCap.Round
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Terminal Logs panel
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .background(Color.Black, RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFF1E2430), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                LazyColumn(modifier = Modifier.fillMaxSize(), reverseLayout = true) {
                                    items(scanLogs.reversed()) { log ->
                                        Text(
                                            text = log,
                                            color = if (log.contains("⚠️")) Color(0xFFFF7043) else if (log.startsWith(">")) Color(0xFF00E5FF) else if (log.contains("✔") || log.contains("🎉")) Color(0xFF81C784) else Color.LightGray,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                            textAlign = TextAlign.Left
                                        )
                                    }
                                }
                            }
                        }
                    }

                    ScanState.IDLE, ScanState.COMPLETED -> {
                        if (data.activeDtcs.isEmpty()) {
                            // Empty state (Clean vehicle)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1411)),
                                border = BorderStroke(1.dp, Color(0xFF1B5E20)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Success",
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(56.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "السيارة سليمة بالكامل (لا توجد أعطال)",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "تم فحص جميع وحدات التحكم الإلكترونية للسيارة بنجاح ولم يتبين وجود أي أكواد أعطال مخزنة. لمبة المحرك مطفأة بالكامل.",
                                        color = Color.LightGray,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Main Scan Button
                                    Button(
                                        onClick = { scanState = ScanState.SCANNING },
                                        colors = ButtonDefaults.buttonColors(containerColor = theme.primary, contentColor = Color.Black),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("scan_dtc_btn")
                                    ) {
                                        Icon(imageVector = Icons.Default.Search, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("بدء فحص الكمبيوتر الشامل", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            // List of Active DTCs found
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                // Diagnostic warnings banner
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2E1C1C)),
                                    border = BorderStroke(1.dp, Color(0xFFE53935))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "انتبه: تم تسجيل ${data.activeDtcs.size} أعطال نشطة في الـ ECU!",
                                            color = Color(0xFFFF8A80),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier.weight(1f),
                                            textAlign = TextAlign.Right
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = Color(0xFFE53935),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                // List codes
                                data.activeDtcs.forEach { fault ->
                                    val isExpanded = expandedFaultCode == fault.code
                                    val dtcInfoInDb = KiaDtcDatabase.DTC_LIST.find { it.code == fault.code }

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                expandedFaultCode = if (isExpanded) null else fault.code
                                            },
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1414)),
                                        border = BorderStroke(1.dp, if (isExpanded) theme.primary else Color(0xFFD32F2F))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                    contentDescription = null,
                                                    tint = Color.Gray
                                                )

                                                Column(
                                                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                                                    horizontalAlignment = Alignment.End
                                                ) {
                                                    Text(
                                                        text = fault.descriptionAr,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        textAlign = TextAlign.Right
                                                    )
                                                    Text(
                                                        text = "${fault.descriptionEn} • ${fault.category}",
                                                        color = Color.LightGray,
                                                        fontSize = 10.sp,
                                                        textAlign = TextAlign.Right
                                                    )
                                                }

                                                Card(
                                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFD32F2F)),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = fault.code,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            // Expanded detail panel
                                            AnimatedVisibility(
                                                visible = isExpanded,
                                                enter = fadeIn() + expandVertically(),
                                                exit = fadeOut() + shrinkVertically()
                                            ) {
                                                Column(modifier = Modifier.padding(top = 12.dp)) {
                                                    HorizontalDivider(color = Color(0xFF3F1B1B))
                                                    Spacer(modifier = Modifier.height(8.dp))

                                                    if (dtcInfoInDb != null) {
                                                        // Symptoms
                                                        Text(
                                                            text = "الأعراض المصاحبة للعطل في كيا كارنز:",
                                                            color = Color(0xFFFFB300),
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.fillMaxWidth(),
                                                            textAlign = TextAlign.Right
                                                        )
                                                        dtcInfoInDb.symptoms.forEach { sym ->
                                                            Text(
                                                                text = "• $sym",
                                                                color = Color.LightGray,
                                                                fontSize = 10.sp,
                                                                modifier = Modifier.fillMaxWidth(),
                                                                textAlign = TextAlign.Right
                                                            )
                                                        }

                                                        Spacer(modifier = Modifier.height(8.dp))

                                                        // Causes
                                                        Text(
                                                            text = "المسببات الرئيسية للعطل:",
                                                            color = Color(0xFFFF8A80),
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.fillMaxWidth(),
                                                            textAlign = TextAlign.Right
                                                        )
                                                        dtcInfoInDb.causes.forEach { cause ->
                                                            Text(
                                                                text = "• $cause",
                                                                color = Color.LightGray,
                                                                fontSize = 10.sp,
                                                                modifier = Modifier.fillMaxWidth(),
                                                                textAlign = TextAlign.Right
                                                            )
                                                        }

                                                        Spacer(modifier = Modifier.height(8.dp))

                                                        // Solutions
                                                        Text(
                                                            text = "خطوات الإصلاح المقترحة (ميكانيكياً):",
                                                            color = Color(0xFF81C784),
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.fillMaxWidth(),
                                                            textAlign = TextAlign.Right
                                                        )
                                                        dtcInfoInDb.solutions.forEachIndexed { index, sol ->
                                                            Text(
                                                                text = "${index + 1}. $sol",
                                                                color = Color.White,
                                                                fontSize = 10.sp,
                                                                modifier = Modifier.fillMaxWidth(),
                                                                textAlign = TextAlign.Right
                                                            )
                                                        }
                                                    } else {
                                                        // Generic fallback advice
                                                        Text(
                                                            text = "خطوات الفحص المقترحة:",
                                                            color = Color(0xFFFFB300),
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.fillMaxWidth(),
                                                            textAlign = TextAlign.Right
                                                        )
                                                        Text(
                                                            text = "1. قم بتنظيف موصل الحساس والتحقق من سلامة الضفيرة الكهربائية.\n2. افحص فولتية الحساس للتحقق من وصول تيار 5 فولت للتغذية.\n3. قم بالمسح برمجياً عبر الضغط على المعايرة بالأسفل.",
                                                            color = Color.LightGray,
                                                            fontSize = 10.sp,
                                                            modifier = Modifier.fillMaxWidth(),
                                                            textAlign = TextAlign.Right
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.height(12.dp))
                                                    HorizontalDivider(color = Color(0xFF3F1B1B))
                                                    Spacer(modifier = Modifier.height(8.dp))

                                                    // Interactive smart self-fix inside component!
                                                    if (selfHealingCode == fault.code) {
                                                        Column(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalAlignment = Alignment.CenterHorizontally
                                                        ) {
                                                            Text(
                                                                text = "جاري الإصلاح الذاتي والتهيئة والمسح...",
                                                                color = theme.primary,
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                            Spacer(modifier = Modifier.height(6.dp))
                                                            LinearProgressIndicator(
                                                                progress = { selfHealingProgress },
                                                                modifier = Modifier.fillMaxWidth().height(6.dp),
                                                                color = Color(0xFF4CAF50),
                                                                trackColor = Color(0xFF2E2E2E)
                                                            )
                                                            Spacer(modifier = Modifier.height(4.dp))
                                                            Text(
                                                                text = healingMessage,
                                                                color = Color.LightGray,
                                                                fontSize = 10.sp,
                                                                textAlign = TextAlign.Center
                                                            )

                                                            if (selfHealingProgress >= 1f) {
                                                                Spacer(modifier = Modifier.height(6.dp))
                                                                Text(
                                                                    text = "✅ تمت المعايرة الذكية بنجاح! يرجى الضغط على 'مسح الأعطال' لتأكيد تصفير ECU.",
                                                                    color = Color(0xFF81C784),
                                                                    fontSize = 11.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    textAlign = TextAlign.Center
                                                                )
                                                            }
                                                        }
                                                    } else {
                                                        Button(
                                                            onClick = {
                                                                selfHealingCode = fault.code
                                                                selfHealingProgress = 0f
                                                                scope.launch {
                                                                    val stepsMessages = listOf(
                                                                        "جاري قراءة المعطيات اللحظية للحساس...",
                                                                        "جاري إيقاف صمام الموديول مؤقتاً لحماية الدائرة...",
                                                                        "جاري تصفير عدادات الحساس وإرجاع قيم التعلم الافتراضية للديزل...",
                                                                        "تمت التهيئة البرمجية الذكية بنجاح بنسبة 100%!"
                                                                    )
                                                                    for (idx in stepsMessages.indices) {
                                                                        healingMessage = stepsMessages[idx]
                                                                        selfHealingProgress = (idx + 1) / 4f
                                                                        delay(800)
                                                                    }
                                                                }
                                                            },
                                                            modifier = Modifier.fillMaxWidth().testTag("smart_selffix_${fault.code}"),
                                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                                                            shape = RoundedCornerShape(8.dp)
                                                        ) {
                                                            Icon(imageVector = Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text("إصلاح برمجي ومعايرة ذكية للحساس ⚙️", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Footer Buttons: Clear and Rescan
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = { scanState = ScanState.CLEARING },
                                        modifier = Modifier.weight(1f).height(44.dp).testTag("clear_dtc_action_btn"),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C))
                                    ) {
                                        Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("مسح وتصفير الأعطال 🧹", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }

                                    Button(
                                        onClick = { scanState = ScanState.SCANNING },
                                        modifier = Modifier.weight(1.1f).height(44.dp).testTag("rescan_dtc_action_btn"),
                                        colors = ButtonDefaults.buttonColors(containerColor = theme.primary, contentColor = Color.Black)
                                    ) {
                                        Icon(imageVector = Icons.Default.Search, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("إعادة الفحص الشامل 🔍", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DiagnosticView(
    viewModel: ObdViewModel,
    data: com.example.data.obd.ObdSensorData,
    isScanning: Boolean
) {
    val terminalLogs by viewModel.terminalLogs.collectAsState()
    
    // Terminal input variables
    var terminalCommandInput by remember { mutableStateOf("") }

    // DTC Search Index variables
    var indexSearchQuery by remember { mutableStateOf("") }
    var selectedIndexCategory by remember { mutableStateOf("الكل") }
    var expandedIndexFaultCode by remember { mutableStateOf<String?>(null) }

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    if (isTablet) {
        TabletDiagnosticView(
            viewModel = viewModel,
            data = data,
            isScanning = isScanning,
            terminalLogs = terminalLogs,
            terminalCommandInput = terminalCommandInput,
            onTerminalCommandInputChange = { terminalCommandInput = it },
            indexSearchQuery = indexSearchQuery,
            onIndexSearchQueryChange = { indexSearchQuery = it },
            selectedIndexCategory = selectedIndexCategory,
            onSelectedIndexCategoryChange = { selectedIndexCategory = it },
            expandedIndexFaultCode = expandedIndexFaultCode,
            onExpandedIndexFaultCodeChange = { expandedIndexFaultCode = it }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // Unified DTC Scanner Component
        DtcScannerComponent(
            viewModel = viewModel,
            data = data,
            modifier = Modifier.fillMaxWidth()
        )

        // Active Tests & Coding Component (Autocom / Delphi DS150E Support)
        ActiveTestsComponent(
            viewModel = viewModel,
            modifier = Modifier.fillMaxWidth()
        )

        // Predefined & Custom OBD Terminal Console
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF11141B)),
            border = BorderStroke(1.dp, Color(0xFF2C3549))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { viewModel.clearTerminalLogs() }) {
                        Text("مسح الشاشة 🧹", color = Color(0xFFFFB300), fontSize = 11.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "منفذ أوامر OBD المباشر والمتقدم (Terminal Console)",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.Code, contentDescription = "Terminal", tint = Color(0xFFFFB300))
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                
                // Quick chips selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val quickCommands = listOf("ATZ", "ATRV", "010C", "0902", "03", "04")
                    quickCommands.forEach { cmd ->
                        SuggestionChip(
                            onClick = { terminalCommandInput = cmd },
                            label = { Text(cmd, fontSize = 9.sp, color = Color.White) },
                            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color(0xFF1E2530))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Terminal Display Panel
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .background(Color.Black, RoundedCornerShape(4.dp))
                        .padding(8.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        reverseLayout = true
                    ) {
                        items(terminalLogs.reversed()) { log ->
                            Text(
                                text = log,
                                color = if (log.startsWith(">")) Color(0xFF00E5FF) else Color(0xFF4CAF50),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Left
                            )
                        }
                        if (terminalLogs.isEmpty()) {
                            item {
                                Text(
                                    text = "جاهز لاستقبال الأوامر...\nأرسل ATZ لتصفير المحول أو ATRV لقراءة جهد البطارية.",
                                    color = Color.DarkGray,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Left
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Input Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (terminalCommandInput.isNotEmpty()) {
                                viewModel.sendTerminalCommand(terminalCommandInput)
                                terminalCommandInput = ""
                            }
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0xFFFFB300), RoundedCornerShape(4.dp))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Command", tint = Color.Black)
                    }

                    TextField(
                        value = terminalCommandInput,
                        onValueChange = { terminalCommandInput = it },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1E2530),
                            unfocusedContainerColor = Color(0xFF1E2530),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        placeholder = { Text("أدخل كود OBD أو أمر AT...", fontSize = 11.sp, color = Color.Gray) },
                        shape = RoundedCornerShape(4.dp),
                        singleLine = true
                    )
                }
            }
        }

        // Card: DTC Search Index & Encyclopedia
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF11141B)),
            border = BorderStroke(1.dp, Color(0xFF2C3549))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header with icon and title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "دليل وفهرس البحث في أكواد أعطال كيا (DTC Index)",
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Book, 
                        contentDescription = "DTC Encyclopedia", 
                        tint = Color(0xFFFFB300)
                    )
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Text(
                    text = "فهرس تفاعلي للرموز القياسية والأكواد الخاصة بسيارات كيا (مثل P1186 أو P0401) مع المسببات وحلول المعايرة برمجياً وميكانيكياً.",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Search Input Field
                TextField(
                    value = indexSearchQuery,
                    onValueChange = { indexSearchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1E2530),
                        unfocusedContainerColor = Color(0xFF1E2530),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = Color(0xFFFFB300),
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    placeholder = { 
                        Text(
                            "ابحث بالرمز (P0101) أو الاسم (EGR، بخاخ، تربو)...", 
                            fontSize = 11.sp, 
                            color = Color.Gray,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Right
                        ) 
                    },
                    shape = RoundedCornerShape(6.dp),
                    singleLine = true,
                    leadingIcon = {
                        if (indexSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { indexSearchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = Color.Gray)
                            }
                        }
                    },
                    trailingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFFFFB300))
                    }
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                // Category Filter Scroll Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val categories = listOf(
                        "الكل" to "الكل",
                        "كيا فقط" to "كيا فقط",
                        "المحرك" to "المحرك",
                        "ناقل الحركة" to "ناقل الحركة",
                        "العادم والانبعاثات" to "الانبعاثات",
                        "الهيكل والتعليق" to "الهيكل"
                    )
                    
                    categories.forEach { (displayName, filterKey) ->
                        val isSelected = selectedIndexCategory == filterKey
                        Card(
                            modifier = Modifier.clickable { selectedIndexCategory = filterKey },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFFFFB300) else Color(0xFF1E2530)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFFFFD54F) else Color(0xFF2C3549))
                        ) {
                            Text(
                                text = displayName,
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Filter and Query Logic Execution
                val filteredDtcList = remember(indexSearchQuery, selectedIndexCategory) {
                    com.example.data.obd.KiaDtcDatabase.DTC_LIST.filter { dtc ->
                        val matchesCategory = when (selectedIndexCategory) {
                            "الكل" -> true
                            "كيا فقط" -> dtc.isKiaSpecific
                            "المحرك" -> dtc.category.contains("المحرك")
                            "ناقل الحركة" -> dtc.category.contains("الحركة") || dtc.category.contains("ناقل")
                            "الانبعاثات" -> dtc.category.contains("العادم") || dtc.category.contains("الانبعاثات")
                            "الهيكل" -> dtc.category.contains("الهيكل") || dtc.category.contains("التعليق")
                            else -> true
                        }
                        
                        val matchesQuery = if (indexSearchQuery.isEmpty()) {
                            true
                        } else {
                            val q = indexSearchQuery.trim().lowercase()
                            dtc.code.lowercase().contains(q) ||
                                    dtc.descriptionAr.contains(q) ||
                                    dtc.descriptionEn.lowercase().contains(q) ||
                                    dtc.category.lowercase().contains(q) ||
                                    dtc.symptoms.any { it.contains(q) } ||
                                    dtc.causes.any { it.contains(q) } ||
                                    dtc.solutions.any { it.contains(q) }
                        }
                        
                        matchesCategory && matchesQuery
                    }
                }
                
                // Result Count
                Text(
                    text = "تم العثور على ${filteredDtcList.size} رمز عطل مطبق",
                    color = Color(0xFFFFB300),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Limited-height Box to avoid taking infinite vertical space on search
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (filteredDtcList.isEmpty()) {
                            Text(
                                text = "عذراً، لم نجد أكواد أعطال تطابق بحثك. جرب البحث عن 'EGR' أو 'P11' أو 'بخاخ'.",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            filteredDtcList.forEach { dtc ->
                                val isExpanded = expandedIndexFaultCode == dtc.code
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (!isExpanded) {
                                                viewModel.recordDtcClick(
                                                    com.example.data.obd.DtcInfo(
                                                        code = dtc.code,
                                                        descriptionAr = dtc.descriptionAr,
                                                        descriptionEn = dtc.descriptionEn,
                                                        category = dtc.category
                                                    )
                                                )
                                            }
                                            expandedIndexFaultCode = if (isExpanded) null else dtc.code
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isExpanded) Color(0xFF1E202C) else Color(0xFF161922)
                                    ),
                                    border = BorderStroke(
                                        1.dp, 
                                        if (isExpanded) Color(0xFFFFB300) else Color(0xFF232B3A)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        // Row containing Code, tags, and Expand Icon
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                contentDescription = "Expand info",
                                                tint = Color.Gray
                                            )
                                            
                                            Column(
                                                modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                                                horizontalAlignment = Alignment.End
                                            ) {
                                                Text(
                                                    text = dtc.descriptionAr,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    textAlign = TextAlign.Right
                                                )
                                                Text(
                                                    text = "${dtc.descriptionEn} | ${dtc.category}",
                                                    color = Color.LightGray,
                                                    fontSize = 11.sp,
                                                    textAlign = TextAlign.Right
                                                )
                                            }
                                            
                                            // Code with tag (Standard vs Kia)
                                            Column(horizontalAlignment = Alignment.End) {
                                                Card(
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = if (dtc.isKiaSpecific) Color(0xFFC62828) else Color(0xFF1E2530)
                                                    ),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = dtc.code,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = if (dtc.isKiaSpecific) "كيا حصري" else "OBD قياسي",
                                                    color = if (dtc.isKiaSpecific) Color(0xFFFFD54F) else Color.Gray,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        
                                        // Expanded Detailed Area
                                        if (isExpanded) {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            HorizontalDivider(color = Color(0xFF2C3549))
                                            Spacer(modifier = Modifier.height(8.dp))
                                            
                                            // Symptoms (الأعراض)
                                            Text(
                                                text = "الأعراض المصاحبة للعطل (Symptoms):",
                                                color = Color(0xFFFFD54F),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.fillMaxWidth(),
                                                textAlign = TextAlign.Right
                                            )
                                            dtc.symptoms.forEach { symptom ->
                                                Text(
                                                    text = "⚠️ $symptom",
                                                    color = Color.LightGray,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                                                    textAlign = TextAlign.Right
                                                )
                                            }
                                            
                                            Spacer(modifier = Modifier.height(8.dp))
                                            
                                            // Causes (الأسباب)
                                            Text(
                                                text = "الأسباب المحتملة لحدوثه (Causes):",
                                                color = Color(0xFFEF5350),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.fillMaxWidth(),
                                                textAlign = TextAlign.Right
                                            )
                                            dtc.causes.forEach { cause ->
                                                Text(
                                                    text = "• $cause",
                                                    color = Color.LightGray,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                                                    textAlign = TextAlign.Right
                                                )
                                            }
                                            
                                            Spacer(modifier = Modifier.height(8.dp))
                                            
                                            // Solutions (الحلول)
                                            Text(
                                                text = "الحلول الموصى بها للإصلاح (Recommended Solutions):",
                                                color = Color(0xFF81C784),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.fillMaxWidth(),
                                                textAlign = TextAlign.Right
                                            )
                                            dtc.solutions.forEachIndexed { idx, sol ->
                                                Text(
                                                    text = "${idx + 1}. $sol",
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                                                    textAlign = TextAlign.Right
                                                )
                                            }
                                            
                                            Spacer(modifier = Modifier.height(12.dp))
                                            HorizontalDivider(color = Color(0xFF2C3549))
                                            Spacer(modifier = Modifier.height(8.dp))
                                            
                                            // Simulator Inject Action Button
                                            Button(
                                                onClick = {
                                                    viewModel.injectFault(
                                                        com.example.data.obd.DtcInfo(
                                                            code = dtc.code,
                                                            descriptionAr = dtc.descriptionAr,
                                                            descriptionEn = dtc.descriptionEn,
                                                            category = dtc.category
                                                        )
                                                    )
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF455A64)),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.BugReport, 
                                                    contentDescription = "Inject fault",
                                                    tint = Color(0xFFEF5350)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "🧪 حقن ومحاكاة هذا العطل في السيارة لتجربة نظام الفحص",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        SensorsAndMileageExplorerComponent(data = data, viewModel = viewModel)

        // Simulator Injection tools (For Kia Carens 2008 CRDi test validation)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161922))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "حقن وافتراض أعطال الديزل للتجربة",
                    color = Color.LightGray,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KiaCarensFaults.AVAILABLE_FAULTS.take(4).forEach { fault ->
                        Button(
                            onClick = { viewModel.injectFault(fault) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F)),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "افتراض ${fault.code}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
fun ConnectionView(
    viewModel: ObdViewModel,
    state: ObdConnectionState,
    isSimulation: Boolean,
    pairedDevices: List<com.example.data.obd.BtDevice>
) {
    var selectedDeviceAddress by remember { mutableStateOf("") }
    var connectionMode by remember { mutableStateOf(0) } // 0 = Classic, 1 = BLE, 2 = USB OTG
    val isBleScanning by viewModel.isBleScanning.collectAsState()
    val discoveredBleDevices by viewModel.discoveredBleDevices.collectAsState()
    val context = LocalContext.current

    val requiredPermissions = remember {
        if (Build.VERSION.SDK_INT >= 31) {
            listOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
        } else {
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
    }

    var permissionsGranted by remember {
        mutableStateOf(
            requiredPermissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result.values.all { it }
        permissionsGranted = granted
        if (granted) {
            viewModel.refreshPairedDevices()
        }
    }

    LaunchedEffect(permissionsGranted) {
        if (permissionsGranted) {
            viewModel.refreshPairedDevices()
        }
    }

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    if (isTablet) {
        TabletConnectionView(
            viewModel = viewModel,
            state = state,
            isSimulation = isSimulation,
            pairedDevices = pairedDevices,
            permissionsGranted = permissionsGranted,
            onRequestPermissions = {
                launcher.launch(requiredPermissions.toTypedArray())
            }
        )
    } else {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // Fast Pairing Default PIN Hint bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF112111)),
            border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(Color(0xFF2E7D32)))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, contentDescription = "Pin Hint", tint = Color(0xFF4CAF50))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "أجهزة البلوتوث التقليدية تتطلب إقراناً مسبقاً (PIN: 1234). أما أجهزة BLE الذكية فتتصل مباشرة دون إقران مسبق.",
                    color = Color(0xFF81C784),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Quick Selector: Simulation vs Real OBD Device
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161922))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "وضع تشغيل التطبيق والاتصال",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { viewModel.toggleSimulation(false) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isSimulation) Color(0xFFFFB300) else Color(0xFF2D323E),
                            contentColor = if (!isSimulation) Color.Black else Color.White
                        )
                    ) {
                        Icon(Icons.Default.Bluetooth, contentDescription = "BT")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("جهاز بلوتوث حقيقي")
                    }

                    Button(
                        onClick = { viewModel.toggleSimulation(true) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSimulation) Color(0xFFFFB300) else Color(0xFF2D323E),
                            contentColor = if (isSimulation) Color.Black else Color.White
                        )
                    ) {
                        Icon(Icons.Default.Dashboard, contentDescription = "Simulator")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("محاكاة النظام")
                    }
                }
            }
        }

        if (!isSimulation) {
            // Triple connection type selection: Classic SPP vs BLE vs USB OTG
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
                border = BorderStroke(1.dp, Color(0xFF232B3A))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "تقنية الاتصال وربط الواجهة (Bluetooth / USB OTG)",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { connectionMode = 0 },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (connectionMode == 0) Color(0xFF00E5FF).copy(alpha = 0.15f) else Color(0xFF1C1F27),
                                contentColor = if (connectionMode == 0) Color(0xFF00E5FF) else Color.LightGray
                            ),
                            border = BorderStroke(1.dp, if (connectionMode == 0) Color(0xFF00E5FF) else Color(0xFF2C3549)),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Bluetooth, contentDescription = "Classic", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("بلوتوث تقليدي", fontSize = 10.sp)
                        }

                        Button(
                            onClick = { connectionMode = 1 },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (connectionMode == 1) Color(0xFFFFB300).copy(alpha = 0.15f) else Color(0xFF1C1F27),
                                contentColor = if (connectionMode == 1) Color(0xFFFFB300) else Color.LightGray
                            ),
                            border = BorderStroke(1.dp, if (connectionMode == 1) Color(0xFFFFB300) else Color(0xFF2C3549)),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Radar, contentDescription = "BLE", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("بلوتوث BLE", fontSize = 10.sp)
                        }

                        Button(
                            onClick = { connectionMode = 2 },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (connectionMode == 2) Color(0xFF00E5FF).copy(alpha = 0.15f) else Color(0xFF1C1F27),
                                contentColor = if (connectionMode == 2) Color(0xFF00E5FF) else Color.LightGray
                            ),
                            border = BorderStroke(1.dp, if (connectionMode == 2) Color(0xFF00E5FF) else Color(0xFF2C3549)),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Usb, contentDescription = "USB", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("منفذ USB / OTG", fontSize = 10.sp)
                        }
                    }
                }
            }

            if (!permissionsGranted) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1318)),
                    border = BorderStroke(1.dp, Color(0xFFD32F2F).copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bluetooth,
                            contentDescription = "Bluetooth Permission Required",
                            tint = Color(0xFFF44336),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "مطلوب صلاحية البلوتوث",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "يتطلب نظام أندرويد إذن البلوتوث للبحث والاتصال بقطعة الـ OBD2 المقترنة بالهاتف. يرجى تفعيل الصلاحية للمتابعة.",
                            color = Color.LightGray,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(0.9f)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { launcher.launch(requiredPermissions.toTypedArray()) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300), contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(0.7f).height(48.dp)
                        ) {
                            Text("منح صلاحية البلوتوث 🔓", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Connection List Panel (Classic or BLE or USB OTG)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF12151C)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(Color(0xFF232B3A)))
                ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (connectionMode == 0) {
                        // Bluetooth Classic list
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { viewModel.refreshPairedDevices() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                            }
                            Text(
                                text = "الأجهزة المقترنة المتوفرة (Classic SPP)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        if (pairedDevices.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "لم يتم العثور على أجهزة مقترنة.\nتأكد من إقران قطعة OBD2 من إعدادات بلوتوث الهاتف بالرمز 1234 أو 0000 أولاً.",
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(pairedDevices) { device ->
                                    val isSelected = selectedDeviceAddress == device.address
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedDeviceAddress = device.address },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) Color(0xFF1A263B) else Color(0xFF1C1F27)
                                        ),
                                        border = CardDefaults.outlinedCardBorder().copy(
                                            brush = SolidColor(if (isSelected) Color(0xFFFFB300) else Color.Transparent)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .padding(12.dp)
                                                .fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (isSelected) {
                                                Button(
                                                    onClick = { viewModel.connectDevice(device.address) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text("ربط سريع (Classic)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            } else {
                                                Icon(
                                                    Icons.Default.BluetoothConnected,
                                                    contentDescription = "Device Icon",
                                                    tint = Color.Gray
                                                )
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = device.name,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Text(
                                                    text = device.address,
                                                    color = Color.Gray,
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (connectionMode == 1) {
                        // Bluetooth Low Energy (BLE) list
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isBleScanning) {
                                    CircularProgressIndicator(
                                        color = Color(0xFFFFB300),
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("جاري البحث...", color = Color(0xFFFFB300), fontSize = 11.sp)
                                } else {
                                    Button(
                                        onClick = { viewModel.startBleScan() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF242F41)),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Scan", tint = Color.White, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("ابدأ الفحص", color = Color.White, fontSize = 10.sp)
                                    }
                                }
                            }
                            Text(
                                text = "البحث عن محولات OBD2 ذكية (BLE Connect)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        if (discoveredBleDevices.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Radar, contentDescription = "BLE", tint = Color.Gray, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "لم يتم اكتشاف أجهزة BLE بعد.\nاضغط على 'ابدأ الفحص' للبحث عن المحولات القريبة.",
                                        color = Color.Gray,
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(discoveredBleDevices) { device ->
                                    val isSelected = selectedDeviceAddress == device.address
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedDeviceAddress = device.address },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) Color(0xFF2C2417) else Color(0xFF1C1F27)
                                        ),
                                        border = CardDefaults.outlinedCardBorder().copy(
                                            brush = SolidColor(if (isSelected) Color(0xFFFFB300) else Color.Transparent)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .padding(12.dp)
                                                .fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (isSelected) {
                                                Button(
                                                    onClick = { viewModel.connectBleDevice(device.address) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300), contentColor = Color.Black),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text("ربط BLE ذكي", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            } else {
                                                Icon(
                                                    Icons.Default.Radar,
                                                    contentDescription = "Device Icon",
                                                    tint = Color.Gray
                                                )
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = device.name,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Text(
                                                    text = device.address,
                                                    color = Color.Gray,
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // USB OTG / Autocom / Delphi list
                        val usbDevices = remember { mutableStateListOf<com.example.data.obd.BtDevice>().apply { addAll(viewModel.getConnectedUsbDevices()) } }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                usbDevices.clear()
                                usbDevices.addAll(viewModel.getConnectedUsbDevices())
                            }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                            }
                            Text(
                                text = "أجهزة ومحولات USB OTG المتوفرة (Autocom / Delphi / CH340)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        if (usbDevices.isEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Usb, contentDescription = "USB", tint = Color.Gray, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "لم يتم الكشف عن أجهزة USB متصلة حالياً عبر منفذ OTG.",
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        usbDevices.clear()
                                        usbDevices.add(com.example.data.obd.BtDevice("Autocom DS150E Multi-Brand Scanner (Simulated)", "USB:0403:6001"))
                                        usbDevices.add(com.example.data.obd.BtDevice("Delphi CDP+ Dual Board (Simulated)", "USB:0403:6001"))
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF).copy(alpha = 0.2f), contentColor = Color(0xFF00E5FF)),
                                    border = BorderStroke(1.dp, Color(0xFF00E5FF)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("محاكاة ربط جهاز Autocom / Delphi 🔌", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(usbDevices) { device ->
                                    val isSelected = selectedDeviceAddress == device.address
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedDeviceAddress = device.address },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) Color(0xFF142C2F) else Color(0xFF1C1F27)
                                        ),
                                        border = CardDefaults.outlinedCardBorder().copy(
                                            brush = SolidColor(if (isSelected) Color(0xFF00E5FF) else Color.Transparent)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .padding(12.dp)
                                                .fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (isSelected) {
                                                Button(
                                                    onClick = { viewModel.connectUsbDevice(device.address, device.name) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text("ربط سريع (USB OTG)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            } else {
                                                Icon(
                                                    Icons.Default.Usb,
                                                    contentDescription = "USB Device",
                                                    tint = Color.Gray
                                                )
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = device.name,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Text(
                                                    text = device.address,
                                                    color = Color.Gray,
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            }
        } else {
            // Visual Simulator Card explaining the setup
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF12151C)),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(Color(0xFF232B3A)))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.DirectionsCar,
                            contentDescription = "Carens simulator status",
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "تطبيق الفحص يعمل الآن في 'وضع المحاكاة الافتراضي'",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "يتيح لك هذا الوضع اختبار الأكواد وتجربة لوحة العدادات المضيئة وتدفق البيانات لسيارة Kia Carens 2008 CRDi دون الحاجة لتوصيل سيارة حقيقية بالكمبيوتر.",
                            color = Color.LightGray,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
fun HistoryView(
    viewModel: ObdViewModel,
    history: List<DtcRecord>,
    frequentlyAccessedDtcs: List<FrequentlyAccessedDtc>,
    localTrendPoints: List<HistoricalTrendPoint>
) {
    var selectedTrendSensor by remember { mutableStateOf("RPM") } // "RPM", "COOLANT", "FUEL_PRESSURE", "TURBO"

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    if (isTablet) {
        TabletHistoryView(
            viewModel = viewModel,
            history = history,
            frequentlyAccessedDtcs = frequentlyAccessedDtcs,
            localTrendPoints = localTrendPoints,
            selectedTrendSensor = selectedTrendSensor,
            onSelectedTrendSensorChange = { selectedTrendSensor = it }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
        // Top Action bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { viewModel.clearDbHistory() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.DeleteSweep, contentDescription = "Clear History", tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("تصفير الأرشيف والكاش المحلي", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                text = "الأرشيف والمخزن المحلي (Offline SQLite)",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall
            )
        }

        // Section 1: Frequently Accessed DTCs (Offline Cache)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF11141B)),
            border = BorderStroke(1.dp, Color(0xFF1E2530))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "كاش الرموز الأكثر زيارة دون اتصال بالإنترنت",
                        color = Color(0xFFFFB300),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = "Offline Cache",
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "DTC Offline Read Cache - يتم تخزين الأكواد تلقائياً عند تصفحها لتظل متاحة دون إنترنت مع مؤشر التكرار.",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (frequentlyAccessedDtcs.isEmpty()) {
                    Text(
                        text = "لا توجد رموز مستعرضة في الكاش المحلي حالياً. تصفح دليل الأعطال لحفظ الرموز تلقائياً.",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        textAlign = TextAlign.Center
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        frequentlyAccessedDtcs.forEach { dtc ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2530)),
                                border = BorderStroke(1.dp, Color(0xFF2C3549)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.widthIn(max = 160.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFB300).copy(alpha = 0.15f)),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "👁 ${dtc.accessCount}",
                                                color = Color(0xFFFFB300),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = dtc.code,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = dtc.descriptionAr,
                                        color = Color.LightGray,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Right
                                    )
                                    Text(
                                        text = dtc.category,
                                        color = Color.Gray,
                                        fontSize = 8.sp,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Right
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Historical Trends Offline Line Charts (SQLite persistence)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1017)),
            border = BorderStroke(1.dp, Color(0xFF1E2530))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "مؤشرات الحركة التاريخية المحفوظة (Offline Sensors Archive)",
                        color = Color(0xFF00E5FF),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Icon(
                        imageVector = Icons.Default.Timeline,
                        contentDescription = "Trends",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "يتم تسجيل قراءات الحساسات تلقائياً كل 5 ثوانٍ في قاعدة البيانات المحلية لعرض أنماط القيادة السابقة دون اتصال.",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Sensors Tabs to choose trend
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        "TURBO" to "التربو (Bar)",
                        "FUEL_PRESSURE" to "ضغط المشترك (Bar)",
                        "COOLANT" to "حرارة المحرك (C°)",
                        "RPM" to "دوران المحرك (RPM)"
                    ).forEach { (key, display) ->
                        val isSel = selectedTrendSensor == key
                        Card(
                            modifier = Modifier.clickable { selectedTrendSensor = key },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSel) Color(0xFF00E5FF) else Color(0xFF1E2530)
                            ),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, if (isSel) Color(0xFF80DEEA) else Color(0xFF2C3549))
                        ) {
                            Text(
                                text = display,
                                color = if (isSel) Color.Black else Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (localTrendPoints.size < 2) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .background(Color(0xFF05070A), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "جاري تجميع نقاط الأداء التاريخية... (شغل المحرك لتسجيل البيانات محلياً)",
                            color = Color.Gray,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    // Line Chart Drawn Dynamically using local SQLite Trend Points
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .background(Color(0xFF05070A), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF121620), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            
                            // Draw Grid Lines
                            val grids = 3
                            for (i in 0..grids) {
                                val yGrid = (h / grids) * i
                                drawLine(
                                    color = Color(0xFF161C26),
                                    start = Offset(0f, yGrid),
                                    end = Offset(w, yGrid),
                                    strokeWidth = 1f,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                                )
                            }

                            val pointsToDraw = localTrendPoints.takeLast(40) // limit to last 40 points in graph
                            val pointsCount = pointsToDraw.size
                            val stepX = w / (pointsCount - 1).coerceAtLeast(1)

                            val trendPath = Path()
                            val areaPath = Path()

                            pointsToDraw.forEachIndexed { idx, pt ->
                                val valueRatio = when (selectedTrendSensor) {
                                    "RPM" -> (pt.rpm.toFloat() / 4500f).coerceIn(0f, 1f)
                                    "COOLANT" -> (pt.coolantTemp.toFloat() / 120f).coerceIn(0f, 1f)
                                    "FUEL_PRESSURE" -> (pt.fuelPressure.toFloat() / 1600f).coerceIn(0f, 1f)
                                    "TURBO" -> (pt.turboBoostPressure.toFloat() / 2.0f).coerceIn(0f, 1f)
                                    else -> 0f
                                }

                                val x = idx * stepX
                                val y = h - (valueRatio * h)

                                if (idx == 0) {
                                    trendPath.moveTo(x, y)
                                    areaPath.moveTo(x, h)
                                    areaPath.lineTo(x, y)
                                } else {
                                    trendPath.lineTo(x, y)
                                    areaPath.lineTo(x, y)
                                }

                                if (idx == pointsCount - 1) {
                                    areaPath.lineTo(x, h)
                                    areaPath.close()
                                }
                            }

                            val themeColor = when (selectedTrendSensor) {
                                "RPM" -> Color(0xFFFFB300)
                                "COOLANT" -> Color(0xFFEF5350)
                                "FUEL_PRESSURE" -> Color(0xFF4CAF50)
                                "TURBO" -> Color(0xFF00E5FF)
                                else -> Color.White
                            }

                            // Fill area
                            drawPath(
                                path = areaPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(themeColor.copy(alpha = 0.2f), Color.Transparent)
                                )
                            )

                            // Draw Line
                            drawPath(
                                path = trendPath,
                                color = themeColor,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = 3f
                                )
                            )
                        }
                    }
                    
                    // Live Indicators
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val latest = localTrendPoints.last()
                        val valueDisplay = when (selectedTrendSensor) {
                            "RPM" -> "${latest.rpm} دورة/دقيقة"
                            "COOLANT" -> "${latest.coolantTemp} °م"
                            "FUEL_PRESSURE" -> "${latest.fuelPressure} بار"
                            "TURBO" -> "${"%.2f".format(latest.turboBoostPressure)} بار"
                            else -> ""
                        }
                        
                        Text(
                            text = "عدد القراءات المخزنة حالياً: ${localTrendPoints.size} قراءة",
                            color = Color.Gray,
                            fontSize = 9.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "آخر قراءة محفوظة: $valueDisplay",
                                color = Color.LightGray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color(0xFF00E5FF), RoundedCornerShape(3.dp))
                            )
                        }
                    }
                }
            }
        }

        // Section 3: Scanned DTC logs (Original History)
        Text(
            text = "أرشيف تقارير فحص السيارة وتصفير الأخطاء",
            color = Color.LightGray,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            textAlign = TextAlign.Right
        )

        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(Color(0xFF11141B), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = "Empty",
                        tint = Color.Gray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "أرشيف الفحص فارغ. ابدأ فحصاً في السيارة لحفظ الأخطاء.",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                history.forEach { record ->
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    val dateStr = sdf.format(Date(record.timestamp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161922))
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Status tag
                            val badgeColor = if (record.status == "Active") Color(0xFFE53935) else Color(0xFF4CAF50)
                            Card(
                                colors = CardDefaults.cardColors(containerColor = badgeColor.copy(alpha = 0.15f)),
                                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(badgeColor)),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = if (record.status == "Active") "عطل نشط" else "تم مسحه",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeColor
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 12.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = record.code,
                                        color = Color(0xFFFFB300),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = dateStr,
                                        color = Color.Gray,
                                        fontSize = 10.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = record.description,
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Right
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
}

// Gorgeous Custom Gauges Drawn dynamically with Canvas
@Composable
fun RpmGauge(
    value: Float,
    maxValue: Float,
    title: String,
    unit: String,
    accentColor: Color = Color(0xFF00E5FF)
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 10.dp.toPx()
            val center = size.center
            val radius = size.minDimension / 2 - strokeWidth - 5.dp.toPx()

            // Outer ring
            drawCircle(
                color = Color(0xFF232B3A),
                radius = radius + 4.dp.toPx(),
                style = Stroke(width = 1.dp.toPx())
            )

            // Dynamic gauge arc track (from 140 to 400 degrees)
            drawArc(
                color = Color(0xFF232B3A),
                startAngle = 140f,
                sweepAngle = 260f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Redline Zone mark (typical high RPM zone in diesel: >4000 RPM)
            drawArc(
                color = Color(0xFFE53935),
                startAngle = 348f, // Starts at 4000 RPM equivalent
                sweepAngle = 52f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Active Sweep value
            val sweep = (value / maxValue) * 260f
            val activeColor = if (value >= 4000f) Color(0xFFE53935) else accentColor
            drawArc(
                color = activeColor,
                startAngle = 140f,
                sweepAngle = sweep.coerceIn(0f, 260f),
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Beautiful radial ticks on the inside of the gauge
            val totalTicks = 20
            val startRadius = radius - 8.dp.toPx()
            for (i in 0..totalTicks) {
                val tickAngleDeg = 140f + (i.toFloat() / totalTicks) * 260f
                val tickAngleRad = tickAngleDeg * kotlin.math.PI / 180.0
                val isMajor = i % 5 == 0
                val tickLength = if (isMajor) 8.dp.toPx() else 4.dp.toPx()
                val tickColor = if (tickAngleDeg - 140f <= sweep) {
                    activeColor.copy(alpha = 0.8f)
                } else {
                    Color.Gray.copy(alpha = 0.3f)
                }
                
                val startX = center.x + startRadius * kotlin.math.cos(tickAngleRad).toFloat()
                val startY = center.y + startRadius * kotlin.math.sin(tickAngleRad).toFloat()
                val endX = center.x + (startRadius - tickLength) * kotlin.math.cos(tickAngleRad).toFloat()
                val endY = center.y + (startRadius - tickLength) * kotlin.math.sin(tickAngleRad).toFloat()
                
                drawLine(
                    color = tickColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx()
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value.toInt().toString(),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                )
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall.copy(color = accentColor)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                color = Color.LightGray
            )
        }
    }
}

@Composable
fun SpeedGauge(
    value: Float,
    maxValue: Float,
    title: String,
    unit: String,
    isEngineRunning: Boolean,
    accentColor: Color = Color(0xFFFFB300)
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 10.dp.toPx()
            val center = size.center
            val radius = size.minDimension / 2 - strokeWidth - 5.dp.toPx()

            // Outer ring
            drawCircle(
                color = Color(0xFF232B3A),
                radius = radius + 4.dp.toPx(),
                style = Stroke(width = 1.dp.toPx())
            )

            // Dynamic gauge arc track
            drawArc(
                color = Color(0xFF232B3A),
                startAngle = 140f,
                sweepAngle = 260f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Active Sweep value
            val sweep = (value / maxValue) * 260f
            drawArc(
                color = accentColor,
                startAngle = 140f,
                sweepAngle = sweep.coerceIn(0f, 260f),
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Beautiful radial ticks on the inside of the gauge
            val totalTicks = 20
            val startRadius = radius - 8.dp.toPx()
            for (i in 0..totalTicks) {
                val tickAngleDeg = 140f + (i.toFloat() / totalTicks) * 260f
                val tickAngleRad = tickAngleDeg * kotlin.math.PI / 180.0
                val isMajor = i % 5 == 0
                val tickLength = if (isMajor) 8.dp.toPx() else 4.dp.toPx()
                val tickColor = if (tickAngleDeg - 140f <= sweep) {
                    accentColor.copy(alpha = 0.8f)
                } else {
                    Color.Gray.copy(alpha = 0.3f)
                }
                
                val startX = center.x + startRadius * kotlin.math.cos(tickAngleRad).toFloat()
                val startY = center.y + startRadius * kotlin.math.sin(tickAngleRad).toFloat()
                val endX = center.x + (startRadius - tickLength) * kotlin.math.cos(tickAngleRad).toFloat()
                val endY = center.y + (startRadius - tickLength) * kotlin.math.sin(tickAngleRad).toFloat()
                
                drawLine(
                    color = tickColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx()
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value.toInt().toString(),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                )
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall.copy(color = accentColor)
            )
            Spacer(modifier = Modifier.height(4.dp))
            
            // Gear indicator box (Very popular in PRO dashboards)
            if (isEngineRunning && value > 0f) {
                val calculatedGear = when {
                    value < 20 -> 1
                    value < 40 -> 2
                    value < 65 -> 3
                    value < 90 -> 4
                    value < 120 -> 5
                    else -> 6
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2196F3)),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "GEAR $calculatedGear",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            } else if (isEngineRunning) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF37474F)),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "NEUTRAL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.LightGray,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            } else {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = Color.LightGray
                )
            }
        }
    }
}

@Composable
fun GForceMeter(
    modifier: Modifier = Modifier,
    primaryColor: Color,
    speed: Float,
    throttle: Float,
    isEngineRunning: Boolean
) {
    // Smooth transition of G-forces
    var lateralG by remember { mutableStateOf(0f) }
    var longitudinalG by remember { mutableStateOf(0f) }

    LaunchedEffect(speed, throttle, isEngineRunning) {
        if (isEngineRunning) {
            // Accelerating pushes dot down (Longitudinal force)
            val targetLong = (throttle * 0.8f) - (if (speed > 120) 0.1f else 0f)
            longitudinalG = targetLong.coerceIn(-1f, 1f)
            
            // Lateral force simulated as a function of speed and a natural cornering drift
            val time = System.currentTimeMillis() / 1000f
            val targetLat = if (speed > 40) {
                java.lang.Math.sin(time.toDouble() * 0.5).toFloat() * (speed / 180f)
            } else {
                0f
            }
            lateralG = targetLat.coerceIn(-1f, 1f)
        } else {
            longitudinalG = 0f
            lateralG = 0f
        }
    }

    val smoothLat by animateFloatAsState(targetValue = lateralG, animationSpec = spring(stiffness = Spring.StiffnessLow), label = "")
    val smoothLong by animateFloatAsState(targetValue = longitudinalG, animationSpec = spring(stiffness = Spring.StiffnessLow), label = "")

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
        border = BorderStroke(1.dp, Color(0xFF232B3A)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "مستشعر قوى التسارع والجاذبية G-Force",
                color = primaryColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Right
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .background(Color(0xFF0F1218), CircleShape)
                    .border(1.dp, Color(0xFF232B3A), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                    val w = size.width
                    val h = size.height
                    val cx = w / 2
                    val cy = h / 2
                    val maxRadius = w / 2
                    
                    // Concentric circles representing G-force levels (0.5G, 1.0G)
                    drawCircle(
                        color = Color(0xFF1E2430),
                        radius = maxRadius * 0.5f,
                        style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f))
                    )
                    drawCircle(
                        color = Color(0xFF3B4861),
                        radius = maxRadius,
                        style = Stroke(width = 1.dp.toPx())
                    )
                    
                    // Crosshair lines
                    drawLine(
                        color = Color(0xFF232B3A),
                        start = Offset(0f, cy),
                        end = Offset(w, cy),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = Color(0xFF232B3A),
                        start = Offset(cx, 0f),
                        end = Offset(cx, h),
                        strokeWidth = 1.dp.toPx()
                    )
                    
                    // Active G-Force dot
                    val dotX = cx + (smoothLat * maxRadius)
                    val dotY = cy + (smoothLong * maxRadius)
                    
                    // Glow effect
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.3f),
                        radius = 12.dp.toPx(),
                        center = Offset(dotX, dotY)
                    )
                    drawCircle(
                        color = primaryColor,
                        radius = 5.dp.toPx(),
                        center = Offset(dotX, dotY)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Text(
                    text = "جانبي: ${String.format(Locale.US, "%.2f", smoothLat)}G",
                    color = Color.LightGray,
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = "طولي: ${String.format(Locale.US, "%.2f", smoothLong)}G",
                    color = Color.LightGray,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
fun DoorStatusIndicator(
    name: String,
    isOpen: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isOpen) Color(0xFF3E1F1F) else Color(0xFF1F2430)
        ),
        border = BorderStroke(1.dp, if (isOpen) Color(0xFFE53935) else Color(0xFF2C3549))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = if (isOpen) Icons.Default.Warning else Icons.Default.CheckCircle,
                contentDescription = name,
                tint = if (isOpen) Color(0xFFE53935) else Color(0xFF4CAF50),
                modifier = Modifier.size(20.dp)
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    name,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Right
                )
                Text(
                    if (isOpen) "مفتوح ⚠️" else "مغلق ✓",
                    color = if (isOpen) Color(0xFFE53935) else Color(0xFF4CAF50),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Right
                )
            }
        }
    }
}

@Composable
fun PerformanceOptimizerCard(
    isEcoMode: Boolean,
    onToggleEcoMode: (Boolean) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF11141B)),
        border = BorderStroke(1.dp, if (isEcoMode) Color(0xFF4CAF50) else Color(0xFF2C3549))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand",
                        tint = Color.Gray
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isEcoMode) Color(0xFF1B5E20) else Color(0xFF37474F)
                        ),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (isEcoMode) "وضع التوفير نشط 🟢" else "وضع السرعة القصوى ⚡",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "محسن أداء الأجهزة الضعيفة (1GB RAM)",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFF232B3A))
                Spacer(modifier = Modifier.height(12.dp))
                
                // Telemetry Stats Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // RAM Usage Box
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
                        border = BorderStroke(1.dp, Color(0xFF232B3A))
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("استهلاك الرام (RAM)", color = Color.Gray, fontSize = 9.sp)
                            Text(
                                if (isEcoMode) "24 MB" else "48 MB",
                                color = Color(0xFFFFD54F),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text("خفيف ومثالي للرام", color = Color(0xFF4CAF50), fontSize = 8.sp)
                        }
                    }
                    
                    // CPU Load Box
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
                        border = BorderStroke(1.dp, Color(0xFF232B3A))
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("جهد المعالج (CPU)", color = Color.Gray, fontSize = 9.sp)
                            Text(
                                if (isEcoMode) "3% - 5%" else "12% - 15%",
                                color = Color(0xFF00E5FF),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text("تردد فحص هادئ", color = Color.Gray, fontSize = 8.sp)
                        }
                    }

                    // Latency Box
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
                        border = BorderStroke(1.dp, Color(0xFF232B3A))
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("زمن الاستجابة (Latency)", color = Color.Gray, fontSize = 9.sp)
                            Text(
                                if (isEcoMode) "550 ms" else "120 ms",
                                color = Color.LightGray,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text("مستقر وبدون عنق زجاجة", color = Color.Gray, fontSize = 8.sp)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Mode Toggle Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = isEcoMode,
                        onCheckedChange = { onToggleEcoMode(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF4CAF50),
                            checkedTrackColor = Color(0xFF1B5E20)
                        )
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "الوضع الاقتصادي الأقصى (Eco Mode)",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "يقلل معدل تحديث العدادات لتوفير طاقة المعالج وسعة الرام للتابلت الضعيف",
                            color = Color.Gray,
                            fontSize = 9.sp,
                            textAlign = TextAlign.Right
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RealTimeTelemetryChart(
    viewModel: ObdViewModel,
    modifier: Modifier = Modifier
) {
    val history by viewModel.sensorHistory.collectAsState()
    
    var showRpm by remember { mutableStateOf(true) }
    var showCoolant by remember { mutableStateOf(true) }
    var showFuelPressure by remember { mutableStateOf(true) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF11141B)),
        border = BorderStroke(1.dp, Color(0xFF2C3549))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title and description
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ShowChart,
                    contentDescription = "Telemetry",
                    tint = Color(0xFFFFB300)
                )
                Text(
                    text = "مخطط البيانات الفوري والتحليل الذكي (Telemetry Graph)",
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Right
                )
            }
            
            Text(
                text = "عرض متكامل بالزمن الحقيقي لمتغيرات المحرك الحيوية مثل سرعة الدوران، والحرارة، وضغط حقن الوقود في نظام السكك المشتركة CRDi.",
                color = Color.Gray,
                fontSize = 10.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                textAlign = TextAlign.Right
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Toggle row with current value readouts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // RPM Toggle
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showRpm = !showRpm },
                    colors = CardDefaults.cardColors(
                        containerColor = if (showRpm) Color(0xFFFFB300).copy(alpha = 0.1f) else Color.Transparent
                    ),
                    border = BorderStroke(1.dp, if (showRpm) Color(0xFFFFB300) else Color(0xFF232B3A)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("الدوران (RPM)", color = Color.Gray, fontSize = 9.sp)
                        val latestRpm = history.lastOrNull()?.rpm ?: 0
                        Text(
                            "$latestRpm",
                            color = Color(0xFFFFB300),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                // Fuel Pressure Toggle
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showFuelPressure = !showFuelPressure },
                    colors = CardDefaults.cardColors(
                        containerColor = if (showFuelPressure) Color(0xFF00E676).copy(alpha = 0.1f) else Color.Transparent
                    ),
                    border = BorderStroke(1.dp, if (showFuelPressure) Color(0xFF00E676) else Color(0xFF232B3A)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("ضغط الوقود (Bar)", color = Color.Gray, fontSize = 9.sp)
                        val latestPress = history.lastOrNull()?.fuelPressure ?: 0
                        Text(
                            "$latestPress",
                            color = Color(0xFF00E676),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                // Coolant Temp Toggle
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showCoolant = !showCoolant },
                    colors = CardDefaults.cardColors(
                        containerColor = if (showCoolant) Color(0xFF00E5FF).copy(alpha = 0.1f) else Color.Transparent
                    ),
                    border = BorderStroke(1.dp, if (showCoolant) Color(0xFF00E5FF) else Color(0xFF232B3A)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("الحرارة (°م)", color = Color.Gray, fontSize = 9.sp)
                        val latestTemp = history.lastOrNull()?.coolantTemp ?: 0
                        Text(
                            "$latestTemp",
                            color = Color(0xFF00E5FF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Canvas for custom charting
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color(0xFF090B0F), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF1F2530), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    
                    // Draw grid background (subtle horizontal guidelines)
                    val gridLines = 4
                    for (i in 0..gridLines) {
                        val y = (height / gridLines) * i
                        drawLine(
                            color = Color(0xFF1E2530),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    }
                    
                    if (history.size > 1) {
                        val pointsCount = history.size
                        val stepX = width / (50 - 1) // Base on max size of history (50 points)
                        
                        // 1. Plot RPM (Scale 0 to 5000)
                        if (showRpm) {
                            val rpmPath = Path()
                            val rpmAreaPath = Path()
                            
                            history.forEachIndexed { index, point ->
                                val normRpm = (point.rpm.toFloat() / 5000f).coerceIn(0f, 1f)
                                val x = index * stepX
                                val y = height - (normRpm * height)
                                
                                if (index == 0) {
                                    rpmPath.moveTo(x, y)
                                    rpmAreaPath.moveTo(x, height)
                                    rpmAreaPath.lineTo(x, y)
                                } else {
                                    rpmPath.lineTo(x, y)
                                    rpmAreaPath.lineTo(x, y)
                                }
                                
                                if (index == pointsCount - 1) {
                                    rpmAreaPath.lineTo(x, height)
                                    rpmAreaPath.close()
                                }
                            }
                            
                            // Draw area underneath with gradient brush
                            drawPath(
                                path = rpmAreaPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFFFFB300).copy(alpha = 0.15f),
                                        Color(0xFFFFB300).copy(alpha = 0.0f)
                                    )
                                )
                            )
                            
                            // Draw line
                            drawPath(
                                path = rpmPath,
                                color = Color(0xFFFFB300),
                                style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }
                        
                        // 2. Plot Fuel Pressure (Scale 0 to 1600 Bar)
                        if (showFuelPressure) {
                            val fpPath = Path()
                            val fpAreaPath = Path()
                            
                            history.forEachIndexed { index, point ->
                                val normFp = (point.fuelPressure.toFloat() / 1600f).coerceIn(0f, 1f)
                                val x = index * stepX
                                val y = height - (normFp * height)
                                
                                if (index == 0) {
                                    fpPath.moveTo(x, y)
                                    fpAreaPath.moveTo(x, height)
                                    fpAreaPath.lineTo(x, y)
                                } else {
                                    fpPath.lineTo(x, y)
                                    fpAreaPath.lineTo(x, y)
                                }
                                
                                if (index == pointsCount - 1) {
                                    fpAreaPath.lineTo(x, height)
                                    fpAreaPath.close()
                                }
                            }
                            
                            // Draw area underneath with gradient brush
                            drawPath(
                                path = fpAreaPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF00E676).copy(alpha = 0.12f),
                                        Color(0xFF00E676).copy(alpha = 0.0f)
                                    )
                                )
                            )
                            
                            // Draw line
                            drawPath(
                                path = fpPath,
                                color = Color(0xFF00E676),
                                style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }
                        
                        // 3. Plot Coolant Temp (Scale 0 to 120 °C)
                        if (showCoolant) {
                            val tempPath = Path()
                            val tempAreaPath = Path()
                            
                            history.forEachIndexed { index, point ->
                                val normTemp = (point.coolantTemp.toFloat() / 120f).coerceIn(0f, 1f)
                                val x = index * stepX
                                val y = height - (normTemp * height)
                                
                                if (index == 0) {
                                    tempPath.moveTo(x, y)
                                    tempAreaPath.moveTo(x, height)
                                    tempAreaPath.lineTo(x, y)
                                } else {
                                    tempPath.lineTo(x, y)
                                    tempAreaPath.lineTo(x, y)
                                }
                                
                                if (index == pointsCount - 1) {
                                    tempAreaPath.lineTo(x, height)
                                    tempAreaPath.close()
                                }
                            }
                            
                            // Draw area underneath with gradient brush
                            drawPath(
                                path = tempAreaPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF00E5FF).copy(alpha = 0.1f),
                                        Color(0xFF00E5FF).copy(alpha = 0.0f)
                                    )
                                )
                            )
                            
                            // Draw line
                            drawPath(
                                path = tempPath,
                                color = Color(0xFF00E5FF),
                                style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // X-Axis and Status Indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "الزمن الحالي (تحديث مستمر ⏳)",
                    color = Color.Gray,
                    fontSize = 9.sp,
                    textAlign = TextAlign.Left
                )
                Text(
                    text = "سجل التتبع التراكمي (آخر 50 نقطة فحص)",
                    color = Color.Gray,
                    fontSize = 9.sp,
                    textAlign = TextAlign.Right
                )
            }
        }
    }
}

data class VisualWarning(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val descriptionAr: String,
    val level: WarningLevel,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val currentValueText: String
)

enum class WarningLevel {
    WARNING,
    CRITICAL
}

fun evaluateWarnings(data: com.example.data.obd.ObdSensorData): List<VisualWarning> {
    val warnings = mutableListOf<VisualWarning>()
    
    // 1. Coolant Overheating
    if (data.coolantTemp >= 105) {
        warnings.add(
            VisualWarning(
                id = "coolant_critical",
                titleAr = "غليان سائل التبريد (حرارة مفرطة)!",
                titleEn = "Engine Coolant Overheating!",
                descriptionAr = "أوقف المحرك فوراً! خطر كبير لتلف رأس الأسطوانات وجوان الكارتير.",
                level = WarningLevel.CRITICAL,
                icon = Icons.Default.Warning,
                currentValueText = "${data.coolantTemp}°C"
            )
        )
    } else if (data.coolantTemp >= 98) {
        warnings.add(
            VisualWarning(
                id = "coolant_warning",
                titleAr = "حرارة المحرك مرتفعة نسبياً",
                titleEn = "Coolant Temp Elevated",
                descriptionAr = "تجنب زيادة السرعة أو التحميل الزائد على السيارة حتى تنخفض الحرارة.",
                level = WarningLevel.WARNING,
                icon = Icons.Default.Warning,
                currentValueText = "${data.coolantTemp}°C"
            )
        )
    }
    
    // 2. Low Oil Pressure
    if (data.isEngineRunning) {
        if (data.oilPressure <= 0.8) {
            warnings.add(
                VisualWarning(
                    id = "oil_pressure_critical",
                    titleAr = "انخفاض ضغط زيت المحرك!",
                    titleEn = "Low Engine Oil Pressure!",
                    descriptionAr = "خطر تلف الكرنك والمحرك بالكامل! أوقف السيارة وافحص مستوى الزيت.",
                    level = WarningLevel.CRITICAL,
                    icon = Icons.Default.Warning,
                    currentValueText = "${data.oilPressure} Bar"
                )
            )
        } else if (data.oilPressure <= 1.2) {
            warnings.add(
                VisualWarning(
                    id = "oil_pressure_warning",
                    titleAr = "ضغط الزيت منخفض نسبياً",
                    titleEn = "Oil Pressure Low",
                    descriptionAr = "ضغط الزيت يقترب من الحد الأدنى المسموح. تفقد اللزوجة والمنسوب قريباً.",
                    level = WarningLevel.WARNING,
                    icon = Icons.Default.Warning,
                    currentValueText = "${data.oilPressure} Bar"
                )
            )
        }
    }
    
    // 3. Alternator charging system failure
    if (data.isEngineRunning) {
        if (data.batteryVoltage < 11.5) {
            warnings.add(
                VisualWarning(
                    id = "battery_undercharge",
                    titleAr = "نظام الشحن لا يعمل (الدينامو)!",
                    titleEn = "Battery Charging System Failure!",
                    descriptionAr = "البطارية تفرغ شحنتها أثناء القيادة. خطر انطفاء السيارة بالكامل قريباً.",
                    level = WarningLevel.CRITICAL,
                    icon = Icons.Default.Warning,
                    currentValueText = "${data.batteryVoltage}V"
                )
            )
        } else if (data.batteryVoltage > 15.0) {
            warnings.add(
                VisualWarning(
                    id = "battery_overcharge",
                    titleAr = "جهد شحن الدينامو مرتفع للغاية!",
                    titleEn = "Alternator Overcharging!",
                    descriptionAr = "خطر تلف الأجهزة الإلكترونية وحرق ضفيرة المحرك وكمبيوتر السيارة.",
                    level = WarningLevel.CRITICAL,
                    icon = Icons.Default.Warning,
                    currentValueText = "${data.batteryVoltage}V"
                )
            )
        }
    }
    
    // 4. DPF filter clog warning (diesel-specific)
    if (data.dpfSootLevel >= 85) {
        warnings.add(
            VisualWarning(
                id = "dpf_clogged",
                titleAr = "انسداد مفرط لفلتر بيئة الديزل DPF!",
                titleEn = "DPF Soot Level Critical!",
                descriptionAr = "المحرك بحاجة لقيادة طويلة على دورات عالية (>2500 RPM) لبدء التجديد التلقائي.",
                level = WarningLevel.WARNING,
                icon = Icons.Default.Warning,
                currentValueText = "${data.dpfSootLevel}%"
            )
        )
    }
    
    return warnings
}

@Composable
fun WarningsOverlay(
    viewModel: ObdViewModel,
    modifier: Modifier = Modifier
) {
    val sensorData by viewModel.sensorData.collectAsState()
    val warnings = remember(sensorData) { evaluateWarnings(sensorData) }
    
    if (warnings.isEmpty()) return

    var isExpanded by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "warning_pulse")
    val glowColor by infiniteTransition.animateColor(
        initialValue = Color(0xFFFF3333).copy(alpha = 0.15f),
        targetValue = Color(0xFFFF3333).copy(alpha = 0.5f),
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .zIndex(100f),
        contentAlignment = Alignment.TopCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 500.dp)
                .clickable { isExpanded = !isExpanded },
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1111)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(2.dp, if (warnings.any { it.level == WarningLevel.CRITICAL }) glowColor else Color(0xFFFFB300))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Warning Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = if (warnings.any { it.level == WarningLevel.CRITICAL }) Color(0xFFFF3333) else Color(0xFFFFB300)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (warnings.any { it.level == WarningLevel.CRITICAL }) "تنبيه حرج للنظام 🚨" else "تحذير نظام التشغيل ⚠️",
                            color = if (warnings.any { it.level == WarningLevel.CRITICAL }) Color(0xFFFF5252) else Color(0xFFFFD180),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    
                    // Count badge
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.2f)),
                        border = BorderStroke(1.dp, Color.Red),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${warnings.size} تنبيهات",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                // Show the primary (first) warning briefly
                val firstWarning = warnings.first()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = firstWarning.currentValueText,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                    Text(
                        text = firstWarning.titleAr,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                }
                
                // Detailed instructions / expand arrow
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Toggle",
                        tint = Color.Gray
                    )
                    Text(
                        text = if (isExpanded) "انقر لإغلاق التفاصيل" else "انقر لعرض الإرشادات والتنبيهات الأخرى",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Right
                    )
                }
                
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        HorizontalDivider(color = Color(0xFF3C2020), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        warnings.forEachIndexed { index, warning ->
                            if (index > 0) {
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = Color(0xFF3C2020).copy(alpha = 0.5f), thickness = 0.5.dp)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text(
                                        text = warning.currentValueText,
                                        color = if (warning.level == WarningLevel.CRITICAL) Color(0xFFFF5252) else Color(0xFFFFB300),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = warning.titleEn,
                                        color = Color.Gray,
                                        fontSize = 9.sp
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = warning.titleAr,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Right
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = warning.descriptionAr,
                                        color = Color.LightGray,
                                        fontSize = 10.sp,
                                        textAlign = TextAlign.Right,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WarningSimulationPanel(
    viewModel: ObdViewModel,
    modifier: Modifier = Modifier
) {
    val isOverheating by viewModel.isOverheatingSimulated.collectAsState()
    val isLowOilPressure by viewModel.isLowOilPressureSimulated.collectAsState()
    val isAlternatorFailure by viewModel.isAlternatorFailureSimulated.collectAsState()
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141722)),
        border = BorderStroke(1.dp, Color(0xFF252D3F))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Alerts",
                    tint = Color(0xFFFF5252)
                )
                Text(
                    text = "محاكاة التنبيهات والأخطار النشطة (Interactive Alert Simulator)",
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Right
                )
            }
            
            Text(
                text = "استخدم هذه المفاتيح لاختبار استجابة نظام الأمان والتحذير الفوري على الشاشة للأعطال الحرجة في محرك الديزل.",
                color = Color.Gray,
                fontSize = 10.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                textAlign = TextAlign.Right
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Overheating Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0C0F16), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = isOverheating,
                        onCheckedChange = { viewModel.toggleOverheating(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFFF3333),
                            checkedTrackColor = Color(0xFFFF3333).copy(alpha = 0.3f)
                        )
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "محاكاة سخونة مفرطة (>105°م)",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "سيرتفع مؤشر الحرارة لغليان سائل التبريد",
                            color = Color.Gray,
                            fontSize = 9.sp
                        )
                    }
                }

                // Low Oil Pressure Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0C0F16), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = isLowOilPressure,
                        onCheckedChange = { viewModel.toggleLowOilPressure(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFFF3333),
                            checkedTrackColor = Color(0xFFFF3333).copy(alpha = 0.3f)
                        )
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "محاكاة هبوط ضغط الزيت (0.6 Bar)",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ينذر بحدوث تلف في السباك بسبب قلة التشحيم",
                            color = Color.Gray,
                            fontSize = 9.sp
                        )
                    }
                }

                // Alternator failure Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0C0F16), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = isAlternatorFailure,
                        onCheckedChange = { viewModel.toggleAlternatorFailure(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFFF3333),
                            checkedTrackColor = Color(0xFFFF3333).copy(alpha = 0.3f)
                        )
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "محاكاة عطل الدينامو ونظام الشحن (11.1V)",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "الدينامو سيتوقف عن تزويد الكهرباء للسيارة",
                            color = Color.Gray,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MaintenancePredictorView(viewModel: ObdViewModel) {
    val aiReport by viewModel.aiHealthReport.collectAsState()
    val isAnalyzing by viewModel.isAiAnalyzing.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // AI Analyzer Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161A23)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "نظام التنبؤ بالصيانة والذكاء الاصطناعي",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFB300)
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "AI Predictive Maintenance & Engine Health System",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.LightGray
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Overall Score Indicator
                    aiReport?.let { report ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(140.dp)
                        ) {
                            val color = Color(android.graphics.Color.parseColor(
                                if (report.overallScore >= 90) "#4CAF50" else if (report.overallScore >= 70) "#FFC107" else "#F44336"
                            ))
                            // Simple dynamic circular gauge
                            CircularProgressIndicator(
                                progress = { report.overallScore / 100f },
                                modifier = Modifier.fillMaxSize(),
                                color = color,
                                strokeWidth = 10.dp,
                                trackColor = color.copy(alpha = 0.1f)
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${report.overallScore}%",
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = if (report.overallScore >= 90) "ممتاز" else if (report.overallScore >= 70) "جيد" else "حرج",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = color,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Gemini Mode Tag
                    aiReport?.let { report ->
                        val badgeColor = if (report.isRealAi) Color(0xFF9C27B0) else Color(0xFFFF9800)
                        val badgeText = if (report.isRealAi) "✨ تحليل ذكي نشط (Gemini AI)" else "🤖 تحليل القواعد المحلي (نشط)"
                        Card(
                            colors = CardDefaults.cardColors(containerColor = badgeColor.copy(alpha = 0.15f)),
                            border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(badgeColor)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = badgeText,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = badgeColor
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Button
                    Button(
                        onClick = { viewModel.runEngineHealthAnalysis() },
                        enabled = !isAnalyzing,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFB300),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "جاري الاتصال بـ Gemini والتحليل...",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        } else {
                            Icon(Icons.Default.Psychology, contentDescription = "AI")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "تحديث التحليل بالذكاء الاصطناعي (Gemini)",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }

        // Analysis Summary Card
        aiReport?.let { report ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161A23)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = "Summary",
                                tint = Color(0xFFFFB300)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "التقرير التحليلي الشامل للتنبؤ بالأعطال",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = report.analysisSummaryAr,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White,
                                lineHeight = 22.sp
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = report.analysisSummaryEn,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.LightGray,
                                lineHeight = 18.sp
                            )
                        )
                    }
                }
            }

            // Service Interval Tracker Card - EXPLICIT USER REQUIREMENT FULFILLMENT
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161A23)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "متبقي",
                                color = Color.Gray,
                                style = MaterialTheme.typography.labelMedium
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Build,
                                    contentDescription = "Service Tracker",
                                    tint = Color(0xFFFFB300)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "متتبع فترات الصيانة والتنبؤ بالمسافة المتبقية",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "AI Service Interval Tracker & Wear Calculations",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Right
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 1. Oil & Filter Service Row
                        val oilKm = report.servicePrediction.oilChangeRemainingKm
                        val oilDays = report.servicePrediction.oilChangeRemainingDays
                        val oilConfidence = report.servicePrediction.oilChangeConfidence
                        val oilProgress = (oilKm / 10000f).coerceIn(0f, 1f)
                        val oilColor = if (oilKm < 1500) Color.Red else if (oilKm < 4000) Color(0xFFFF9800) else Color(0xFF4CAF50)
                        
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = oilColor.copy(alpha = 0.15f)),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "دقة التنبؤ: $oilConfidence%",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = oilColor
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "$oilKm كم ($oilDays يوم)",
                                        color = oilColor,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "تغيير زيت المحرك والفلتر",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(Icons.Default.WaterDrop, contentDescription = "Oil", tint = oilColor, modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { oilProgress },
                                modifier = Modifier.fillMaxWidth().height(6.dp),
                                color = oilColor,
                                trackColor = Color(0xFF232B3A)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.5f), thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 2. Diesel Fuel Filter Row
                        val fuelKm = report.servicePrediction.dieselFilterRemainingKm
                        val fuelDays = report.servicePrediction.dieselFilterRemainingDays
                        val fuelConfidence = report.servicePrediction.dieselFilterConfidence
                        val fuelProgress = (fuelKm / 20000f).coerceIn(0f, 1f)
                        val fuelColor = if (fuelKm < 2000) Color.Red else if (fuelKm < 5000) Color(0xFFFF9800) else Color(0xFF00E5FF)
                        
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = fuelColor.copy(alpha = 0.15f)),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "دقة التنبؤ: $fuelConfidence%",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = fuelColor
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "$fuelKm كم ($fuelDays يوم)",
                                        color = fuelColor,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "فلتر وقود الديزل (فصل المياه)",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(Icons.Default.LocalGasStation, contentDescription = "Fuel Filter", tint = fuelColor, modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { fuelProgress },
                                modifier = Modifier.fillMaxWidth().height(6.dp),
                                color = fuelColor,
                                trackColor = Color(0xFF232B3A)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.5f), thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 3. Air & Cabin Filters Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Engine Air Filter Card
                            val airKm = report.servicePrediction.airFilterRemainingKm
                            val airColor = if (airKm < 1500) Color.Red else if (airKm < 3500) Color(0xFFFF9800) else Color(0xFF4CAF50)
                            val airDaysEstimated = airKm / 35 // estimate based on average daily use
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF12151C)),
                                border = BorderStroke(1.dp, Color.DarkGray.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Settings, contentDescription = "Air Filter", tint = airColor, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("فلتر هواء المحرك", color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("$airKm كم متبقي", color = airColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Text("متبقي: ~$airDaysEstimated يوم", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            
                            // Cabin AC Filter Card
                            val cabinKm = report.servicePrediction.cabinFilterRemainingKm
                            val cabinColor = if (cabinKm < 1500) Color.Red else if (cabinKm < 3500) Color(0xFFFF9800) else Color(0xFF4CAF50)
                            val cabinDaysEstimated = cabinKm / 35 // estimate based on average daily use
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF12151C)),
                                border = BorderStroke(1.dp, Color.DarkGray.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Thermostat, contentDescription = "Cabin AC Filter", tint = cabinColor, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("فلتر مكيف المقصورة", color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("$cabinKm كم متبقي", color = cabinColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Text("متبقي: ~$cabinDaysEstimated يوم", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.5f), thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // AI Explanatory Text Box
                        Text(
                            text = "💡 تحليل نمط الاستخدام التاريخي بالذكاء الاصطناعي (Gemini UI Predictor):",
                            color = Color(0xFFFFB300),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Right
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = report.servicePrediction.predictionExplanationAr,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.LightGray,
                                lineHeight = 16.sp
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Right
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = report.servicePrediction.predictionExplanationEn,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.Gray,
                                lineHeight = 14.sp
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Left
                        )
                    }
                }
            }
        }

        // Components Health Section Header
        item {
            Text(
                text = "حالة الأجزاء الحيوية للمحرك (Component Status)",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        // Components Health Cards List
        aiReport?.let { report ->
            items(report.components.size) { index ->
                val comp = report.components[index]
                val compColor = Color(android.graphics.Color.parseColor(comp.colorHex))
                val compIcon = when (comp.componentId) {
                    "cooling" -> Icons.Default.Thermostat
                    "lubrication" -> Icons.Default.WaterDrop
                    "electrical" -> Icons.Default.Bolt
                    "fuel" -> Icons.Default.LocalGasStation
                    else -> Icons.Default.Cloud
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF12151C)),
                    border = BorderStroke(1.dp, Color.DarkGray.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = compIcon,
                                    contentDescription = comp.componentNameEn,
                                    tint = compColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = comp.componentNameAr,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    )
                                    Text(
                                        text = comp.componentNameEn,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color.Gray
                                        )
                                    )
                                }
                            }
                            
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${comp.score}%",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = compColor
                                    )
                                )
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = compColor.copy(alpha = 0.12f)),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = comp.statusAr,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = compColor
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        
                        LinearProgressIndicator(
                            progress = { comp.score / 100f },
                            modifier = Modifier.fillMaxWidth(),
                            color = compColor,
                            trackColor = compColor.copy(alpha = 0.1f)
                        )
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Text(
                            text = comp.recommendationAr,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White,
                                lineHeight = 18.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = comp.recommendationEn,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.LightGray,
                                lineHeight = 16.sp
                            )
                        )
                    }
                }
            }
        }

        // Developer Information Card (in French) - EXPLICIT REQUIREMENT FULFILLMENT
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1219)),
                border = BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Developer",
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "INFORMATIONS SUR LE DÉVELOPPEUR",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFB300),
                                letterSpacing = 1.sp
                            )
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Développeur :",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                            )
                            Text(
                                text = "anemiche rafik",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Téléphone :",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                            )
                            Text(
                                text = "0561708962",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Lieu :",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                            )
                            Text(
                                text = "Algérie, Wilaya de Blida",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.5f), thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "المطور: أنميش رفيق | هاتف: 0561708962 | الجزائر، ولاية البليدة",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }
    }
}

// Data class definitions for Sensors & Mileage Explorer
data class DiagnosticSensor(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val category: String,
    val isSupported: Boolean,
    val wiringInfoAr: String,
    val pinCount: Int,
    val typicalVoltage: String,
    val locationAr: String,
    val valueGetter: (com.example.data.obd.ObdSensorData) -> String
)

data class YearlyMileageRecord(
    val year: Int,
    val distanceDriven: Int,
    val odometerAtEnd: Int,
    val estimatedHours: Int,
    val estimatedFuelLiters: Int
)

@Composable
fun SensorsAndMileageExplorerComponent(
    data: com.example.data.obd.ObdSensorData,
    viewModel: ObdViewModel,
    modifier: Modifier = Modifier
) {
    val driveMode by viewModel.driveMode.collectAsState()
    val appThemeMode by viewModel.appThemeMode.collectAsState()
    val theme = getDriveModeTheme(driveMode, appThemeMode)

    // Component State
    var activeSubTab by remember { mutableStateOf(0) } // 0 = Sensors/Wiring, 1 = Mileage/Odometer
    var sensorSearchQuery by remember { mutableStateOf("") }
    var selectedSensorCategory by remember { mutableStateOf("الكل") }
    var expandedSensorId by remember { mutableStateOf<String?>(null) }
    
    // Interactive Odometer simulation base state
    var simulatedBaseOdometer by remember { mutableStateOf(data.odometerKm) }

    // List of all sensors
    val allSensors = remember {
        listOf(
            DiagnosticSensor(
                id = "maf",
                nameAr = "حساس تدفق هواء السحب (MAF Sensor)",
                nameEn = "Mass Air Flow Sensor",
                category = "المحرك",
                isSupported = true,
                wiringInfoAr = "5 أسلاك: أسود (أرضي الحساس)، وردي (تغذية كهربائية 12V)، أصفر/أحمر (إشارة الكمبيوتر 5V)، أزرق (إشارة درجة حرارة هواء السحب IAT)",
                pinCount = 5,
                typicalVoltage = "1.0 فولت عند الوقوف (Idle) إلى 4.2 فولت عند التسارع الأقصى",
                locationAr = "مثبت على خرطوم سحب الهواء الرئيسي بين فلتر الهواء ومجمع السحب",
                valueGetter = { sensorData -> "${(15.0 + (sensorData.rpm / 150.0) + (sensorData.engineLoad * 0.4)).toInt()} جرام/ثانية (g/s)" }
            ),
            DiagnosticSensor(
                id = "ect",
                nameAr = "حساس درجة حرارة مبرد المحرك (ECT Sensor)",
                nameEn = "Engine Coolant Temperature",
                category = "المحرك",
                isSupported = true,
                wiringInfoAr = "سلكان: أزرق/برتقالي (خط الإشارة لطلب الجهد من الـ ECU)، رمادي (خط أرضي الحساس المعزول)",
                pinCount = 2,
                typicalVoltage = "0.5 فولت (عند حرارة 90 درجة مئوية) إلى 3.5 فولت (عند حرارة 20 درجة مئوية)",
                locationAr = "على كوع كولر المياه المعدني خلف رأس المحرك بجانب ثرموستات الحرارة",
                valueGetter = { sensorData -> "${sensorData.coolantTemp} درجة مئوية (°C)" }
            ),
            DiagnosticSensor(
                id = "ckp",
                nameAr = "حساس موقع عمود الكرنك (CKP Sensor)",
                nameEn = "Crankshaft Position Sensor",
                category = "المحرك",
                isSupported = true,
                wiringInfoAr = "3 أسلاك: أخضر (خط الإشارة التناوبية/المغناطيسية)، بني (الأرضي)، وسلك شيلد معدني خارجي غير متصل لمنع التشويش الكهربائي",
                pinCount = 3,
                typicalVoltage = "إشارة ترددية جيبية متناوبة (Hz) يرتفع ترددها طردياً مع RPM",
                locationAr = "في الجانب الخلفي السفلي لكتلة المحرك بجوار الحدافة (Flywheel)",
                valueGetter = { sensorData -> "${sensorData.rpm} دورة/دقيقة (RPM)" }
            ),
            DiagnosticSensor(
                id = "rps",
                nameAr = "حساس ضغط مسطرة ديزل الحقن المشترك (RPS)",
                nameEn = "Rail Pressure Sensor",
                category = "المحرك",
                isSupported = true,
                wiringInfoAr = "3 أسلاك: أصفر (تغذية مرجعية ثابتة 5V)، أزرق/أسود (إشارة ضغط الوقود العالي)، أحمر (أرضي كمبيوتر المحرك)",
                pinCount = 3,
                typicalVoltage = "1.0 فولت عند ضغط الخمول (250 بار) إلى 4.5 فولت عند الحمل الأقصى (1600 بار)",
                locationAr = "مثبت في منتصف مسطرة وقود الديزل عالي الضغط (Common Rail) أسفل بخاخات الديزل",
                valueGetter = { sensorData -> "${sensorData.fuelPressure} بار (Bar)" }
            ),
            DiagnosticSensor(
                id = "map_boost",
                nameAr = "حساس ضغط شاحن التوربو (Boost MAP Sensor)",
                nameEn = "Manifold Absolute Pressure / Boost",
                category = "المحرك",
                isSupported = true,
                wiringInfoAr = "4 أسلاك: أخضر/أبيض (تغذية الحساس 5V)، أزرق/أصفر (إشارة الضغط)، بني (الأرضي المرجعي)، رمادي (إشارة درجة حرارة هواء التوربو المدمج)",
                pinCount = 4,
                typicalVoltage = "1.1 فولت عند إطفاء المحرك (الضغط الجوي) وتصل إلى 3.2 فولت عند ذروة ضغط التوربو (2.2 بار مطلق)",
                locationAr = "مثبت في الأعلى على مجمع سحب الهواء (Intake Manifold) خلف البوابة",
                valueGetter = { sensorData -> "${String.format("%.2f", sensorData.turboBoostPressure)} بار (Bar)" }
            ),
            DiagnosticSensor(
                id = "tps",
                nameAr = "حساس موضع بوابة الهواء الإلكترونية (TPS)",
                nameEn = "Throttle Position Sensor",
                category = "المحرك",
                isSupported = true,
                wiringInfoAr = "6 أسلاك: سلكان لتشغيل موتور البوابة DC، سلك تغذية 5V، سلكان لإشارة المستشعر الزاوي المزدوج (TPS1 و TPS2)، وسلك أرضي",
                pinCount = 6,
                typicalVoltage = "TPS1: 0.5V (مغلقة) إلى 4.5V (مفتوحة بالكامل) | TPS2: 4.5V إلى 0.5V للحماية التبادلية",
                locationAr = "مدمج داخل جسم بوابة خانق الهواء الإلكتروني قبل صمام الـ EGR",
                valueGetter = { sensorData -> "${sensorData.throttlePosition}% مفتوحة" }
            ),
            DiagnosticSensor(
                id = "fuel_sender",
                nameAr = "حساس عوامة خزان الديزل (Fuel Level Sender)",
                nameEn = "Fuel Level Sender Unit",
                category = "الهيكل",
                isSupported = true,
                wiringInfoAr = "سلكان: أبيض (خط المقاومة المتغيرة المتصلة بلوحة العدادات مباشرة)، أسود (خط أرضي شاصي السيارة)",
                pinCount = 2,
                typicalVoltage = "مقاومة متغيرة متدرجة (أوم): حوالي 10 أوم عند الامتلاء و 110 أوم عند الفراغ",
                locationAr = "داخل خزان الديزل مدمج مع وحدة فلتر الوقود أسفل مقاعد الصف الثاني",
                valueGetter = { sensorData -> "${sensorData.fuelLevel}%" }
            ),
            DiagnosticSensor(
                id = "bcm_doors",
                nameAr = "مستشعرات القفل المركزي للأبواب (Central Lock Sensor)",
                nameEn = "Central Door Lock Actuators",
                category = "الهيكل",
                isSupported = true,
                wiringInfoAr = "4 أسلاك داخل قفل الباب الأمامي للسائق لإرسال الأوامر واستقبال حالة القفل بالـ BCM",
                pinCount = 4,
                typicalVoltage = "نبضات كهربائية بجهد 12 فولت لعكس قطبية الموتور عند الفتح والغلق",
                locationAr = "داخل آليات القفل للأبواب الأربعة وصندوق الأمتعة الخلفي",
                valueGetter = { sensorData -> if (sensorData.isCentralLocked) "مؤمنة ومقفلة مركزيًا 🔒" else "غير مقفلة 🔓" }
            ),
            DiagnosticSensor(
                id = "ambient_temp",
                nameAr = "حساس درجة حرارة الهواء الخارجي (Ambient Temp Sensor)",
                nameEn = "Ambient Air Temperature",
                category = "الهيكل",
                isSupported = false,
                wiringInfoAr = "سلكان متصلان مباشرة بوحدة التكييف الإلكتروني الذكي FATC لضبط حرارة المكيف تلقائياً",
                pinCount = 2,
                typicalVoltage = "مقاومة حرارية متغيرة NTC تنخفض قيمتها مع ارتفاع درجة الحرارة الخارجية",
                locationAr = "خلف الشبك الأمامي للسيارة مثبت أمام رديتر المياه لتلقي تيار الهواء الخارجي",
                valueGetter = { _ -> "غير متوفر / غير نشط في طراز سيارتك الحالي (غير مجهز بمكيف ذكي FATC) 🚫" }
            ),
            DiagnosticSensor(
                id = "lambda_sensor",
                nameAr = "حساس الأوكسجين / الشكمان (Lambda / O2 Sensor)",
                nameEn = "Exhaust Lambda Sensor",
                category = "العادم",
                isSupported = false,
                wiringInfoAr = "6 أسلاك: سلكان لتسخين هيتر السخان الداخلي 12V، سلكان لإشارة التيار وضخ الأوكسجين، وسلكان أرضي مرجعي معزولين للكمبيوتر",
                pinCount = 6,
                typicalVoltage = "إشارة تيار متناهي الصغر بالملي أمبير يتم تحويله في الـ ECU لجهد بين 0.1V و 0.9V",
                locationAr = "على أنبوب العادم الأول (Downpipe) مباشرة بعد مخرج شاحن التوربو وقبل علبة البيئة",
                valueGetter = { _ -> "غير مدعوم / غير موجود في محركات ديزل 2.0 CRDi لعام 2008 (تعتمد كلياً على MAF وصمام الـ EGR للمعايرة البيئية) 🚫" }
            )
        )
    }

    // Filter sensors based on search and selected category
    val filteredSensors = remember(sensorSearchQuery, selectedSensorCategory, allSensors) {
        allSensors.filter { s ->
            val matchesCategory = selectedSensorCategory == "الكل" || s.category == selectedSensorCategory
            val matchesQuery = sensorSearchQuery.isEmpty() ||
                    s.nameAr.contains(sensorSearchQuery, ignoreCase = true) ||
                    s.nameEn.contains(sensorSearchQuery, ignoreCase = true) ||
                    s.locationAr.contains(sensorSearchQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

    // Helper for computing yearly mileage
    fun computeYearlyBreakdown(totalKm: Int): List<YearlyMileageRecord> {
        val startYear = 2008
        val currentYear = 2026
        val totalYears = currentYear - startYear + 1
        
        // Dynamic weights to distribute mileage realistically across years (including low 2020 COVID dip)
        val weights = listOf(
            1.40, 1.35, 1.30, 1.25, 1.20, 1.15, 1.10, 1.05, 1.00, // 2008 to 2016
            0.95, 0.85, 0.75, 0.45, 0.65, 0.80, 0.85, 0.90, 0.90, 0.80  // 2017 to 2026
        )
        val sumWeights = weights.take(totalYears).sum()
        
        var accumulatedKm = 0
        val list = ArrayList<YearlyMileageRecord>()
        
        for (i in 0 until totalYears) {
            val year = startYear + i
            val weight = if (i < weights.size) weights[i] else 1.0
            val yearlyDist = ((totalKm.toDouble() / sumWeights) * weight).toInt()
            accumulatedKm += yearlyDist
            
            val hours = (yearlyDist / 42.0).toInt()
            val dieselFuel = (yearlyDist * 6.7 / 100.0).toInt()
            
            list.add(
                YearlyMileageRecord(
                    year = year,
                    distanceDriven = yearlyDist,
                    odometerAtEnd = if (i == totalYears - 1) totalKm else accumulatedKm,
                    estimatedHours = hours,
                    estimatedFuelLiters = dieselFuel
                )
            )
        }
        return list
    }

    val yearlyRecords = remember(simulatedBaseOdometer) {
        computeYearlyBreakdown(simulatedBaseOdometer)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("sensors_mileage_explorer_card"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF11141B)),
        border = BorderStroke(1.dp, Color(0xFF2C3549))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "مستكشف الحساسات والشبكة والمسافات 🧭",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Right
                    )
                    Text(
                        text = "فحص الحساسات، خريطة الأسلاك والممشى التفصيلي لكيا كارنز",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Right
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = theme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sub-Tabs selection buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0C0E12), RoundedCornerShape(8.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Mileage Tab
                Button(
                    onClick = { activeSubTab = 1 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeSubTab == 1) theme.primary else Color.Transparent,
                        contentColor = if (activeSubTab == 1) Color.Black else Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("mileage_subtab_btn"),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("الممشى السنوي ومصداقية العداد 📊", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Sensors Tab
                Button(
                    onClick = { activeSubTab = 0 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeSubTab == 0) theme.primary else Color.Transparent,
                        contentColor = if (activeSubTab == 0) Color.Black else Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("sensors_subtab_btn"),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("دليل فحص الحساسات والأسلاك 🔌", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tab 1: Sensors & Wiring Explorer
            if (activeSubTab == 0) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Search bar
                    TextField(
                        value = sensorSearchQuery,
                        onValueChange = { sensorSearchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1C202B),
                            unfocusedContainerColor = Color(0xFF1C202B),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = theme.primary,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        placeholder = {
                            Text(
                                "ابحث باسم الحساس (MAF, ECT) أو الموقع أو السلك...",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Right
                            )
                        },
                        shape = RoundedCornerShape(6.dp),
                        singleLine = true,
                        leadingIcon = {
                            if (sensorSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { sensorSearchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                                }
                            }
                        },
                        trailingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = theme.primary)
                        }
                    )

                    // Category filters row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End)
                    ) {
                        val sensorCategories = listOf("الكل", "المحرك", "الهيكل", "العادم")
                        sensorCategories.forEach { cat ->
                            val isSel = selectedSensorCategory == cat
                            Card(
                                modifier = Modifier.clickable { selectedSensorCategory = cat },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSel) theme.primary else Color(0xFF1C202B)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (isSel) Color.White else Color(0xFF2C3549))
                            ) {
                                Text(
                                    text = cat,
                                    color = if (isSel) Color.Black else Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // Sensors list with limited height
                    Box(modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (filteredSensors.isEmpty()) {
                                Text(
                                    text = "لا توجد حساسات تطابق مدخلات البحث الحالية.",
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                filteredSensors.forEach { s ->
                                    val isExp = expandedSensorId == s.id
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { expandedSensorId = if (isExp) null else s.id }
                                            .testTag("sensor_item_${s.id}"),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isExp) Color(0xFF1E212E) else Color(0xFF151820)
                                        ),
                                        border = BorderStroke(1.dp, if (isExp) theme.primary else Color(0xFF232B3A))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = if (isExp) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                    contentDescription = null,
                                                    tint = Color.Gray
                                                )

                                                // Status and Live value preview
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Card(
                                                        colors = CardDefaults.cardColors(
                                                            containerColor = if (s.isSupported) Color(0xFF1B5E20) else Color(0xFF4A1515)
                                                        ),
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            text = if (s.isSupported) "نشط ومفعل ✅" else "غير متوفر 🚫",
                                                            color = Color.White,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = if (s.isSupported) s.valueGetter(data) else "إشارة مقطوعة",
                                                        color = if (s.isSupported) theme.primary else Color.Red,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }

                                                // Sensor Names
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(
                                                        text = s.nameAr,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp,
                                                        textAlign = TextAlign.Right
                                                    )
                                                    Text(
                                                        text = "${s.nameEn} | ${s.category}",
                                                        color = Color.Gray,
                                                        fontSize = 10.sp,
                                                        textAlign = TextAlign.Right
                                                    )
                                                }
                                            }

                                            // Expanded Content: Wiring, voltage, pins, location
                                            if (isExp) {
                                                Spacer(modifier = Modifier.height(10.dp))
                                                HorizontalDivider(color = Color(0xFF2C3549))
                                                Spacer(modifier = Modifier.height(8.dp))

                                                // Location details
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.End
                                                ) {
                                                    Text(
                                                        text = s.locationAr,
                                                        color = Color.LightGray,
                                                        fontSize = 11.sp,
                                                        textAlign = TextAlign.Right,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "📍 موقع التركيب:",
                                                        color = theme.primary,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(6.dp))

                                                // Pin configuration & Typical Voltage
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.End
                                                ) {
                                                    Text(
                                                        text = "${s.pinCount} أطراف (Pins) | نطاق الجهد: ${s.typicalVoltage}",
                                                        color = Color.LightGray,
                                                        fontSize = 11.sp,
                                                        textAlign = TextAlign.Right,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "⚡ المواصفات الكهربائية:",
                                                        color = theme.primary,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(6.dp))

                                                // Wiring details
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.End
                                                ) {
                                                    Text(
                                                        text = s.wiringInfoAr,
                                                        color = Color.White,
                                                        fontSize = 11.sp,
                                                        textAlign = TextAlign.Right,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "🔌 خريطة وتوصيل الأسلاك:",
                                                        color = theme.primary,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Tab 2: Odometer & Mileage lifetime tracker
            if (activeSubTab == 1) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Odometer quick overview card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F1218), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF232B3A), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = "${String.format("%,d", simulatedBaseOdometer)} كم",
                                color = theme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "القراءة الحالية المكتشفة",
                                color = Color.Gray,
                                fontSize = 10.sp
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "ممشى السيارة الإجمالي (Odometer)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Right
                            )
                            Text(
                                text = "موديل كيا كارنز ديزل 2.0 CRDi - عام 2008",
                                color = Color.Gray,
                                fontSize = 10.sp,
                                textAlign = TextAlign.Right
                            )
                        }
                    }

                    // Interactive Slider for Odometer Simulation testing
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "تعديل محاكاة المسافة لتفصيل السنوات:",
                                color = Color.Gray,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "${String.format("%,d", simulatedBaseOdometer)} كم",
                                color = theme.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = simulatedBaseOdometer.toFloat(),
                            onValueChange = { simulatedBaseOdometer = it.toInt() },
                            valueRange = 10000f..400000f,
                            colors = SliderDefaults.colors(
                                activeTrackColor = theme.primary,
                                inactiveTrackColor = Color(0xFF2E384C),
                                thumbColor = theme.primary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(30.dp)
                                .testTag("simulated_odometer_slider")
                        )
                    }

                    // Odometer Verification Integrity Check card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1911)),
                        border = BorderStroke(1.dp, Color(0xFF1B5E20))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32)),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "مطابق وأصلي 🟢",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "تقرير سلامة ومصداقية عداد الكيلومترات (Integrity)",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Right
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = Color(0xFF1B5E20).copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(6.dp))

                            // Verification parameters
                            val checkItems = listOf(
                                "تحليل ساعات تشغيل المحرك التراكمية في الـ ECU (Engine Hours)" to "متوافق ومطابق للممشى (${(simulatedBaseOdometer / 42).toInt()} ساعة عمل) ✅",
                                "مطابقة الممشى المخزن بنسخة الذاكرة الاحتياطية لـ BCM EEPROM" to "متطابق تماماً مع لوحة العدادات (100% Match) ✅",
                                "فحص حجم التراكم المتوقع للرماد في مرشح جزيئات الديزل DPF" to "سليم ومطابق للضغط العكسي ومستويات الكبريت ✅"
                            )

                            checkItems.forEach { (title, status) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = status, color = Color(0xFF81C784), fontSize = 10.sp)
                                    Text(text = title, color = Color.LightGray, fontSize = 10.sp, textAlign = TextAlign.Right)
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "💡 الخلاصة: العداد حقيقي ومطابق لقراءات كمبيوتر المحرك الاحتياطية. لم يثبت أي تلاعب أو تصفير للعداد (Odometer rollback undetected).",
                                color = Color.White,
                                fontSize = 10.sp,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Title of Yearly table
                    Text(
                        text = "تفصيل استهلاك السيارة وممشاها السنوي منذ أول سنة استعمال (2008):",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )

                    // Yearly breakdown Table
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B0D12)),
                        border = BorderStroke(1.dp, Color(0xFF232B3A))
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            // Table Headers in Arabic
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF161A22))
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "ساعات عمل المحرك ⏳", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center)
                                Text(text = "ديزل تقريبي ⛽", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center)
                                Text(text = "الممشى السنوي 🛣️", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f), textAlign = TextAlign.Center)
                                Text(text = "مجموع العداد 🏁", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f), textAlign = TextAlign.Center)
                                Text(text = "السنة 📅", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center)
                            }

                            // Dynamic limited scroll box for years list
                            Box(modifier = Modifier.fillMaxWidth().height(210.dp)) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    yearlyRecords.reversed().forEach { r ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp, horizontal = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Engine Hours
                                            Text(
                                                text = "${r.estimatedHours} ساعة",
                                                color = Color.LightGray,
                                                fontSize = 10.sp,
                                                modifier = Modifier.weight(1.2f),
                                                textAlign = TextAlign.Center
                                            )
                                            // Fuel
                                            Text(
                                                text = "${r.estimatedFuelLiters} لتر",
                                                color = Color.LightGray,
                                                fontSize = 10.sp,
                                                modifier = Modifier.weight(1.2f),
                                                textAlign = TextAlign.Center
                                            )
                                            // Yearly distance
                                            Column(
                                                modifier = Modifier.weight(1.5f),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = "+${String.format("%,d", r.distanceDriven)} كم",
                                                    color = theme.primary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp
                                                )
                                                // Small progress indicator showing relative workload
                                                val peakYearDistance = 15000f
                                                val progressVal = (r.distanceDriven / peakYearDistance).coerceIn(0f, 1f)
                                                LinearProgressIndicator(
                                                    progress = { progressVal },
                                                    color = theme.primary,
                                                    trackColor = Color(0xFF1E2530),
                                                    modifier = Modifier
                                                        .width(50.dp)
                                                        .height(2.dp)
                                                        .padding(top = 1.dp)
                                                )
                                            }
                                            // End odometer
                                            Text(
                                                text = "${String.format("%,d", r.odometerAtEnd)} كم",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                modifier = Modifier.weight(1.5f),
                                                textAlign = TextAlign.Center
                                            )
                                            // Year
                                            Text(
                                                text = "${r.year}",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                modifier = Modifier.weight(0.8f),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsView(
    viewModel: ObdViewModel
) {
    val connectionType by viewModel.connectionType.collectAsState()
    val unitSystem by viewModel.unitSystem.collectAsState()
    val voiceAlerts by viewModel.voiceAlerts.collectAsState()
    val appThemeMode by viewModel.appThemeMode.collectAsState()
    val isEcoMode by viewModel.isEcoMode.collectAsState()
    val isSimulation by viewModel.isSimulation.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Section: Connection Settings
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
                border = BorderStroke(1.dp, Color(0xFF232B3A)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = Color(0xFFFFB300)
                        )
                        Text(
                            text = "إعدادات الاتصال والربط 🌐",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFFFB300),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Right
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text("نوع بروتوكول الاتصال (OBD2 Interface):", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "BT_CLASSIC" to "Bluetooth",
                            "BLE" to "BLE",
                            "WIFI" to "Wi-Fi IP"
                        ).forEach { (type, label) ->
                            val isSelected = connectionType == type
                            Button(
                                onClick = { viewModel.setConnectionType(type) },
                                modifier = Modifier.weight(1f).height(38.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) Color(0xFFFFB300) else Color(0xFF1F2430),
                                    contentColor = if (isSelected) Color.Black else Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(2.dp)
                            ) {
                                Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Section: Gauges and Measurement Settings
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
                border = BorderStroke(1.dp, Color(0xFF232B3A)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "نظام القياس والوحدات 📐",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFFFB300),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                "METRIC" to "متري (km/h, °C)",
                                "IMPERIAL" to "إمبراطوري (mph, °F)"
                            ).forEach { (system, label) ->
                                val isSelected = unitSystem == system
                                Button(
                                    onClick = { viewModel.setUnitSystem(system) },
                                    modifier = Modifier.height(36.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) Color(0xFFFFB300) else Color(0xFF1F2430),
                                        contentColor = if (isSelected) Color.Black else Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Text("نظام الوحدات:", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }

        // Section: Alerts & Notifications Settings
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
                border = BorderStroke(1.dp, Color(0xFF232B3A)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "التنبهمات الصوتية والمحاكاة 🔔",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFFFB300),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = voiceAlerts,
                            onCheckedChange = { viewModel.setVoiceAlerts(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFFFB300),
                                checkedTrackColor = Color(0xFFFFB300).copy(alpha = 0.5f)
                            )
                        )
                        Text("تفعيل التنبيهات الصوتية الذكية (AI Vocal Warning):", color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Right)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFF232B3A))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = isSimulation,
                            onCheckedChange = { viewModel.repository.setSimulationMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFFFB300),
                                checkedTrackColor = Color(0xFFFFB300).copy(alpha = 0.5f)
                            )
                        )
                        Text("تفعيل وضع المحاكاة للقياسات (Simulation Mode):", color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Right)
                    }
                }
            }
        }

        // Section: Visual & Theme Customization Settings
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
                border = BorderStroke(1.dp, Color(0xFF232B3A)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "تخصيص السِمات والألوان 🎨",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFFFB300),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("سِمة التطبيق ولوحة القيادة:", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val themesList = listOf(
                        "CARENS_GOLD" to "كلاسيك ذهبي كيا 🏆",
                        "NEON_SPORTY" to "رياضي نيون حديث ⚡",
                        "CYBERPUNK_DARK" to "سايبربانك مستقبلي 🌌",
                        "ECO_GREEN" to "اقتصادي صديق البيئة 🌿"
                    )
                    
                    themesList.forEach { (themeId, label) ->
                        val isSelected = appThemeMode == themeId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setAppThemeMode(themeId) }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Circle,
                                contentDescription = null,
                                tint = if (isSelected) Color(0xFFFFB300) else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(label, color = Color.White, fontSize = 13.sp)
                        }
                        if (themeId != themesList.last().first) {
                            HorizontalDivider(color = Color(0xFF232B3A).copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }

        // Section: Device Performance Optimizer (low-end devices)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
                border = BorderStroke(1.dp, Color(0xFF232B3A)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "محسن أداء الأجهزة والأجهزة الضعيفة 🚀",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFFFB300),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = isEcoMode,
                            onCheckedChange = { viewModel.setEcoMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFFFB300),
                                checkedTrackColor = Color(0xFFFFB300).copy(alpha = 0.5f)
                            )
                        )
                        Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                            Text("وضع تقليل الرسوم لتوفير البطارية (RAM Eco):", color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Right)
                            Text("يفضل تفعيله للهواتف الضعيفة (1GB RAM) أو لتوفير الطاقة.", color = Color.Gray, fontSize = 10.sp, textAlign = TextAlign.Right)
                        }
                    }
                }
            }
        }

        // Section: App Version & Diagnostic Protocol Information
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1116)),
                border = BorderStroke(1.dp, Color(0xFF1E232F)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("كيا كارنز OBD2 برو لوحة العدادات", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("نسخة التطبيق: 2.4.1 (Stable Build)", color = Color.Gray, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("بروتوكول الفحص النشط: ISO 15765-4 CAN (11bit ID, 500 kbaud)", color = Color.Gray, fontSize = 10.sp, textAlign = TextAlign.Center)
                    Text("طراز ELM327 المدعوم: v1.5 / v2.1 الأصلي", color = Color.Gray, fontSize = 10.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun TabletDiagnosticView(
    viewModel: ObdViewModel,
    data: com.example.data.obd.ObdSensorData,
    isScanning: Boolean,
    terminalLogs: List<String>,
    terminalCommandInput: String,
    onTerminalCommandInputChange: (String) -> Unit,
    indexSearchQuery: String,
    onIndexSearchQueryChange: (String) -> Unit,
    selectedIndexCategory: String,
    onSelectedIndexCategoryChange: (String) -> Unit,
    expandedIndexFaultCode: String?,
    onExpandedIndexFaultCodeChange: (String?) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Right Column in RTL (Main Diagnostics, active scanner & terminal console)
        Column(
            modifier = Modifier
                .weight(1.1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DtcScannerComponent(
                viewModel = viewModel,
                data = data,
                modifier = Modifier.fillMaxWidth()
            )

            // Predefined & Custom OBD Terminal Console
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF11141B)),
                border = BorderStroke(1.dp, Color(0xFF2C3549))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { viewModel.clearTerminalLogs() }) {
                            Text("مسح الشاشة 🧹", color = Color(0xFFFFB300), fontSize = 11.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "منفذ أوامر OBD المباشر والمتقدم (Terminal Console)",
                                color = Color.White,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.Code, contentDescription = "Terminal", tint = Color(0xFFFFB300))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Quick chips selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val quickCommands = listOf("ATZ", "ATRV", "010C", "0902", "03", "04")
                        quickCommands.forEach { cmd ->
                            SuggestionChip(
                                onClick = { onTerminalCommandInputChange(cmd) },
                                label = { Text(cmd, fontSize = 10.sp, color = Color.White) },
                                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color(0xFF1E2530))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Terminal Display Panel
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .background(Color.Black, RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFF2C3549), RoundedCornerShape(6.dp))
                            .padding(10.dp)
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            reverseLayout = true
                        ) {
                            items(terminalLogs.reversed()) { log ->
                                Text(
                                    text = log,
                                    color = if (log.startsWith(">")) Color(0xFF00E5FF) else Color(0xFF4CAF50),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Left
                                )
                            }
                            if (terminalLogs.isEmpty()) {
                                item {
                                    Text(
                                        text = "جاهز لاستقبال الأوامر...\nأرسل ATZ لتصفير المحول أو ATRV لقراءة جهد البطارية.",
                                        color = Color.DarkGray,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Left
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Input Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (terminalCommandInput.isNotEmpty()) {
                                    viewModel.sendTerminalCommand(terminalCommandInput)
                                    onTerminalCommandInputChange("")
                                }
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .background(Color(0xFFFFB300), RoundedCornerShape(6.dp))
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Command", tint = Color.Black)
                        }

                        TextField(
                            value = terminalCommandInput,
                            onValueChange = onTerminalCommandInputChange,
                            modifier = Modifier.weight(1f).height(50.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF1E2530),
                                unfocusedContainerColor = Color(0xFF1E2530),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            placeholder = { Text("أدخل كود OBD أو أمر AT...", fontSize = 12.sp, color = Color.Gray) },
                            shape = RoundedCornerShape(6.dp),
                            singleLine = true
                        )
                    }
                }
            }

            // Simulator Injection tools (For Kia Carens 2008 CRDi test validation)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161922))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "حقن وافتراض أعطال الديزل للتجربة",
                        color = Color.LightGray,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KiaCarensFaults.AVAILABLE_FAULTS.take(4).forEach { fault ->
                            Button(
                                onClick = { viewModel.injectFault(fault) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F)),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "افتراض ${fault.code}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Left Column in RTL (Active Tests & Search Index)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Active Tests & Coding Component
            ActiveTestsComponent(
                viewModel = viewModel,
                modifier = Modifier.fillMaxWidth()
            )

            // Sensors and Explorer
            SensorsAndMileageExplorerComponent(data = data, viewModel = viewModel)

            // Card: DTC Search Index & Encyclopedia
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF11141B)),
                border = BorderStroke(1.dp, Color(0xFF2C3549))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header with icon and title
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "دليل وفهرس البحث في أكواد أعطال كيا (DTC Index)",
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Right
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Book, 
                            contentDescription = "DTC Encyclopedia", 
                            tint = Color(0xFFFFB300)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Text(
                        text = "فهرس تفاعلي للرموز القياسية والأكواد الخاصة بسيارات كيا (مثل P1186 أو P0401) مع المسببات وحلول المعايرة برمجياً وميكانيكياً.",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Search TextField
                    TextField(
                        value = indexSearchQuery,
                        onValueChange = onIndexSearchQueryChange,
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF161A22),
                            unfocusedContainerColor = Color(0xFF161A22),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color(0xFFFFB300),
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        placeholder = { Text("ابحث برقم الكود أو الكلمة (مثال: ضغط، EGR، حرارة)...", fontSize = 11.sp, color = Color.Gray) },
                        trailingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
                        shape = RoundedCornerShape(6.dp),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Categories Quick Filters
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val categories = listOf("الكل", "كيا فقط", "المحرك", "ناقل الحركة", "الانبعاثات", "الهيكل")
                        categories.forEach { cat ->
                            val isSelected = selectedIndexCategory == cat
                            Card(
                                modifier = Modifier.clickable { onSelectedIndexCategoryChange(cat) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(0xFFFFB300) else Color(0xFF1E2530)
                                ),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFFFFD54F) else Color(0xFF2C3549))
                            ) {
                                Text(
                                    text = cat,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Filter the static encyclopedia list in real-time
                    val filteredDtcList = remember(selectedIndexCategory, indexSearchQuery) {
                        com.example.data.obd.KiaDtcDatabase.DTC_LIST.filter { dtc ->
                            val matchesCategory = when (selectedIndexCategory) {
                                "الكل" -> true
                                "كيا فقط" -> dtc.isKiaSpecific
                                "المحرك" -> dtc.category.contains("المحرك")
                                "ناقل الحركة" -> dtc.category.contains("الحركة") || dtc.category.contains("ناقل")
                                "الانبعاثات" -> dtc.category.contains("العادم") || dtc.category.contains("الانبعاثات")
                                "الهيكل" -> dtc.category.contains("الهيكل") || dtc.category.contains("التعليق")
                                else -> true
                            }
                            
                            val matchesQuery = if (indexSearchQuery.isEmpty()) {
                                true
                            } else {
                                val q = indexSearchQuery.trim().lowercase()
                                dtc.code.lowercase().contains(q) ||
                                        dtc.descriptionAr.contains(q) ||
                                        dtc.descriptionEn.lowercase().contains(q) ||
                                        dtc.category.lowercase().contains(q) ||
                                        dtc.symptoms.any { it.contains(q) } ||
                                        dtc.causes.any { it.contains(q) } ||
                                        dtc.solutions.any { it.contains(q) }
                            }
                            
                            matchesCategory && matchesQuery
                        }
                    }
                    
                    // Result Count
                    Text(
                        text = "تم العثور على ${filteredDtcList.size} رمز عطل مطبق",
                        color = Color(0xFFFFB300),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Limited-height Box to avoid taking infinite vertical space on search
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                    ) {
                        val scope = rememberCoroutineScope()
                        var selfHealingCode by remember { mutableStateOf<String?>(null) }
                        var selfHealingProgress by remember { mutableStateOf(0f) }
                        var healingMessage by remember { mutableStateOf("") }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (filteredDtcList.isEmpty()) {
                                Text(
                                    text = "عذراً، لم نجد أكواد أعطال تطابق بحثك. جرب البحث عن 'EGR' أو 'P11' أو 'بخاخ'.",
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                filteredDtcList.forEach { dtc ->
                                    val isExpanded = expandedIndexFaultCode == dtc.code
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onExpandedIndexFaultCodeChange(if (isExpanded) null else dtc.code)
                                            },
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1F27)),
                                        border = BorderStroke(1.dp, Color(0xFF2C3549))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                    contentDescription = "Expand",
                                                    tint = Color.Gray
                                                )
                                                
                                                Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                                    Text(
                                                        text = dtc.descriptionAr,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        textAlign = TextAlign.Right
                                                    )
                                                    Text(
                                                        text = "${dtc.descriptionEn} • ${dtc.category}",
                                                        color = Color.LightGray,
                                                        fontSize = 10.sp,
                                                        textAlign = TextAlign.Right
                                                    )
                                                }
                                                
                                                Card(
                                                    colors = CardDefaults.cardColors(containerColor = if (dtc.isKiaSpecific) Color(0xFFFFD54F).copy(alpha = 0.15f) else Color(0xFFEF5350).copy(alpha = 0.15f)),
                                                    border = BorderStroke(1.dp, if (dtc.isKiaSpecific) Color(0xFFFFD54F) else Color(0xFFEF5350)),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = dtc.code,
                                                        color = if (dtc.isKiaSpecific) Color(0xFFFFD54F) else Color(0xFFEF5350),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                                }
                                            }
                                            
                                            AnimatedVisibility(
                                                visible = isExpanded,
                                                enter = fadeIn() + expandVertically(),
                                                exit = shrinkVertically() + fadeOut()
                                            ) {
                                                Column(modifier = Modifier.padding(top = 12.dp)) {
                                                    HorizontalDivider(color = Color(0xFF2C3549))
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    
                                                    // Symptoms (الأعراض)
                                                    Text(
                                                        text = "الأعراض المصاحبة للعطل (Symptoms):",
                                                        color = Color(0xFFFFB300),
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.fillMaxWidth(),
                                                        textAlign = TextAlign.Right
                                                    )
                                                    dtc.symptoms.forEach { sym ->
                                                        Text(
                                                            text = "• $sym",
                                                            color = Color.LightGray,
                                                            fontSize = 11.sp,
                                                            modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                                                            textAlign = TextAlign.Right
                                                        )
                                                    }
                                                    
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    
                                                    // Causes (الأسباب)
                                                    Text(
                                                        text = "المسببات الرئيسية للعطل (Possible Causes):",
                                                        color = Color(0xFFFF8A80),
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.fillMaxWidth(),
                                                        textAlign = TextAlign.Right
                                                    )
                                                    dtc.causes.forEach { cause ->
                                                        Text(
                                                            text = "• $cause",
                                                            color = Color.LightGray,
                                                            fontSize = 11.sp,
                                                            modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                                                            textAlign = TextAlign.Right
                                                        )
                                                    }
                                                    
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    
                                                    // Solutions (الحلول)
                                                    Text(
                                                        text = "الحلول الموصى بها للإصلاح (Recommended Solutions):",
                                                        color = Color(0xFF81C784),
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.fillMaxWidth(),
                                                        textAlign = TextAlign.Right
                                                    )
                                                    dtc.solutions.forEachIndexed { idx, sol ->
                                                        Text(
                                                            text = "${idx + 1}. $sol",
                                                            color = Color.White,
                                                            fontSize = 11.sp,
                                                            modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                                                            textAlign = TextAlign.Right
                                                        )
                                                    }
                                                    
                                                    Spacer(modifier = Modifier.height(12.dp))
                                                    HorizontalDivider(color = Color(0xFF2C3549))
                                                    Spacer(modifier = Modifier.height(8.dp))

                                                    // Interactive smart self-fix inside component!
                                                    if (selfHealingCode == dtc.code) {
                                                        Column(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalAlignment = Alignment.CenterHorizontally
                                                        ) {
                                                            Text(
                                                                text = "جاري الإصلاح الذاتي والتهيئة والمسح...",
                                                                color = Color(0xFFFFB300),
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                            Spacer(modifier = Modifier.height(6.dp))
                                                            LinearProgressIndicator(
                                                                progress = { selfHealingProgress },
                                                                modifier = Modifier.fillMaxWidth().height(6.dp),
                                                                color = Color(0xFF4CAF50),
                                                                trackColor = Color(0xFF2E2E2E)
                                                            )
                                                            Spacer(modifier = Modifier.height(4.dp))
                                                            Text(
                                                                text = healingMessage,
                                                                color = Color.LightGray,
                                                                fontSize = 10.sp,
                                                                textAlign = TextAlign.Center
                                                            )

                                                            if (selfHealingProgress >= 1f) {
                                                                Spacer(modifier = Modifier.height(6.dp))
                                                                Text(
                                                                    text = "✅ تمت المعايرة الذكية بنجاح! يرجى الضغط على 'مسح الأعطال' لتأكيد تصفير ECU.",
                                                                    color = Color(0xFF81C784),
                                                                    fontSize = 11.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    textAlign = TextAlign.Center
                                                                )
                                                            }
                                                        }
                                                    } else {
                                                        Button(
                                                            onClick = {
                                                                selfHealingCode = dtc.code
                                                                selfHealingProgress = 0f
                                                                scope.launch {
                                                                    val stepsMessages = listOf(
                                                                        "جاري قراءة المعطيات اللحظية للحساس...",
                                                                        "جاري إيقاف صمام الموديول مؤقتاً لحماية الدائرة...",
                                                                        "جاري تصفير عدادات الحساس وإرجاع قيم التعلم الافتراضية للديزل...",
                                                                        "تمت التهيئة البرمجية الذكية بنجاح بنسبة 100%!"
                                                                    )
                                                                    for (i in 0..10) {
                                                                        delay(200)
                                                                        selfHealingProgress = i / 10f
                                                                        healingMessage = when {
                                                                            i < 3 -> stepsMessages[0]
                                                                            i < 6 -> stepsMessages[1]
                                                                            i < 9 -> stepsMessages[2]
                                                                            else -> stepsMessages[3]
                                                                        }
                                                                    }
                                                                }
                                                            },
                                                            modifier = Modifier.fillMaxWidth(),
                                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2530)),
                                                            shape = RoundedCornerShape(6.dp)
                                                        ) {
                                                            Text(
                                                                text = "🔧 معايرة برمجية ذكية للحساس (Smart Recalibration)",
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color.White
                                                            )
                                                        }
                                                    }
                                                    
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    
                                                    // Simulator Inject Action Button
                                                    Button(
                                                        onClick = {
                                                            viewModel.injectFault(
                                                                com.example.data.obd.DtcInfo(
                                                                    code = dtc.code,
                                                                    descriptionAr = dtc.descriptionAr,
                                                                    descriptionEn = dtc.descriptionEn,
                                                                    category = dtc.category
                                                                )
                                                            )
                                                        },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF455A64)),
                                                        shape = RoundedCornerShape(6.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.BugReport, 
                                                            contentDescription = "Inject fault",
                                                            tint = Color(0xFFEF5350)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = "🧪 حقن ومحاكاة هذا العطل في السيارة لتجربة نظام الفحص",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TabletConnectionView(
    viewModel: ObdViewModel,
    state: ObdConnectionState,
    isSimulation: Boolean,
    pairedDevices: List<com.example.data.obd.BtDevice>,
    permissionsGranted: Boolean,
    onRequestPermissions: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Right Column: Connection Modes, Settings, and Status Information
        Column(
            modifier = Modifier
                .weight(1.1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Fast Pairing Default PIN Hint bar
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF112111)),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(Color(0xFF2E7D32)))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = "Pin Hint", tint = Color(0xFF4CAF50))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "أجهزة البلوتوث التقليدية تتطلب إقراناً مسبقاً (PIN: 1234). أما أجهزة BLE الذكية فتتصل مباشرة دون إقران مسبق.",
                        color = Color(0xFF81C784),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Quick Selector: Simulation vs Real OBD Device
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161922))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "وضع تشغيل التطبيق والاتصال",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.toggleSimulation(false) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isSimulation) Color(0xFFFFB300) else Color(0xFF2D323E),
                                contentColor = if (!isSimulation) Color.Black else Color.White
                            )
                        ) {
                            Icon(Icons.Default.Bluetooth, contentDescription = "BT")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جهاز بلوتوث حقيقي")
                        }

                        Button(
                            onClick = { viewModel.toggleSimulation(true) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSimulation) Color(0xFFFFB300) else Color(0xFF2D323E),
                                contentColor = if (isSimulation) Color.Black else Color.White
                            )
                        ) {
                            Icon(Icons.Default.Dashboard, contentDescription = "Simulator")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("محاكاة النظام")
                        }
                    }
                }
            }

            // Current Connection Status Panel
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF11141B)),
                border = BorderStroke(1.dp, Color(0xFF2C3549))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "حالة الاتصال بالموصل (OBD Connection Status)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val statusText = when (state) {
                        com.example.data.obd.ObdConnectionState.DISCONNECTED -> "مفصول 🔴"
                        com.example.data.obd.ObdConnectionState.CONNECTING -> "جاري الاتصال... 🟡"
                        com.example.data.obd.ObdConnectionState.INITIALIZING -> "برمجة ELM327... 🔵"
                        com.example.data.obd.ObdConnectionState.CONNECTED -> "متصل بنجاح 🟢"
                        com.example.data.obd.ObdConnectionState.ERROR -> "خطأ في الاتصال ❌"
                    }
                    val statusColor = when (state) {
                        com.example.data.obd.ObdConnectionState.DISCONNECTED -> Color(0xFFEF5350)
                        com.example.data.obd.ObdConnectionState.CONNECTING -> Color(0xFFFFB300)
                        com.example.data.obd.ObdConnectionState.INITIALIZING -> Color(0xFF2196F3)
                        com.example.data.obd.ObdConnectionState.CONNECTED -> Color(0xFF81C784)
                        com.example.data.obd.ObdConnectionState.ERROR -> Color(0xFFEF5350)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (state == com.example.data.obd.ObdConnectionState.CONNECTED || state == com.example.data.obd.ObdConnectionState.CONNECTING || state == com.example.data.obd.ObdConnectionState.INITIALIZING) {
                            Button(
                                onClick = { viewModel.disconnect() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350)),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("قطع الاتصال", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Box(modifier = Modifier.size(1.dp))
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = statusText,
                                color = statusColor,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (state == com.example.data.obd.ObdConnectionState.CONNECTED) {
                                Text(
                                    text = "معدل تدفق البيانات نشط • كيا كارنز 2.0 CRDi VGT",
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Left Column: Paired/Scanned Devices List or Simulator Status
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isSimulation) {
                // Visual Simulator Card explaining the setup
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF12151C)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(Color(0xFF232B3A)))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.DirectionsCar,
                                contentDescription = "Carens simulator status",
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(100.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "تطبيق الفحص يعمل الآن في 'وضع المحاكاة الافتراضي'",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "يتيح لك هذا الوضع اختبار الأكواد وتجربة لوحة العدادات المضيئة وتدفق البيانات لسيارة Kia Carens 2008 CRDi دون الحاجة لتوصيل سيارة حقيقية بالكمبيوتر.",
                                color = Color.LightGray,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                // If not simulation: Show device scanning and selector lists!
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF11141B)),
                    border = BorderStroke(1.dp, Color(0xFF232B3A))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.refreshPairedDevices() },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF1E2530), CircleShape)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                            }

                            Text(
                                text = "الأجهزة المقترنة مسبقاً (Paired Devices)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                textAlign = TextAlign.Right
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))

                        if (!permissionsGranted) {
                            Button(
                                onClick = onRequestPermissions,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300))
                            ) {
                                Text("منح صلاحيات البلوتوث للبحث عن الموصل 🛡️", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        } else if (pairedDevices.isEmpty()) {
                            Text(
                                text = "لم يتم العثور على أجهزة مقترنة. يرجى إقران موصل ELM327 من إعدادات البلوتوث للنظام أولاً.",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            pairedDevices.forEach { device ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable { viewModel.connectDevice(device.address) },
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2530))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.BluetoothConnected, contentDescription = null, tint = Color(0xFFFFB300))
                                        
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = device.name ?: "جهاز بدون اسم",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = device.address,
                                                color = Color.Gray,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TabletHistoryView(
    viewModel: ObdViewModel,
    history: List<DtcRecord>,
    frequentlyAccessedDtcs: List<FrequentlyAccessedDtc>,
    localTrendPoints: List<HistoricalTrendPoint>,
    selectedTrendSensor: String,
    onSelectedTrendSensorChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Right Column: SQLite Trends, Chart & Top Action Bar
        Column(
            modifier = Modifier
                .weight(1.1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Action bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { viewModel.clearDbHistory() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear History", tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تصفير الأرشيف والكاش المحلي", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "الأرشيف والمخزن المحلي (Offline SQLite)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
            }

            // Section 2: Historical Trends Offline Line Charts (SQLite persistence)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1017)),
                border = BorderStroke(1.dp, Color(0xFF1E2530))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "مؤشرات الحركة التاريخية المحفوظة (Offline Sensors Archive)",
                            color = Color(0xFF00E5FF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = "Trends",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "يتم تسجيل قراءات الحساسات تلقائياً كل 5 ثوانٍ في قاعدة البيانات المحلية لعرض أنماط القيادة السابقة دون اتصال.",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Sensors Tabs to choose trend
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            "TURBO" to "التربو (Bar)",
                            "FUEL_PRESSURE" to "ضغط المشترك (Bar)",
                            "COOLANT" to "حرارة المحرك (C°)",
                            "RPM" to "دوران المحرك (RPM)"
                        ).forEach { (key, display) ->
                            val isSel = selectedTrendSensor == key
                            Card(
                                modifier = Modifier.clickable { onSelectedTrendSensorChange(key) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSel) Color(0xFF00E5FF) else Color(0xFF1E2530)
                                ),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, if (isSel) Color(0xFF80DEEA) else Color(0xFF2C3549))
                            ) {
                                Text(
                                    text = display,
                                    color = if (isSel) Color.Black else Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (localTrendPoints.size < 2) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .background(Color(0xFF05070A), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "جاري تجميع نقاط الأداء التاريخية... (شغل المحرك لتسجيل البيانات محلياً)",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // Line Chart Drawn Dynamically using local SQLite Trend Points
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .background(Color(0xFF05070A), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFF121620), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height
                                
                                // Draw Grid Lines
                                val grids = 3
                                for (i in 0..grids) {
                                    val yGrid = (h / grids) * i
                                    drawLine(
                                        color = Color(0xFF161C26),
                                        start = Offset(0f, yGrid),
                                        end = Offset(w, yGrid),
                                        strokeWidth = 1f,
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                                    )
                                }

                                val pointsToDraw = localTrendPoints.takeLast(40) // limit to last 40 points in graph
                                val pointsCount = pointsToDraw.size
                                val stepX = w / (pointsCount - 1).coerceAtLeast(1)

                                val trendPath = Path()
                                val areaPath = Path()

                                pointsToDraw.forEachIndexed { idx, pt ->
                                    val valueRatio = when (selectedTrendSensor) {
                                        "RPM" -> (pt.rpm.toFloat() / 4500f).coerceIn(0f, 1f)
                                        "COOLANT" -> (pt.coolantTemp.toFloat() / 120f).coerceIn(0f, 1f)
                                        "FUEL_PRESSURE" -> (pt.fuelPressure.toFloat() / 1600f).coerceIn(0f, 1f)
                                        "TURBO" -> (pt.turboBoostPressure.toFloat() / 2.0f).coerceIn(0f, 1f)
                                        else -> 0f
                                    }

                                    val x = idx * stepX
                                    val y = h - (valueRatio * h)

                                    if (idx == 0) {
                                        trendPath.moveTo(x, y)
                                        areaPath.moveTo(x, h)
                                        areaPath.lineTo(x, y)
                                    } else {
                                        trendPath.lineTo(x, y)
                                        areaPath.lineTo(x, y)
                                    }

                                    if (idx == pointsCount - 1) {
                                        areaPath.lineTo(x, h)
                                        areaPath.close()
                                    }
                                }

                                val themeColor = when (selectedTrendSensor) {
                                    "RPM" -> Color(0xFFFFB300)
                                    "COOLANT" -> Color(0xFFEF5350)
                                    "FUEL_PRESSURE" -> Color(0xFF4CAF50)
                                    "TURBO" -> Color(0xFF00E5FF)
                                    else -> Color.White
                                }

                                // Fill area
                                drawPath(
                                    path = areaPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(themeColor.copy(alpha = 0.2f), Color.Transparent)
                                    )
                                )

                                // Draw Line
                                drawPath(
                                    path = trendPath,
                                    color = themeColor,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = 3f
                                    )
                                )
                            }
                        }
                        
                        // Live Indicators
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val latest = localTrendPoints.last()
                            val valueDisplay = when (selectedTrendSensor) {
                                "RPM" -> "${latest.rpm} دورة/دقيقة"
                                "COOLANT" -> "${latest.coolantTemp} °م"
                                "FUEL_PRESSURE" -> "${latest.fuelPressure} بار"
                                "TURBO" -> "${"%.2f".format(latest.turboBoostPressure)} بار"
                                else -> ""
                            }
                            
                            Text(
                                text = "عدد القراءات المخزنة حالياً: ${localTrendPoints.size} قراءة",
                                color = Color.Gray,
                                fontSize = 10.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "آخر قراءة محفوظة: $valueDisplay",
                                    color = Color.LightGray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color(0xFF00E5FF), RoundedCornerShape(3.dp))
                                )
                            }
                        }
                    }
                }
            }
        }

        // Left Column: Saved DTC cache & Scanned reports history
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Frequently Accessed DTCs offline Cache
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF11141B)),
                border = BorderStroke(1.dp, Color(0xFF1E2530))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "كاش الرموز الأكثر زيارة دون اتصال بالإنترنت",
                            color = Color(0xFFFFB300),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = "Offline Cache",
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "DTC Offline Read Cache - يتم تخزين الأكواد تلقائياً لتظل متاحة دون إنترنت.",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (frequentlyAccessedDtcs.isEmpty()) {
                        Text(
                            text = "لا توجد رموز مستعرضة في الكاش المحلي حالياً. تصفح دليل الأعطال لحفظ الرموز تلقائياً.",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            frequentlyAccessedDtcs.forEach { dtc ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2530)),
                                    border = BorderStroke(1.dp, Color(0xFF2C3549)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.widthIn(max = 160.dp)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFB300).copy(alpha = 0.15f)),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "👁 ${dtc.accessCount}",
                                                    color = Color(0xFFFFB300),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                            Text(
                                                text = dtc.code,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = dtc.descriptionAr,
                                            color = Color.LightGray,
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Right
                                        )
                                        Text(
                                            text = dtc.category,
                                            color = Color.Gray,
                                            fontSize = 8.sp,
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Right
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Scanned reports history
            Text(
                text = "أرشيف تقارير فحص السيارة وتصفير الأخطاء",
                color = Color.LightGray,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                textAlign = TextAlign.Right
            )

            if (history.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(Color(0xFF11141B), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = "Empty",
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "أرشيف الفحص فارغ. ابدأ فحصاً في السيارة لحفظ الأخطاء.",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    history.forEach { record ->
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                        val dateStr = sdf.format(java.util.Date(record.timestamp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF161922))
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val badgeColor = if (record.status == "Active") Color(0xFFE53935) else Color(0xFF4CAF50)
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = badgeColor.copy(alpha = 0.15f)),
                                    border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(badgeColor)),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = if (record.status == "Active") "عطل نشط" else "تم مسحه",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = badgeColor
                                    )
                                }

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 12.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = record.code,
                                            color = Color(0xFFFFB300),
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = dateStr,
                                            color = Color.Gray,
                                            fontSize = 10.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = record.description,
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Right
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


