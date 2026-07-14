package com.example.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.DtcRecord
import com.example.data.db.FrequentlyAccessedDtc
import com.example.data.db.HistoricalTrendPoint
import com.example.data.obd.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ObdViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("ObdLayoutPrefs", android.content.Context.MODE_PRIVATE)

    private val database = AppDatabase.getDatabase(application)
    private val obdManager = BluetoothObdManager(application)
    val repository = ObdRepository(
        obdManager = obdManager,
        dtcDao = database.dtcDao(),
        frequentlyAccessedDtcDao = database.frequentlyAccessedDtcDao(),
        historicalTrendPointDao = database.historicalTrendPointDao()
    )

    val connectionState: StateFlow<ObdConnectionState> = repository.connectionState
    val sensorData: StateFlow<ObdSensorData> = repository.sensorData
    val errorMessage: StateFlow<String?> = repository.errorMessage
    val isSimulation: StateFlow<Boolean> = repository.isSimulation
    val isEcoMode: StateFlow<Boolean> = repository.isEcoMode
    val terminalLogs: StateFlow<List<String>> = repository.terminalLogs
    val connectedDeviceName: StateFlow<String?> = repository.connectedDeviceName
    val isDpfRegenerating: StateFlow<Boolean> = repository.isDpfRegenerating

    enum class DriveMode {
        NORMAL, ECO, SPORT, COMFORT
    }

    private val _driveMode = MutableStateFlow(DriveMode.NORMAL)
    val driveMode: StateFlow<DriveMode> = _driveMode.asStateFlow()

    // Persistent Settings
    private val _unitSystem = MutableStateFlow(sharedPrefs.getString("settings_unit_system", "METRIC") ?: "METRIC")
    val unitSystem: StateFlow<String> = _unitSystem.asStateFlow()

    private val _connectionType = MutableStateFlow(sharedPrefs.getString("settings_connection_type", "BT_CLASSIC") ?: "BT_CLASSIC")
    val connectionType: StateFlow<String> = _connectionType.asStateFlow()

    private val _voiceAlerts = MutableStateFlow(sharedPrefs.getBoolean("settings_voice_alerts", true))
    val voiceAlerts: StateFlow<Boolean> = _voiceAlerts.asStateFlow()

    private val _appThemeMode = MutableStateFlow(sharedPrefs.getString("settings_app_theme", "CARENS_GOLD") ?: "CARENS_GOLD")
    val appThemeMode: StateFlow<String> = _appThemeMode.asStateFlow()

    fun setUnitSystem(system: String) {
        _unitSystem.value = system
        sharedPrefs.edit().putString("settings_unit_system", system).apply()
    }

    fun setConnectionType(type: String) {
        _connectionType.value = type
        sharedPrefs.edit().putString("settings_connection_type", type).apply()
    }

    fun setVoiceAlerts(enabled: Boolean) {
        _voiceAlerts.value = enabled
        sharedPrefs.edit().putBoolean("settings_voice_alerts", enabled).apply()
    }

    fun setAppThemeMode(theme: String) {
        _appThemeMode.value = theme
        sharedPrefs.edit().putString("settings_app_theme", theme).apply()
    }

    fun setDriveMode(mode: DriveMode) {
        _driveMode.value = mode
        setEcoMode(mode == DriveMode.ECO)
    }

    val isOverheatingSimulated: StateFlow<Boolean> = repository.isOverheatingSimulated
    val isLowOilPressureSimulated: StateFlow<Boolean> = repository.isLowOilPressureSimulated
    val isAlternatorFailureSimulated: StateFlow<Boolean> = repository.isAlternatorFailureSimulated

    fun toggleOverheating(enabled: Boolean) {
        repository.toggleOverheating(enabled)
    }

    fun toggleLowOilPressure(enabled: Boolean) {
        repository.toggleLowOilPressure(enabled)
    }

    fun toggleAlternatorFailure(enabled: Boolean) {
        repository.toggleAlternatorFailure(enabled)
    }

    val savedDtcHistory: StateFlow<List<DtcRecord>> = repository.savedDtcHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val frequentlyAccessedDtcs: StateFlow<List<FrequentlyAccessedDtc>> = repository.frequentlyAccessedDtcs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val localTrendPoints: StateFlow<List<HistoricalTrendPoint>> = repository.localTrendPoints
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun recordDtcClick(dtc: DtcInfo) {
        viewModelScope.launch {
            repository.recordDtcAccess(
                code = dtc.code,
                descAr = dtc.descriptionAr,
                descEn = dtc.descriptionEn,
                category = dtc.category
            )
        }
    }

    private val _pairedDevices = MutableStateFlow<List<BtDevice>>(emptyList())
    val pairedDevices: StateFlow<List<BtDevice>> = _pairedDevices.asStateFlow()

    val isBleScanning: StateFlow<Boolean> = repository.isBleScanning
    val discoveredBleDevices: StateFlow<List<BtDevice>> = repository.discoveredBleDevices

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _sensorHistory = MutableStateFlow<List<SensorHistoryPoint>>(emptyList())
    val sensorHistory: StateFlow<List<SensorHistoryPoint>> = _sensorHistory.asStateFlow()

    private val _fullSensorHistory = MutableStateFlow<List<ObdSensorData>>(emptyList())
    val fullSensorHistory: StateFlow<List<ObdSensorData>> = _fullSensorHistory.asStateFlow()

    private val _aiHealthReport = MutableStateFlow<EngineHealthReport?>(null)
    val aiHealthReport: StateFlow<EngineHealthReport?> = _aiHealthReport.asStateFlow()

    private val _isAiAnalyzing = MutableStateFlow(false)
    val isAiAnalyzing: StateFlow<Boolean> = _isAiAnalyzing.asStateFlow()

    private val defaultLayout = listOf(
        "RPM_SPEED",
        "BATTERY",
        "COOLANT",
        "TURBO",
        "POWER",
        "INFO_BANNER",
        "OIL_MAINTENANCE",
        "DPF"
    )

    private val _dashboardLayout = MutableStateFlow<List<String>>(emptyList())
    val dashboardLayout: StateFlow<List<String>> = _dashboardLayout.asStateFlow()

    init {
        val savedLayoutStr = sharedPrefs.getString("gauge_layout_order", null)
        if (savedLayoutStr != null) {
            val list = savedLayoutStr.split(",").filter { it.isNotEmpty() }
            if (list.size == defaultLayout.size && list.all { it in defaultLayout }) {
                _dashboardLayout.value = list
            } else {
                _dashboardLayout.value = defaultLayout
            }
        } else {
            _dashboardLayout.value = defaultLayout
        }

        refreshPairedDevices()
        
        // Trigger initial local rule-based analysis so it is never blank on startup
        viewModelScope.launch {
            _aiHealthReport.value = GeminiHealthPredictor.calculateLocalRuleHealth(emptyList(), sensorData.value)
        }
        
        // Collect sensorData to build rolling history for telemetry graphs and AI trends
        viewModelScope.launch {
            var lastUpdate = 0L
            var lastDbSave = 0L
            sensorData.collect { data ->
                val now = System.currentTimeMillis()
                // Limit updates to at most once per 600ms to keep it smooth and avoid performance issues on weak devices
                if (now - lastUpdate >= 600L) {
                    lastUpdate = now
                    _sensorHistory.value = (_sensorHistory.value + SensorHistoryPoint(
                        timestamp = now,
                        rpm = data.rpm,
                        coolantTemp = data.coolantTemp,
                        fuelPressure = data.fuelPressure
                    )).let { list ->
                        if (list.size > 50) list.takeLast(50) else list
                    }

                    _fullSensorHistory.value = (_fullSensorHistory.value + data).let { list ->
                        if (list.size > 30) list.takeLast(30) else list
                    }
                    
                    // If no Gemini AI report is available, or if it was rule-based, keep it updated in real-time
                    if (_aiHealthReport.value == null || _aiHealthReport.value?.isRealAi == false) {
                        _aiHealthReport.value = GeminiHealthPredictor.calculateLocalRuleHealth(_fullSensorHistory.value, data)
                    }

                    // Save trend point to Room database every 5 seconds
                    if (now - lastDbSave >= 5000L) {
                        lastDbSave = now
                        repository.saveTrendPoint(
                            HistoricalTrendPoint(
                                timestamp = now,
                                rpm = data.rpm,
                                speed = data.speed,
                                coolantTemp = data.coolantTemp,
                                fuelPressure = data.fuelPressure,
                                turboBoostPressure = data.turboBoostPressure,
                                batteryVoltage = data.batteryVoltage,
                                engineLoad = data.engineLoad,
                                dpfSootLevel = data.dpfSootLevel,
                                oilPressure = data.oilPressure
                            )
                        )
                    }
                }
            }
        }
    }

    fun runEngineHealthAnalysis() {
        viewModelScope.launch {
            _isAiAnalyzing.value = true
            try {
                val report = GeminiHealthPredictor.analyzeEngineHealthWithGemini(
                    history = _fullSensorHistory.value,
                    currentData = sensorData.value
                )
                _aiHealthReport.value = report
            } catch (e: Exception) {
                // Fallback to local rule engine if any unhandled error
                _aiHealthReport.value = GeminiHealthPredictor.calculateLocalRuleHealth(
                    _fullSensorHistory.value,
                    sensorData.value
                )
            } finally {
                _isAiAnalyzing.value = false
            }
        }
    }

    fun refreshPairedDevices() {
        _pairedDevices.value = repository.getPairedDevices()
    }

    fun connectDevice(address: String) {
        viewModelScope.launch {
            repository.connectToDevice(address)
        }
    }

    fun startBleScan() {
        repository.startBleScan()
    }

    fun stopBleScan() {
        repository.stopBleScan()
    }

    fun connectBleDevice(address: String) {
        viewModelScope.launch {
            repository.connectToBleDevice(address)
        }
    }

    fun getConnectedUsbDevices(): List<BtDevice> = repository.getConnectedUsbDevices()

    fun connectUsbDevice(address: String, name: String) {
        viewModelScope.launch {
            repository.connectUsbDevice(address, name)
        }
    }

    fun performGaugeSweep() {
        repository.performGaugeSweep()
    }

    fun performWarningLightsTest() {
        repository.performWarningLightsTest()
    }

    fun performDpfRegeneration() {
        repository.performDpfRegeneration()
    }

    fun performActiveLockTest() {
        repository.performActiveLockTest()
    }

    fun performActiveWindowTest() {
        repository.performActiveWindowTest()
    }

    fun performActiveFuelPumpTest() {
        repository.performActiveFuelPumpTest()
    }

    fun performActiveFanTest(speed: String) {
        repository.performActiveFanTest(speed)
    }

    fun performInjectorCoding(cylinder: Int, code: String) {
        repository.performInjectorCoding(cylinder, code)
    }

    fun toggleSimulation(enabled: Boolean) {
        repository.setSimulationMode(enabled)
    }

    fun applyThrottle(throttle: Float) {
        repository.applyThrottle(throttle)
    }

    fun toggleEngine() {
        repository.toggleEnginePower()
    }

    fun toggleDoor(doorIndex: Int) {
        repository.toggleDoor(doorIndex)
    }

    fun setWindowPosition(windowIndex: Int, positionPercent: Int) {
        repository.setWindowPosition(windowIndex, positionPercent)
    }

    fun toggleCentralLock() {
        repository.toggleCentralLock()
    }

    fun resetOilLife() {
        repository.resetOilLife()
    }

    fun injectFault(fault: DtcInfo) {
        repository.injectFaultCode(fault)
    }

    fun performTroubleCodeScan() {
        viewModelScope.launch {
            _isScanning.value = true
            // Real diagnostic scanning lag feeling
            kotlinx.coroutines.delay(2000)

            val currentDtcs = sensorData.value.activeDtcs
            if (currentDtcs.isEmpty()) {
                // Log all clean scan
                repository.saveDtcRecord(
                    DtcRecord(
                        code = "P0000",
                        description = "النظام سليم بالكامل - لا توجد أكواد أعطال مسجلة لكيا كارنز",
                        status = "Cleared"
                    )
                )
            } else {
                for (dtc in currentDtcs) {
                    repository.saveDtcRecord(
                        DtcRecord(
                            code = dtc.code,
                            description = "${dtc.descriptionAr} | ${dtc.category}",
                            status = "Active"
                        )
                    )
                }
            }
            _isScanning.value = false
        }
    }

    fun clearFaults() {
        viewModelScope.launch {
            _isScanning.value = true
            kotlinx.coroutines.delay(1500) // Clearing lag
            repository.clearFaultCodes()
            _isScanning.value = false
        }
    }

    fun clearDbHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun setEcoMode(enabled: Boolean) {
        repository.setEcoMode(enabled)
    }

    fun updateDashboardLayout(newLayout: List<String>) {
        _dashboardLayout.value = newLayout
        sharedPrefs.edit().putString("gauge_layout_order", newLayout.joinToString(",")).apply()
    }

    fun moveWidgetUp(index: Int) {
        if (index <= 0) return
        val current = _dashboardLayout.value.toMutableList()
        val temp = current[index]
        current[index] = current[index - 1]
        current[index - 1] = temp
        updateDashboardLayout(current)
    }

    fun moveWidgetDown(index: Int) {
        val current = _dashboardLayout.value.toMutableList()
        if (index >= current.size - 1) return
        val temp = current[index]
        current[index] = current[index + 1]
        current[index + 1] = temp
        updateDashboardLayout(current)
    }

    fun resetLayout() {
        updateDashboardLayout(defaultLayout)
    }

    fun clearTerminalLogs() {
        repository.clearTerminalLogs()
    }

    fun sendTerminalCommand(cmd: String) {
        repository.sendCustomTerminalCommand(cmd)
    }

    fun disconnect() {
        repository.disconnect()
    }

    override fun onCleared() {
        super.onCleared()
        repository.disconnect()
    }
}
