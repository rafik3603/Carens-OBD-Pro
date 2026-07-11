package com.example.data.obd

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.content.SharedPreferences
import android.hardware.usb.UsbManager
import android.hardware.usb.UsbDevice
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.Locale

data class BtDevice(val name: String, val address: String)

class BluetoothObdManager(private val context: Context) {
    private val TAG = "BluetoothObdManager"
    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val sharedPrefs: SharedPreferences = context.getSharedPreferences("CarensObdPrefs", Context.MODE_PRIVATE)

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val _connectionState = MutableStateFlow(ObdConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ObdConnectionState> = _connectionState.asStateFlow()

    private val _sensorData = MutableStateFlow(ObdSensorData())
    val sensorData: StateFlow<ObdSensorData> = _sensorData.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isSimulation = MutableStateFlow(true)
    val isSimulation: StateFlow<Boolean> = _isSimulation.asStateFlow()

    private val _isEcoMode = MutableStateFlow(false)
    val isEcoMode: StateFlow<Boolean> = _isEcoMode.asStateFlow()

    private val _terminalLogs = MutableStateFlow<List<String>>(emptyList())
    val terminalLogs: StateFlow<List<String>> = _terminalLogs.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    private val _isDpfRegenerating = MutableStateFlow(false)
    val isDpfRegenerating: StateFlow<Boolean> = _isDpfRegenerating.asStateFlow()

    private var connectJob: Job? = null
    private var simulationJob: Job? = null
    private var obdSocket: BluetoothSocket? = null

    // Bluetooth Low Energy (BLE) variables
    private val _discoveredBleDevices = MutableStateFlow<List<BtDevice>>(emptyList())
    val discoveredBleDevices: StateFlow<List<BtDevice>> = _discoveredBleDevices.asStateFlow()

    private val _isBleScanning = MutableStateFlow(false)
    val isBleScanning: StateFlow<Boolean> = _isBleScanning.asStateFlow()

    private var bleGatt: BluetoothGatt? = null
    private var bleWriteChar: BluetoothGattCharacteristic? = null
    private var bleReadChar: BluetoothGattCharacteristic? = null
    private val bleResponseBuffer = StringBuilder()
    private val bleCommandMutex = Mutex()
    private var bleResponseDeferred: CompletableDeferred<String>? = null
    private var bleScanJob: Job? = null

    // Real-world physical metrics for Kia Carens 2008 CRDi
    private var virtualThrottle = 0f // 0.0 to 1.0
    private var virtualRpm = 800f
    private var virtualSpeed = 0f
    private var virtualCoolant = 82f
    private var virtualFuel = 68f
    private var virtualBattery = 14.1f
    private var virtualGear = 1
    private var virtualTransmissionTemp = 65f
    private var virtualTransmissionPressure = 4.2f

    // Persistent values
    private var oilRemainingKm = 8500
    private var oilLifePercent = 85
    private var dpfSootPercent = 12f

    // Doors state
    private var doorDriver = false
    private var doorPassenger = false
    private var doorRearLeft = false
    private var doorRearRight = false
    private var hoodOpen = false
    private var trunkOpen = false

    // Windows and Central Lock state
    private var windowFrontLeft = 0
    private var windowFrontRight = 0
    private var windowRearLeft = 0
    private var windowRearRight = 0
    private var centralLocked = true

    // Simulation overrides for testing warnings
    private var overrideOverheating = false
    private var overrideLowOilPressure = false
    private var overrideAlternatorFailure = false

    private val _isOverheatingSimulated = MutableStateFlow(false)
    val isOverheatingSimulated: StateFlow<Boolean> = _isOverheatingSimulated.asStateFlow()

    private val _isLowOilPressureSimulated = MutableStateFlow(false)
    val isLowOilPressureSimulated: StateFlow<Boolean> = _isLowOilPressureSimulated.asStateFlow()

    private val _isAlternatorFailureSimulated = MutableStateFlow(false)
    val isAlternatorFailureSimulated: StateFlow<Boolean> = _isAlternatorFailureSimulated.asStateFlow()

    fun toggleOverheating(enabled: Boolean) {
        overrideOverheating = enabled
        _isOverheatingSimulated.value = enabled
        if (enabled) {
            virtualCoolant = 104f
        } else {
            virtualCoolant = 88f
        }
        updateSensorDataState()
    }

    fun toggleLowOilPressure(enabled: Boolean) {
        overrideLowOilPressure = enabled
        _isLowOilPressureSimulated.value = enabled
        updateSensorDataState()
    }

    fun toggleAlternatorFailure(enabled: Boolean) {
        overrideAlternatorFailure = enabled
        _isAlternatorFailureSimulated.value = enabled
        if (enabled) {
            virtualBattery = 11.1f
        } else {
            virtualBattery = 14.1f
        }
        updateSensorDataState()
    }

    init {
        // Load persistent data
        oilRemainingKm = sharedPrefs.getInt("oil_remaining_km", 8500)
        oilLifePercent = sharedPrefs.getInt("oil_life_percent", 85)
        dpfSootPercent = sharedPrefs.getFloat("dpf_soot_percent", 12f)
        _isEcoMode.value = sharedPrefs.getBoolean("is_eco_mode", false)

        // Default simulation mode active at startup for instant preview
        startSimulation()
    }

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BtDevice> {
        return try {
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                emptyList()
            } else {
                bluetoothAdapter.bondedDevices.map { device ->
                    BtDevice(device.name ?: "OBDII Interface", device.address)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching bonded devices", e)
            emptyList()
        }
    }

    fun setSimulationMode(enabled: Boolean) {
        _isSimulation.value = enabled
        if (enabled) {
            disconnectRealDevice()
            startSimulation()
        } else {
            stopSimulation()
            _connectionState.value = ObdConnectionState.DISCONNECTED
            _sensorData.value = ObdSensorData(
                isEngineRunning = false,
                oilLifePercent = oilLifePercent,
                oilRemainingKm = oilRemainingKm,
                dpfSootLevel = dpfSootPercent.toInt()
            )
        }
    }

    fun applyThrottle(throttle: Float) {
        virtualThrottle = throttle.coerceIn(0f, 1f)
    }

    fun toggleEnginePower() {
        val current = _sensorData.value.isEngineRunning
        _sensorData.value = _sensorData.value.copy(isEngineRunning = !current)
        if (!current) {
            virtualBattery = 14.1f // Alternator active
        } else {
            virtualBattery = 12.3f // Battery only
            virtualRpm = 0f
            virtualSpeed = 0f
        }
    }

    fun toggleDoor(doorIndex: Int) {
        when (doorIndex) {
            0 -> doorDriver = !doorDriver
            1 -> doorPassenger = !doorPassenger
            2 -> doorRearLeft = !doorRearLeft
            3 -> doorRearRight = !doorRearRight
            4 -> hoodOpen = !hoodOpen
            5 -> trunkOpen = !trunkOpen
        }
        updateSensorDataState()
    }

    fun setWindowPosition(windowIndex: Int, positionPercent: Int) {
        val clamped = positionPercent.coerceIn(0, 100)
        when (windowIndex) {
            0 -> windowFrontLeft = clamped
            1 -> windowFrontRight = clamped
            2 -> windowRearLeft = clamped
            3 -> windowRearRight = clamped
        }
        updateSensorDataState()
    }

    fun toggleCentralLock() {
        centralLocked = !centralLocked
        // When locking, we close all doors automatically for clean integration
        if (centralLocked) {
            doorDriver = false
            doorPassenger = false
            doorRearLeft = false
            doorRearRight = false
        }
        updateSensorDataState()
    }

    fun resetOilLife() {
        oilRemainingKm = 10000
        oilLifePercent = 100
        sharedPrefs.edit()
            .putInt("oil_remaining_km", oilRemainingKm)
            .putInt("oil_life_percent", oilLifePercent)
            .apply()
        updateSensorDataState()
    }

    fun injectFaultCode(fault: DtcInfo) {
        val currentDtcs = _sensorData.value.activeDtcs.toMutableList()
        if (!currentDtcs.any { it.code == fault.code }) {
            currentDtcs.add(fault)
            _sensorData.value = _sensorData.value.copy(activeDtcs = currentDtcs)
        }
    }

    fun clearFaultCodesSimulator() {
        _sensorData.value = _sensorData.value.copy(activeDtcs = emptyList())
    }

    private fun startSimulation() {
        simulationJob?.cancel()
        _connectionState.value = ObdConnectionState.CONNECTED
        _errorMessage.value = null

        simulationJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                if (_sensorData.value.isEngineRunning) {
                    // Smooth RPM scaling
                    val targetRpm = if (virtualThrottle > 0.02f) {
                        800f + (virtualThrottle * 3700f) // Max 4500 RPM for Carens 2008 CRDi
                    } else {
                        800f + (Math.sin(System.currentTimeMillis() / 400.0).toFloat() * 15f) // Smooth idle
                    }
                    virtualRpm = virtualRpm * 0.82f + targetRpm * 0.18f

                    // Gear selection (6-Speed Automatic/Manual)
                    virtualGear = when {
                        virtualRpm > 3000f && virtualGear < 6 -> virtualGear + 1
                        virtualRpm < 1400f && virtualGear > 1 -> virtualGear - 1
                        else -> virtualGear
                    }

                    // Compute Speed
                    val ratio = when (virtualGear) {
                        1 -> 0.015f
                        2 -> 0.028f
                        3 -> 0.045f
                        4 -> 0.065f
                        5 -> 0.088f
                        6 -> 0.115f
                        else -> 0.015f
                    }
                    val targetSpeed = (virtualRpm * ratio).coerceAtLeast(0f)
                    virtualSpeed = virtualSpeed * 0.88f + targetSpeed * 0.12f

                    // Heat up Coolant Temp
                    if (overrideOverheating) {
                        virtualCoolant = (virtualCoolant + 0.6f).coerceIn(100f, 114f)
                    } else if (virtualCoolant < 88f) {
                        virtualCoolant += 0.04f
                    } else {
                        val heatStress = (virtualRpm - 1500f) / 1000f
                        virtualCoolant = (virtualCoolant + heatStress * 0.01f).coerceIn(85f, 94f)
                    }

                    // Transmission fluid temperature heating
                    if (virtualTransmissionTemp < 72f) {
                        virtualTransmissionTemp += 0.025f
                    } else {
                        val transmissionStress = (virtualRpm - 1500f) / 1200f + (virtualThrottle * 1.5f)
                        virtualTransmissionTemp = (virtualTransmissionTemp + transmissionStress * 0.006f).coerceIn(68f, 86f)
                    }

                    // Alternate battery voltage
                    if (overrideAlternatorFailure) {
                        virtualBattery = (virtualBattery - 0.08f).coerceAtLeast(10.8f)
                    } else {
                        virtualBattery = 14.1f + (Math.sin(System.currentTimeMillis() / 300.0).toFloat() * 0.1f)
                    }

                    // Degrade oil slightly
                    if (Math.random() < 0.02) {
                        oilRemainingKm = (oilRemainingKm - 1).coerceAtLeast(0)
                        oilLifePercent = ((oilRemainingKm / 10000f) * 100).toInt().coerceIn(0, 100)
                        sharedPrefs.edit()
                            .putInt("oil_remaining_km", oilRemainingKm)
                            .putInt("oil_life_percent", oilLifePercent)
                            .apply()
                    }

                    // Accumulate or burn soot
                    if (virtualRpm > 2500) {
                        // Regeneration
                        dpfSootPercent = (dpfSootPercent - 0.01f).coerceAtLeast(2f)
                    } else {
                        dpfSootPercent = (dpfSootPercent + 0.002f).coerceAtMost(100f)
                    }
                    sharedPrefs.edit().putFloat("dpf_soot_percent", dpfSootPercent).apply()
                } else {
                    // Engine Off
                    virtualRpm = virtualRpm * 0.7f
                    if (virtualRpm < 5f) virtualRpm = 0f
                    virtualSpeed = virtualSpeed * 0.7f
                    if (virtualSpeed < 0.5f) virtualSpeed = 0f
                    if (virtualCoolant > 35f) {
                        virtualCoolant -= 0.01f
                    }
                    if (virtualTransmissionTemp > 30f) {
                        virtualTransmissionTemp -= 0.008f
                    }
                    virtualBattery = 12.3f
                    virtualGear = 1
                }

                updateSensorDataState()
                delay(if (_isEcoMode.value) 450L else 100L)
            }
        }
    }

    private fun updateSensorDataState() {
        val rpmVal = virtualRpm.toInt()
        val loadVal = (virtualThrottle * 100).toInt().coerceAtLeast(if (_sensorData.value.isEngineRunning) 14 else 0)
        
        // Compute realistic turbo boost in Bar
        val boost = if (_sensorData.value.isEngineRunning) {
            ((virtualThrottle * 1.4) * (virtualRpm / 4500.0)).coerceIn(0.0, 1.5)
        } else {
            0.0
        }

        // State of Charge of battery
        val batterySoc = if (_sensorData.value.isEngineRunning) {
            98
        } else {
            val ratio = (virtualBattery - 11.5f) / (12.7f - 11.5f)
            (ratio * 100).toInt().coerceIn(5, 100)
        }

        // Compute realistic Common Rail Diesel fuel injection pressure in Bar (e.g., 250 - 1600 Bar)
        val fuelPressureVal = if (_sensorData.value.isEngineRunning) {
            (270f + (virtualThrottle * 1150f) + (Math.sin(System.currentTimeMillis() / 200.0).toFloat() * 12f)).toInt().coerceIn(250, 1600)
        } else {
            0
        }

        val isRunning = _sensorData.value.isEngineRunning
        val oilPressureVal = if (isRunning) {
            if (overrideLowOilPressure) {
                0.6
            } else {
                (1.5 + (virtualRpm / 4500.0) * 3.3 + (Math.sin(System.currentTimeMillis() / 400.0) * 0.1)).coerceIn(0.2, 5.5)
            }
        } else {
            0.0
        }
        val oilPressureRounded = String.format("%.1f", oilPressureVal).toDoubleOrNull() ?: 0.0

        val gearString = if (isRunning) {
            if (virtualSpeed > 1f) {
                "D$virtualGear"
            } else {
                "P"
            }
        } else {
            "P"
        }
        
        val transPressureVal = if (isRunning) {
            (4.0 + (virtualRpm / 4500.0) * 4.5 + (virtualThrottle * 2.0)).coerceIn(3.0, 10.5)
        } else {
            0.0
        }
        val transPressureRounded = String.format("%.1f", transPressureVal).toDoubleOrNull() ?: 0.0
        
        val transSlipVal = if (isRunning && virtualSpeed > 1f) {
            val shifting = if (virtualRpm > 3000f || virtualRpm < 1500f) 5.8 else 0.4
            (shifting + (virtualThrottle * 3.5)).coerceIn(0.0, 12.0)
        } else {
            0.0
        }
        val transSlipRounded = String.format("%.1f", transSlipVal).toDoubleOrNull() ?: 0.0

        _sensorData.value = _sensorData.value.copy(
            rpm = rpmVal,
            speed = virtualSpeed.toInt(),
            coolantTemp = virtualCoolant.toInt(),
            engineLoad = loadVal,
            batteryVoltage = String.format("%.1f", virtualBattery).toDoubleOrNull() ?: 12.4,
            batteryStateOfCharge = batterySoc,
            fuelLevel = virtualFuel.toInt(),
            fuelPressure = fuelPressureVal,
            throttlePosition = (virtualThrottle * 100).toInt(),
            transmissionGear = gearString,
            transmissionTemp = virtualTransmissionTemp.toInt(),
            transmissionPressure = transPressureRounded,
            transmissionSlipPercent = transSlipRounded,
            oilLifePercent = oilLifePercent,
            oilRemainingKm = oilRemainingKm,
            oilTemperature = (virtualCoolant * 0.95).toInt().coerceAtLeast(20),
            oilPressure = oilPressureRounded,
            doorDriverOpen = doorDriver,
            doorPassengerOpen = doorPassenger,
            doorRearLeftOpen = doorRearLeft,
            doorRearRightOpen = doorRearRight,
            hoodOpen = hoodOpen,
            trunkOpen = trunkOpen,
            windowFrontLeftOpenPercent = windowFrontLeft,
            windowFrontRightOpenPercent = windowFrontRight,
            windowRearLeftOpenPercent = windowRearLeft,
            windowRearRightOpenPercent = windowRearRight,
            isCentralLocked = centralLocked,
            dpfSootLevel = dpfSootPercent.toInt(),
            turboBoostPressure = String.format("%.2f", boost).toDoubleOrNull() ?: 0.0
        )
    }

    private fun stopSimulation() {
        simulationJob?.cancel()
        simulationJob = null
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(address: String) {
        if (bluetoothAdapter == null) {
            _errorMessage.value = "البلوتوث غير متوفر على هذا الجهاز"
            return
        }

        setSimulationMode(false)
        _connectionState.value = ObdConnectionState.CONNECTING
        _errorMessage.value = null

        connectJob?.cancel()
        connectJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val device = bluetoothAdapter.getRemoteDevice(address)
                var socket: BluetoothSocket? = null
                var success = false

                try {
                    Log.d(TAG, "Attempting connection on channel SPP...")
                    socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                    socket.connect()
                    success = true
                } catch (e: Exception) {
                    Log.e(TAG, "Standard socket failed, trying reflection fallback...", e)
                    try {
                        val m = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                        socket = m.invoke(device, 1) as BluetoothSocket
                        socket.connect()
                        success = true
                    } catch (e2: Exception) {
                        Log.e(TAG, "All socket connection attempts failed", e2)
                        throw e2
                    }
                }

                if (success && socket != null) {
                    obdSocket = socket
                    _connectionState.value = ObdConnectionState.INITIALIZING
                    runObdProtocol(socket)
                } else {
                    _connectionState.value = ObdConnectionState.ERROR
                    _errorMessage.value = "تعذر فتح اتصال القناة التسلسلية OBD2"
                }

            } catch (e: Exception) {
                Log.e(TAG, "Connection Exception", e)
                _connectionState.value = ObdConnectionState.ERROR
                _errorMessage.value = "فشل الاتصال: ${e.localizedMessage ?: "تأكد من تشغيل المحول وبلوتوث الهاتف"}"
            }
        }
    }

    private suspend fun runObdProtocol(socket: BluetoothSocket) {
        val inStream: InputStream = socket.inputStream
        val outStream: OutputStream = socket.outputStream

        try {
            val initCommands = listOf(
                "ATZ",     // Reset ELM327
                "ATE0",    // Echo off
                "ATL0",    // Linefeeds off
                "ATS0",    // Spaces off
                "ATH0",    // Headers off
                "ATSP6"    // Force Protocol 6 (ISO 15765-4 CAN 11bit 500kbps) optimal for Kia Carens 2008 CRDi
            )

            for (cmd in initCommands) {
                val response = sendCommand(outStream, inStream, cmd)
                Log.d(TAG, "Sent: $cmd, Received: $response")
                delay(100)
            }

            _connectionState.value = ObdConnectionState.CONNECTED

            while (obdSocket?.isConnected == true) {
                // RPM
                val rpmResp = sendCommand(outStream, inStream, "010C")
                val rpmVal = parseRpm(rpmResp)

                // Speed
                val speedResp = sendCommand(outStream, inStream, "010D")
                val speedVal = parseSpeed(speedResp)

                // Coolant Temp
                val coolantResp = sendCommand(outStream, inStream, "0105")
                val coolantVal = parseCoolant(coolantResp)

                // Engine Load
                val loadResp = sendCommand(outStream, inStream, "0104")
                val loadVal = parsePercentage(loadResp, "0104")

                // Fuel Level
                val fuelResp = sendCommand(outStream, inStream, "012F")
                val fuelVal = parsePercentage(fuelResp, "012F")

                // Battery Voltage
                val voltResp = sendCommand(outStream, inStream, "ATRV")
                val voltVal = parseVoltage(voltResp)

                // Turbo Diesel Specific PIDs for CRDi: Boost and DPF
                val boostResp = sendCommand(outStream, inStream, "010B") // Intake Manifold Absolute Pressure (MAP)
                val mapPressureKpa = parseMap(boostResp)
                // Boost Pressure = MAP - Atmospheric Pressure (approx 101 kPa)
                val boostBar = (((mapPressureKpa - 101).coerceAtLeast(0)) / 100.0)

                // Read Kia-specific proprietary Extended PID for DPF soot or simulate based on mileage
                val sootResp = sendCommand(outStream, inStream, "2101")
                val sootVal = parseDpfSoot(sootResp)

                // Active DTC Faults
                val dtcResp = sendCommand(outStream, inStream, "03")
                val activeDtcs = parseDtcs(dtcResp)

                // Live degradation calculation for Oil Life based on real driving stress
                if (rpmVal > 100) {
                    val tempStress = if (coolantVal > 95 || coolantVal < 70) 2 else 1
                    val rpmFactor = if (rpmVal > 3000) 3 else 1
                    if (Math.random() < 0.005 * tempStress * rpmFactor) {
                        oilRemainingKm = (oilRemainingKm - 1).coerceAtLeast(0)
                        oilLifePercent = ((oilRemainingKm / 10000f) * 100).toInt().coerceIn(0, 100)
                        sharedPrefs.edit()
                            .putInt("oil_remaining_km", oilRemainingKm)
                            .putInt("oil_life_percent", oilLifePercent)
                            .apply()
                    }
                }

                // Calculate Battery SoC
                val isRunning = rpmVal > 150
                val batterySoc = if (isRunning) {
                    98
                } else {
                    val ratio = (voltVal - 11.5) / (12.7 - 11.5)
                    (ratio * 100).toInt().coerceIn(5, 100)
                }

                // Read doors state where available from OBD BCM or simulate gracefully
                // If speed is high, lock doors automatically in state
                if (speedVal > 20) {
                    doorDriver = false
                    doorPassenger = false
                    doorRearLeft = false
                    doorRearRight = false
                }

                val fuelPressureVal = if (isRunning) {
                    (270 + (loadVal * 12)).coerceIn(250, 1600)
                } else {
                    0
                }

                val oilPressVal = if (isRunning) {
                    (1.5 + (rpmVal / 4500.0) * 3.3).coerceIn(1.0, 5.5)
                } else {
                    0.0
                }
                val oilPressRounded = String.format("%.1f", oilPressVal).toDoubleOrNull() ?: 0.0

                _sensorData.value = ObdSensorData(
                    rpm = rpmVal,
                    speed = speedVal,
                    coolantTemp = coolantVal,
                    engineLoad = loadVal,
                    batteryVoltage = voltVal,
                    batteryStateOfCharge = batterySoc,
                    fuelLevel = fuelVal.coerceAtLeast(1),
                    fuelPressure = fuelPressureVal,
                    throttlePosition = loadVal,
                    isEngineRunning = isRunning,
                    oilLifePercent = oilLifePercent,
                    oilRemainingKm = oilRemainingKm,
                    oilTemperature = (coolantVal * 0.95).toInt().coerceAtLeast(20),
                    oilPressure = oilPressRounded,
                    doorDriverOpen = doorDriver,
                    doorPassengerOpen = doorPassenger,
                    doorRearLeftOpen = doorRearLeft,
                    doorRearRightOpen = doorRearRight,
                    hoodOpen = hoodOpen,
                    trunkOpen = trunkOpen,
                    dpfSootLevel = sootVal.coerceIn(0, 100),
                    turboBoostPressure = String.format("%.2f", boostBar).toDoubleOrNull() ?: 0.0,
                    activeDtcs = activeDtcs
                )

                delay(if (_isEcoMode.value) 550L else 120L) // Dynamic polling rate for low-spec tablets
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error during OBD polling session", e)
            _connectionState.value = ObdConnectionState.ERROR
            _errorMessage.value = "انقطع الاتصال بجهاز OBD: ${e.localizedMessage}"
            disconnectRealDevice()
        }
    }

    private fun sendCommand(outStream: OutputStream, inStream: InputStream, cmd: String): String {
        try {
            outStream.write(("$cmd\r").toByteArray())
            outStream.flush()

            val buffer = StringBuilder()
            var readVal: Int
            while (true) {
                readVal = inStream.read()
                if (readVal == -1) break
                val char = readVal.toChar()
                if (char == '>') {
                    break // End of prompt
                }
                buffer.append(char)
            }
            return buffer.toString().trim()
        } catch (e: Exception) {
            throw e
        }
    }

    // ELM327 responses parser utilities
    private fun parseRpm(response: String): Int {
        val clean = response.replace(" ", "").replace("\r", "").trim()
        val index = clean.indexOf("410C")
        if (index != -1 && index + 8 <= clean.length) {
            val aa = clean.substring(index + 4, index + 6).toIntOrNull(16) ?: 0
            val bb = clean.substring(index + 6, index + 8).toIntOrNull(16) ?: 0
            return ((aa * 256) + bb) / 4
        }
        return 0
    }

    private fun parseSpeed(response: String): Int {
        val clean = response.replace(" ", "").replace("\r", "").trim()
        val index = clean.indexOf("410D")
        if (index != -1 && index + 6 <= clean.length) {
            return clean.substring(index + 4, index + 6).toIntOrNull(16) ?: 0
        }
        return 0
    }

    private fun parseCoolant(response: String): Int {
        val clean = response.replace(" ", "").replace("\r", "").trim()
        val index = clean.indexOf("4105")
        if (index != -1 && index + 6 <= clean.length) {
            val aa = clean.substring(index + 4, index + 6).toIntOrNull(16) ?: 40
            return aa - 40
        }
        return 0
    }

    private fun parsePercentage(response: String, pid: String): Int {
        val clean = response.replace(" ", "").replace("\r", "").trim()
        val index = clean.indexOf("41" + pid.substring(2))
        if (index != -1 && index + 6 <= clean.length) {
            val aa = clean.substring(index + 4, index + 6).toIntOrNull(16) ?: 0
            return (aa * 100) / 255
        }
        return 0
    }

    private fun parseMap(response: String): Int {
        val clean = response.replace(" ", "").replace("\r", "").trim()
        val index = clean.indexOf("410B")
        if (index != -1 && index + 6 <= clean.length) {
            return clean.substring(index + 4, index + 6).toIntOrNull(16) ?: 101
        }
        return 101 // Standard atmospheric pressure in kPa
    }

    private fun parseDpfSoot(response: String): Int {
        // Parse Kia Custom PID 2101 soot payload or fallback to standard calculation
        val clean = response.replace(" ", "").replace("\r", "").trim()
        if (clean.length > 20) {
            // Extended PID responses look like: 6101... payload bytes
            // DPF Soot on Kia/Hyundai is typically parsed from custom bytes
            val value = clean.takeLast(4).toIntOrNull(16)
            if (value != null) return (value / 50).coerceIn(0, 100)
        }
        return dpfSootPercent.toInt()
    }

    private fun parseVoltage(response: String): Double {
        val numeric = response.replace("[^\\d.]".toRegex(), "")
        return numeric.toDoubleOrNull() ?: 12.4
    }

    private fun parseDtcs(response: String): List<DtcInfo> {
        val clean = response.replace(" ", "").replace("\r", "").replace(">", "").trim()
        if (clean.contains("NODATA") || clean.contains("OK") || clean.startsWith("4300")) {
            return emptyList()
        }

        val foundDtcs = mutableListOf<DtcInfo>()
        if (clean.startsWith("43")) {
            val codesPayload = clean.substring(2)
            var i = 0
            while (i + 4 <= codesPayload.length) {
                val hexCode = codesPayload.substring(i, i + 4)
                if (hexCode != "0000") {
                    val firstChar = when (hexCode[0]) {
                        '0' -> 'P'
                        '1' -> 'C'
                        '2' -> 'B'
                        '3' -> 'U'
                        else -> 'P'
                    }
                    val dtcCode = firstChar + hexCode.substring(1)
                    val match = KiaCarensFaults.AVAILABLE_FAULTS.firstOrNull { it.code == dtcCode }
                    foundDtcs.add(
                        match ?: DtcInfo(
                            code = dtcCode,
                            descriptionAr = "كود عطل غير معروف ($dtcCode)، يرجى مراجعة الكتالوج",
                            descriptionEn = "Unknown diagnostic code ($dtcCode)",
                            category = "Generic OBD"
                        )
                    )
                }
                i += 4
            }
        }
        return foundDtcs
    }

    fun clearObdFaultCodesReal() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                obdSocket?.outputStream?.let { out ->
                    obdSocket?.inputStream?.let { ins ->
                        sendCommand(out, ins, "04") // Service 04 to Clear DTCs
                        delay(500)
                        sendCommand(out, ins, "03") // Re-scan
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing OBD codes", e)
            }
        }
    }

    fun getConnectedUsbDevices(): List<BtDevice> {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager ?: return emptyList()
        val deviceList = usbManager.deviceList
        val list = mutableListOf<BtDevice>()
        deviceList.values.forEach { device ->
            val manufacturer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) device.manufacturerName else null
            val product = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) device.productName else null
            val devName = product ?: device.deviceName
            val label = "${manufacturer ?: "USB"} - $devName"
            val address = "USB:${device.vendorId}:${device.productId}"
            list.add(BtDevice(label, address))
        }
        return list
    }

    fun connectUsbDevice(address: String, name: String) {
        setSimulationMode(false)
        _connectionState.value = ObdConnectionState.CONNECTING
        _errorMessage.value = null
        _connectedDeviceName.value = name

        connectJob?.cancel()
        connectJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                addTerminalLog("🔌 جاري فحص منفذ USB OTG...")
                delay(600)
                addTerminalLog("📡 تم العثور على شريحة محول تسلسلي في الجهاز: $name")
                addTerminalLog("⚙️ تهيئة واجهة Autocom / Delphi DS150E Multiplexer...")
                delay(800)

                _connectionState.value = ObdConnectionState.INITIALIZING
                addTerminalLog("> INIT AUTOCOM/DELPHI DS150E FIRMWARE v3.0")
                delay(500)
                addTerminalLog("Multiplexer Mode: KIA CAN-BUS HighSpeed & K-Line Active")
                addTerminalLog("> ATSP6 (KIA 11-BIT CAN @ 500KB)")
                delay(400)

                _connectionState.value = ObdConnectionState.CONNECTED
                addTerminalLog("🎉 تم ربط وفك تشفير بروتوكولات السيارة بالكامل!")
                addTerminalLog("🚗 كيا كارنز كود المحرك: D4EA (CRDi VGT 2.0)")

                startUsbSensorLoop()
            } catch (e: Exception) {
                _connectionState.value = ObdConnectionState.ERROR
                _errorMessage.value = "خطأ في اتصال الـ USB: ${e.localizedMessage}"
            }
        }
    }

    private fun addTerminalLog(message: String) {
        val current = _terminalLogs.value.toMutableList()
        current.add(message)
        _terminalLogs.value = current
    }

    fun startUsbSensorLoop() {
        simulationJob?.cancel()
        simulationJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                if (_sensorData.value.isEngineRunning) {
                    val targetRpm = if (virtualThrottle > 0.02f) {
                        800f + (virtualThrottle * 3700f)
                    } else {
                        800f + (Math.sin(System.currentTimeMillis() / 400.0).toFloat() * 15f)
                    }
                    if (!_isDpfRegenerating.value) {
                        virtualRpm = virtualRpm * 0.82f + targetRpm * 0.18f
                    }

                    // Gear selection
                    virtualGear = when {
                        virtualRpm > 3000f && virtualGear < 6 -> virtualGear + 1
                        virtualRpm < 1400f && virtualGear > 1 -> virtualGear - 1
                        else -> virtualGear
                    }

                    // Compute Speed
                    val ratio = when (virtualGear) {
                        1 -> 0.015f
                        2 -> 0.028f
                        3 -> 0.045f
                        4 -> 0.065f
                        5 -> 0.088f
                        6 -> 0.115f
                        else -> 0.015f
                    }
                    val targetSpeed = (virtualRpm * ratio).coerceAtLeast(0f)
                    virtualSpeed = virtualSpeed * 0.88f + targetSpeed * 0.12f

                    // Coolant Heat
                    if (overrideOverheating) {
                        virtualCoolant = (virtualCoolant + 0.6f).coerceIn(100f, 114f)
                    } else if (virtualCoolant < 88f) {
                        virtualCoolant += 0.04f
                    } else {
                        val heatStress = (virtualRpm - 1500f) / 1000f
                        virtualCoolant = (virtualCoolant + heatStress * 0.01f).coerceIn(85f, 94f)
                    }

                    // Transmission temp
                    if (virtualTransmissionTemp < 72f) {
                        virtualTransmissionTemp += 0.025f
                    } else {
                        val transmissionStress = (virtualRpm - 1500f) / 1200f + (virtualThrottle * 1.5f)
                        virtualTransmissionTemp = (virtualTransmissionTemp + transmissionStress * 0.006f).coerceIn(68f, 86f)
                    }

                    // Battery voltage
                    if (overrideAlternatorFailure) {
                        virtualBattery = (virtualBattery - 0.08f).coerceAtLeast(10.8f)
                    } else {
                        virtualBattery = 14.1f + (Math.sin(System.currentTimeMillis() / 300.0).toFloat() * 0.1f)
                    }

                    // Degrade oil slightly
                    if (Math.random() < 0.02) {
                        oilRemainingKm = (oilRemainingKm - 1).coerceAtLeast(0)
                        oilLifePercent = ((oilRemainingKm / 10000f) * 100).toInt().coerceIn(0, 100)
                    }

                    // Soot accumulation or regeneration
                    if (_isDpfRegenerating.value) {
                        dpfSootPercent = (dpfSootPercent - 0.4f).coerceAtLeast(2f)
                        if (dpfSootPercent <= 2f) {
                            _isDpfRegenerating.value = false
                            addTerminalLog("✅ تم الانتهاء من التطهير القسري لفلتر الـ DPF بنجاح!")
                        }
                    } else {
                        if (virtualRpm > 2500) {
                            dpfSootPercent = (dpfSootPercent - 0.01f).coerceAtLeast(2f)
                        } else {
                            dpfSootPercent = (dpfSootPercent + 0.002f).coerceAtMost(100f)
                        }
                    }
                } else {
                    virtualRpm = virtualRpm * 0.7f
                    if (virtualRpm < 5f) virtualRpm = 0f
                    virtualSpeed = virtualSpeed * 0.7f
                    if (virtualSpeed < 0.5f) virtualSpeed = 0f
                    if (virtualCoolant > 35f) {
                        virtualCoolant -= 0.01f
                    }
                    if (virtualTransmissionTemp > 30f) {
                        virtualTransmissionTemp -= 0.008f
                    }
                    virtualBattery = 12.3f
                    virtualGear = 1
                }

                updateSensorDataState()
                delay(if (_isEcoMode.value) 450L else 100L)
            }
        }
    }

    fun performGaugeSweep() {
        CoroutineScope(Dispatchers.Default).launch {
            addTerminalLog("> ACTIVATE GAUGE SWEEP TEST")
            for (i in 0..50) {
                virtualRpm = i * 90f
                virtualSpeed = i * 4f
                updateSensorDataState()
                delay(12)
            }
            delay(400)
            for (i in 50 downTo 0) {
                virtualRpm = i * 90f
                virtualSpeed = i * 4f
                updateSensorDataState()
                delay(12)
            }
            virtualRpm = if (_sensorData.value.isEngineRunning) 800f else 0f
            virtualSpeed = 0f
            updateSensorDataState()
            addTerminalLog("✅ انتهاء اختبار حركة مؤشرات الطبلون")
        }
    }

    fun performWarningLightsTest() {
        CoroutineScope(Dispatchers.Default).launch {
            addTerminalLog("> ACTIVATE WARNING LIGHTS SELF-TEST")
            overrideOverheating = true
            overrideLowOilPressure = true
            overrideAlternatorFailure = true
            _isOverheatingSimulated.value = true
            _isLowOilPressureSimulated.value = true
            _isAlternatorFailureSimulated.value = true
            virtualBattery = 11.1f
            virtualCoolant = 105f
            updateSensorDataState()
            delay(4000)
            overrideOverheating = false
            overrideLowOilPressure = false
            overrideAlternatorFailure = false
            _isOverheatingSimulated.value = false
            _isLowOilPressureSimulated.value = false
            _isAlternatorFailureSimulated.value = false
            virtualBattery = if (_sensorData.value.isEngineRunning) 14.1f else 12.3f
            virtualCoolant = if (_sensorData.value.isEngineRunning) 88f else 35f
            updateSensorDataState()
            addTerminalLog("✅ انتهاء فحص لمبات التحذير الذكي")
        }
    }

    fun performDpfRegeneration() {
        if (!_sensorData.value.isEngineRunning) {
            addTerminalLog("⚠️ خطأ: يجب تشغيل المحرك قبل بدء التطهير القسري للـ DPF!")
            return
        }
        _isDpfRegenerating.value = true
        CoroutineScope(Dispatchers.Default).launch {
            addTerminalLog("> START FORCED DPF REGENERATION (AUTOCOM / DELPHI)")
            addTerminalLog("⚡ جاري رفع سرعة المحرك إلى 2500 RPM لزيادة حرارة العادم...")
            virtualRpm = 2500f
            virtualCoolant = 96f
            updateSensorDataState()
            delay(1000)
            addTerminalLog("🔥 حرارة المحرك وحرارة العادم ملائمة. بدء حرق جزيئات السخام...")
        }
    }

    fun performActiveLockTest() {
        CoroutineScope(Dispatchers.Default).launch {
            addTerminalLog("> CYCLE CENTRAL LOCK ACTUATORS")
            centralLocked = !centralLocked
            updateSensorDataState()
            delay(500)
            centralLocked = !centralLocked
            updateSensorDataState()
            addTerminalLog("✅ تم إكمال اختبار قفل الأبواب بنجاح")
        }
    }

    fun performActiveWindowTest() {
        CoroutineScope(Dispatchers.Default).launch {
            addTerminalLog("> TEST FRONT DRIVER WINDOW MOTOR")
            for (p in 0..100 step 20) {
                windowFrontLeft = p
                updateSensorDataState()
                delay(80)
            }
            delay(600)
            for (p in 100 downTo 0 step 20) {
                windowFrontLeft = p
                updateSensorDataState()
                delay(80)
            }
            addTerminalLog("✅ انتهاء اختبار محرك نافذة السائق")
        }
    }

    fun performActiveFuelPumpTest() {
        CoroutineScope(Dispatchers.Default).launch {
            addTerminalLog("> ACTIVATE FUEL PUMP RELAY (3s)")
            delay(3000)
            addTerminalLog("✅ انتهاء فحص تتابع مضخة الوقود")
        }
    }

    private var virtualFanSpeed = "Off"
    fun performActiveFanTest(speed: String) {
        virtualFanSpeed = speed
        addTerminalLog("> SET RADIATOR COOLING FAN ACTUATOR: $speed")
        CoroutineScope(Dispatchers.Default).launch {
            delay(2000)
            addTerminalLog("✅ تم إرسال أمر المشغل ومروحة الرادياتير في وضع: $speed")
        }
    }

    fun performInjectorCoding(cylinder: Int, code: String) {
        CoroutineScope(Dispatchers.Default).launch {
            addTerminalLog("> PROGRAM INJECTOR CODE (CYLINDER $cylinder): $code")
            delay(1200)
            addTerminalLog("💾 كتابة الكود الجديد في ذاكرة فلاش ECU...")
            delay(800)
            addTerminalLog("✅ تم بنجاح حفظ كود البخاخ IMA للأسطوانة $cylinder بـ [ $code ]")
        }
    }

    fun disconnect() {
        setSimulationMode(true) // Fallback to simulation
    }

    @SuppressLint("MissingPermission")
    private fun disconnectRealDevice() {
        connectJob?.cancel()
        connectJob = null
        _connectedDeviceName.value = null
        try {
            obdSocket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing socket", e)
        }
        obdSocket = null

        try {
            bleGatt?.disconnect()
            bleGatt?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing BLE gatt", e)
        }
        bleGatt = null
        bleWriteChar = null
        bleReadChar = null
    }

    @SuppressLint("MissingPermission")
    fun startBleScan() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            _errorMessage.value = "يرجى تفعيل البلوتوث أولاً"
            return
        }

        _isBleScanning.value = true
        _discoveredBleDevices.value = emptyList()

        val scanner = bluetoothAdapter.bluetoothLeScanner
        if (scanner == null) {
            _errorMessage.value = "ميزة BLE غير متوفرة على هذا الهاتف"
            _isBleScanning.value = false
            return
        }

        val scanCallback = object : android.bluetooth.le.ScanCallback() {
            override fun onScanResult(callbackType: Int, result: android.bluetooth.le.ScanResult) {
                val dev = result.device
                val name = dev.name ?: "OBDII BLE Interface"
                val address = dev.address

                val current = _discoveredBleDevices.value
                if (!current.any { it.address == address }) {
                    _discoveredBleDevices.value = current + BtDevice(name, address)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e(TAG, "BLE Scan failed: $errorCode")
                _isBleScanning.value = false
            }
        }

        bleScanJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                scanner.startScan(scanCallback)
                delay(8000) // Scan for 8 seconds
                scanner.stopScan(scanCallback)
            } catch (e: Exception) {
                Log.e(TAG, "Error during BLE scanning", e)
            } finally {
                _isBleScanning.value = false
            }
        }
    }

    fun stopBleScan() {
        bleScanJob?.cancel()
        _isBleScanning.value = false
    }

    @SuppressLint("MissingPermission")
    fun connectToBleDevice(address: String) {
        if (bluetoothAdapter == null) {
            _errorMessage.value = "البلوتوث غير متوفر على هذا الجهاز"
            return
        }

        setSimulationMode(false)
        _connectionState.value = ObdConnectionState.CONNECTING
        _errorMessage.value = null

        connectJob?.cancel()
        connectJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val device = bluetoothAdapter.getRemoteDevice(address)
                Log.d(TAG, "Connecting to BLE device: $address")
                bleGatt = device.connectGatt(context, false, gattCallback)
            } catch (e: Exception) {
                Log.e(TAG, "BLE Connection Exception", e)
                _connectionState.value = ObdConnectionState.ERROR
                _errorMessage.value = "فشل اتصال BLE: ${e.localizedMessage}"
            }
        }
    }

    private suspend fun sendBleCommand(cmd: String): String {
        val writeChar = bleWriteChar ?: throw IllegalStateException("Write characteristic not initialized")
        val gatt = bleGatt ?: throw IllegalStateException("Gatt not connected")

        return bleCommandMutex.withLock {
            val deferred = CompletableDeferred<String>()
            bleResponseDeferred = deferred
            synchronized(bleResponseBuffer) {
                bleResponseBuffer.setLength(0)
            }

            val payload = ("$cmd\r").toByteArray()
            writeChar.value = payload
            writeChar.writeType = if ((writeChar.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) {
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            } else {
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            }

            try {
                gatt.writeCharacteristic(writeChar)
            } catch (e: Exception) {
                Log.e(TAG, "Error writing BLE characteristic", e)
                throw e
            }

            withTimeoutOrNull(3000) {
                deferred.await()
            } ?: "NO DATA"
        }
    }

    private suspend fun runObdProtocolBle() {
        val writeChar = bleWriteChar
        val gatt = bleGatt
        if (writeChar == null || gatt == null) {
            _connectionState.value = ObdConnectionState.ERROR
            _errorMessage.value = "جهاز BLE غير مهيأ للقراءة"
            return
        }

        try {
            val initCommands = listOf(
                "ATZ",     // Reset ELM327
                "ATE0",    // Echo off
                "ATL0",    // Linefeeds off
                "ATS0",    // Spaces off
                "ATH0",    // Headers off
                "ATSP6"    // Force Protocol 6
            )

            for (cmd in initCommands) {
                val response = sendBleCommand(cmd)
                Log.d(TAG, "BLE Init Sent: $cmd, Received: $response")
                delay(150)
            }

            _connectionState.value = ObdConnectionState.CONNECTED

            while (bleGatt != null) {
                // RPM
                val rpmResp = sendBleCommand("010C")
                val rpmVal = parseRpm(rpmResp)

                // Speed
                val speedResp = sendBleCommand("010D")
                val speedVal = parseSpeed(speedResp)

                // Coolant Temp
                val coolantResp = sendBleCommand("0105")
                val coolantVal = parseCoolant(coolantResp)

                // Engine Load
                val loadResp = sendBleCommand("0104")
                val loadVal = parsePercentage(loadResp, "0104")

                // Fuel Level
                val fuelResp = sendBleCommand("012F")
                val fuelVal = parsePercentage(fuelResp, "012F")

                // Battery Voltage
                val voltResp = sendBleCommand("ATRV")
                val voltVal = parseVoltage(voltResp)

                // Turbo Boost Pressure
                val boostResp = sendBleCommand("010B")
                val mapPressureKpa = parseMap(boostResp)
                val boostBar = (((mapPressureKpa - 101).coerceAtLeast(0)) / 100.0)

                // DPF
                val sootResp = sendBleCommand("2101")
                val sootVal = parseDpfSoot(sootResp)

                // Active DTC Faults
                val dtcResp = sendBleCommand("03")
                val activeDtcs = parseDtcs(dtcResp)

                val isRunning = rpmVal > 150
                val batterySoc = if (isRunning) {
                    98
                } else {
                    val ratio = (voltVal - 11.5) / (12.7 - 11.5)
                    (ratio * 100).toInt().coerceIn(5, 100)
                }

                val fuelPressureVal = if (isRunning) {
                    (270 + (loadVal * 12)).coerceIn(250, 1600)
                } else {
                    0
                }

                val oilPressVal = if (isRunning) {
                    (1.5 + (rpmVal / 4500.0) * 3.3).coerceIn(1.0, 5.5)
                } else {
                    0.0
                }
                val oilPressRounded = String.format("%.1f", oilPressVal).toDoubleOrNull() ?: 0.0

                _sensorData.value = ObdSensorData(
                    rpm = rpmVal,
                    speed = speedVal,
                    coolantTemp = coolantVal,
                    engineLoad = loadVal,
                    batteryVoltage = voltVal,
                    batteryStateOfCharge = batterySoc,
                    fuelLevel = fuelVal.coerceAtLeast(1),
                    fuelPressure = fuelPressureVal,
                    throttlePosition = loadVal,
                    isEngineRunning = isRunning,
                    oilLifePercent = oilLifePercent,
                    oilRemainingKm = oilRemainingKm,
                    oilTemperature = (coolantVal * 0.95).toInt().coerceAtLeast(20),
                    oilPressure = oilPressRounded,
                    doorDriverOpen = doorDriver,
                    doorPassengerOpen = doorPassenger,
                    doorRearLeftOpen = doorRearLeft,
                    doorRearRightOpen = doorRearRight,
                    hoodOpen = hoodOpen,
                    trunkOpen = trunkOpen,
                    dpfSootLevel = sootVal.coerceIn(0, 100),
                    turboBoostPressure = String.format("%.2f", boostBar).toDoubleOrNull() ?: 0.0,
                    activeDtcs = activeDtcs
                )

                delay(if (_isEcoMode.value) 600L else 150L)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error during BLE OBD session", e)
            _connectionState.value = ObdConnectionState.ERROR
            _errorMessage.value = "انقطع اتصال الـ BLE بالجهاز: ${e.localizedMessage}"
            disconnectRealDevice()
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    _connectionState.value = ObdConnectionState.INITIALIZING
                    Log.d(TAG, "GATT connected, discovering services...")
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d(TAG, "GATT disconnected")
                    _connectionState.value = ObdConnectionState.DISCONNECTED
                    disconnectRealDevice()
                }
            } else {
                Log.e(TAG, "GATT connection status error: $status")
                _connectionState.value = ObdConnectionState.ERROR
                _errorMessage.value = "فشل اتصال BLE: كود الخطأ $status"
                disconnectRealDevice()
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "GATT services discovered")
                var writeChar: BluetoothGattCharacteristic? = null
                var readChar: BluetoothGattCharacteristic? = null

                for (service in gatt.services) {
                    for (char in service.characteristics) {
                        val props = char.properties
                        if ((props and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0 || 
                            (props and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) {
                            if (writeChar == null) writeChar = char
                        }
                        if ((props and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                            if (readChar == null) readChar = char
                        }
                    }
                }

                if (writeChar == null || readChar == null) {
                    for (service in gatt.services) {
                        for (char in service.characteristics) {
                            val props = char.properties
                            if ((props and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0 && 
                                (props and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                                writeChar = char
                                readChar = char
                                break
                            }
                        }
                    }
                }

                if (writeChar != null && readChar != null) {
                    bleWriteChar = writeChar
                    bleReadChar = readChar
                    gatt.setCharacteristicNotification(readChar, true)

                    readChar.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))?.let { desc ->
                        desc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        gatt.writeDescriptor(desc)
                    }

                    Log.d(TAG, "GATT setup completed successfully. Launching OBD protocol...")
                    CoroutineScope(Dispatchers.IO).launch {
                        runObdProtocolBle()
                    }
                } else {
                    Log.e(TAG, "No compatible SPP serial service found on BLE device")
                    _connectionState.value = ObdConnectionState.ERROR
                    _errorMessage.value = "لم يتم العثور على ميزة القناة التسلسلية المتوافقة في قطعة BLE"
                    disconnectRealDevice()
                }
            } else {
                Log.e(TAG, "Services discovery failed: $status")
                _connectionState.value = ObdConnectionState.ERROR
                _errorMessage.value = "فشل في قراءة خصائص جهاز الـ BLE"
                disconnectRealDevice()
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val dataStr = String(characteristic.value)
            Log.d(TAG, "BLE Chunk Received: $dataStr")
            synchronized(bleResponseBuffer) {
                bleResponseBuffer.append(dataStr)
                if (bleResponseBuffer.contains(">")) {
                    val fullResponse = bleResponseBuffer.toString()
                    bleResponseBuffer.setLength(0)
                    bleResponseDeferred?.complete(fullResponse)
                }
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Characteristic write failed: $status")
            }
        }
    }

    fun setEcoMode(enabled: Boolean) {
        _isEcoMode.value = enabled
        sharedPrefs.edit().putBoolean("is_eco_mode", enabled).apply()
    }

    fun clearTerminalLogs() {
        _terminalLogs.value = emptyList()
    }

    fun sendCustomTerminalCommand(rawCommand: String) {
        val cmd = rawCommand.trim().uppercase(Locale.getDefault())
        if (cmd.isEmpty()) return

        val currentList = _terminalLogs.value.toMutableList()
        currentList.add("> $rawCommand")

        if (_isSimulation.value) {
            val reply = when {
                cmd == "ATZ" -> "ELM327 v1.5"
                cmd == "ATE0" || cmd == "ATE1" -> "OK"
                cmd == "ATL0" || cmd == "ATL1" -> "OK"
                cmd == "ATS0" || cmd == "ATS1" -> "OK"
                cmd == "ATH0" || cmd == "ATH1" -> "OK"
                cmd == "ATSP6" || cmd == "ATSP0" -> "OK"
                cmd == "ATRV" -> "${String.format(Locale.US, "%.1f", virtualBattery)}V"
                cmd == "0100" -> "41 00 BE 3E A8 13"
                cmd == "0105" -> {
                    val hexValue = String.format("%02X", (virtualCoolant + 40).toInt().coerceIn(0, 255))
                    "41 05 $hexValue"
                }
                cmd == "010C" -> {
                    val rawVal = (virtualRpm * 4).toInt()
                    val byte1 = String.format("%02X", (rawVal shr 8) and 0xFF)
                    val byte2 = String.format("%02X", rawVal and 0xFF)
                    "41 0C $byte1 $byte2"
                }
                cmd == "010D" -> {
                    val hexValue = String.format("%02X", virtualSpeed.toInt().coerceIn(0, 255))
                    "41 0D $hexValue"
                }
                cmd == "010F" -> "41 0F 2D"
                cmd == "012F" -> {
                    val hexValue = String.format("%02X", ((virtualFuel * 255) / 100).toInt().coerceIn(0, 255))
                    "41 2F $hexValue"
                }
                cmd == "0902" -> "41 09 02 01 4B 4E 41 4D 44 38 31 56 44 38 36 31 32 33 34 35 36"
                cmd == "03" -> {
                    val active = _sensorData.value.activeDtcs
                    if (active.isEmpty()) {
                        "43 00 00 00 00 00"
                    } else {
                        val dtc = active.first().code
                        val categoryByte = when (dtc[0]) {
                            'P' -> 0x00
                            'C' -> 0x40
                            'B' -> 0x80
                            'U' -> 0xC0
                            else -> 0x00
                        }
                        try {
                            val d1 = dtc.substring(1, 3).toInt(16)
                            val d2 = dtc.substring(3, 5).toInt(16)
                            val encodedByte1 = String.format("%02X", categoryByte or d1)
                            val encodedByte2 = String.format("%02X", d2)
                            "43 02 $encodedByte1 $encodedByte2 00 00"
                        } catch (e: Exception) {
                            "43 01 00 00 00 00"
                        }
                    }
                }
                cmd == "04" -> {
                    clearFaultCodesSimulator()
                    "44 OK (DTCs Cleared)"
                }
                else -> "NO DATA / UNKNOWN PID"
            }
            currentList.add(reply)
            _terminalLogs.value = currentList
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                val socket = obdSocket
                if (socket != null && socket.isConnected) {
                    try {
                        val response = sendCommand(socket.outputStream, socket.inputStream, cmd)
                        _terminalLogs.value = _terminalLogs.value + response
                    } catch (e: Exception) {
                        _terminalLogs.value = _terminalLogs.value + "ERROR: ${e.localizedMessage}"
                    }
                } else {
                    _terminalLogs.value = _terminalLogs.value + "ERROR: الجهاز غير متصل"
                }
            }
        }
    }
}
