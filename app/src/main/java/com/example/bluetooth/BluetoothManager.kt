package com.example.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager as AndroidBluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.Locale
import java.util.UUID

/**
 * Representation of an OBD2 Bluetooth Device.
 */
data class ObdBluetoothDevice(
    val name: String,
    val address: String,
    val isBonded: Boolean,
    val isLikelyObd: Boolean = false,
    val device: BluetoothDevice? = null
)

/**
 * Connection states for Bluetooth OBD2 communication.
 */
sealed class BluetoothConnectionState {
    object Disconnected : BluetoothConnectionState()
    object Discovering : BluetoothConnectionState()
    data class Connecting(val deviceName: String, val address: String) : BluetoothConnectionState()
    data class Connected(val deviceName: String, val address: String) : BluetoothConnectionState()
    data class Error(val message: String, val cause: Throwable? = null) : BluetoothConnectionState()
}

/**
 * Dedicated BluetoothManager handling OBD2 device discovery, connection state,
 * and RFCOMM socket communication using standard Android Bluetooth APIs.
 */
class BluetoothManager(private val context: Context) {

    companion object {
        private const val TAG = "OBD2_BluetoothManager"
        // Standard Serial Port Profile (SPP) UUID used by ELM327 and OBD2 adapters
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        // Keywords commonly associated with OBD2 interfaces
        private val OBD_KEYWORDS = listOf(
            "OBD", "ELM", "VGATE", "VIEOCAR", "CAR", "SCAN",
            "DIAG", "LINK", "AUTO", "KWP", "ISO"
        )
    }

    private val androidBtManager: AndroidBluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? AndroidBluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = androidBtManager?.adapter

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    // Connection state
    private val _connectionState = MutableStateFlow<BluetoothConnectionState>(BluetoothConnectionState.Disconnected)
    val connectionState: StateFlow<BluetoothConnectionState> = _connectionState.asStateFlow()

    // Discovery state
    private val _discoveredDevices = MutableStateFlow<List<ObdBluetoothDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<ObdBluetoothDevice>> = _discoveredDevices.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    // Log / Traffic streaming
    private val _rawTrafficFlow = MutableSharedFlow<String>(replay = 20)
    val rawTrafficFlow: SharedFlow<String> = _rawTrafficFlow.asSharedFlow()

    // Socket and communication streams
    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    private val ioMutex = Mutex()
    private var isReceiverRegistered = false

    /**
     * BroadcastReceiver for standard Android Bluetooth discovery events.
     */
    private val discoveryReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            when (action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }

                    device?.let { dev ->
                        val devName = dev.name ?: "Unknown OBD Device"
                        val devAddress = dev.address
                        val isBonded = dev.bondState == BluetoothDevice.BOND_BONDED
                        val isObd = isLikelyObdInterface(devName)

                        val obdDevice = ObdBluetoothDevice(
                            name = devName,
                            address = devAddress,
                            isBonded = isBonded,
                            isLikelyObd = isObd,
                            device = dev
                        )

                        val currentList = _discoveredDevices.value
                        if (!currentList.any { it.address.equals(devAddress, ignoreCase = true) }) {
                            Log.d(TAG, "Discovered device: $devName ($devAddress)")
                            _discoveredDevices.value = currentList + obdDevice
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    Log.d(TAG, "Bluetooth discovery started")
                    _isDiscovering.value = true
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Log.d(TAG, "Bluetooth discovery finished")
                    _isDiscovering.value = false
                }
            }
        }
    }

    /**
     * Checks if Bluetooth is supported and enabled on this device.
     */
    fun isBluetoothAvailable(): Boolean {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled
    }

    /**
     * Retrieves all currently paired (bonded) Bluetooth devices.
     */
    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<ObdBluetoothDevice> {
        if (!isBluetoothAvailable()) return emptyList()

        return try {
            val bonded = bluetoothAdapter?.bondedDevices ?: emptySet()
            bonded.map { device ->
                val name = device.name ?: "OBDII Device"
                ObdBluetoothDevice(
                    name = name,
                    address = device.address,
                    isBonded = true,
                    isLikelyObd = isLikelyObdInterface(name),
                    device = device
                )
            }.sortedWith(compareByDescending<ObdBluetoothDevice> { it.isLikelyObd }.thenBy { it.name })
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching bonded devices", e)
            emptyList()
        }
    }

    /**
     * Starts device discovery using standard Android BluetoothAdapter APIs.
     * Note: Cancels any ongoing discovery before starting a new scan.
     */
    @SuppressLint("MissingPermission")
    fun startDiscovery(): Boolean {
        if (!isBluetoothAvailable()) {
            _connectionState.value = BluetoothConnectionState.Error("Bluetooth is not enabled")
            return false
        }

        try {
            // Cancel any previous discovery in progress
            if (bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter.cancelDiscovery()
            }

            // Register discovery receiver
            if (!isReceiverRegistered) {
                val filter = IntentFilter().apply {
                    addAction(BluetoothDevice.ACTION_FOUND)
                    addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
                    addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                }
                context.registerReceiver(discoveryReceiver, filter)
                isReceiverRegistered = true
            }

            // Pre-seed with paired devices
            val paired = getPairedDevices()
            _discoveredDevices.value = paired

            val started = bluetoothAdapter?.startDiscovery() ?: false
            if (started) {
                _isDiscovering.value = true
                Log.d(TAG, "Started standard Bluetooth discovery")
            } else {
                Log.w(TAG, "Failed to start Bluetooth discovery")
            }
            return started
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting discovery", e)
            _connectionState.value = BluetoothConnectionState.Error("Discovery error: ${e.localizedMessage}", e)
            return false
        }
    }

    /**
     * Stops active device discovery.
     */
    @SuppressLint("MissingPermission")
    fun stopDiscovery() {
        try {
            if (bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter.cancelDiscovery()
            }
            _isDiscovering.value = false
            unregisterReceiverSafely()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping discovery", e)
        }
    }

    /**
     * Connects to the OBD2 device at the given MAC address via RFCOMM socket.
     * Implements standard SPP with a reflection fallback for maximum compatibility.
     */
    @SuppressLint("MissingPermission")
    suspend fun connect(address: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isBluetoothAvailable()) {
            val err = "Bluetooth is not available or disabled"
            _connectionState.value = BluetoothConnectionState.Error(err)
            return@withContext Result.failure(IllegalStateException(err))
        }

        // Cancel discovery before connecting as recommended by Android documentation
        stopDiscovery()

        // Disconnect existing socket if any
        disconnect()

        val device: BluetoothDevice = try {
            bluetoothAdapter!!.getRemoteDevice(address)
        } catch (e: Exception) {
            val err = "Invalid Bluetooth address: $address"
            _connectionState.value = BluetoothConnectionState.Error(err, e)
            return@withContext Result.failure(e)
        }

        val devName = device.name ?: address
        _connectionState.value = BluetoothConnectionState.Connecting(devName, address)
        logTraffic("Connecting to $devName ($address)...")

        ioMutex.withLock {
            var socket: BluetoothSocket? = null
            var connected = false

            // 1. Primary Attempt: Standard RFCOMM socket via standard SPP UUID
            try {
                Log.d(TAG, "Attempting standard RFCOMM socket connection on SPP UUID: $SPP_UUID")
                socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                socket.connect()
                connected = true
                Log.d(TAG, "Standard RFCOMM socket connected successfully")
            } catch (e1: IOException) {
                Log.w(TAG, "Standard RFCOMM connection failed (${e1.message}), trying reflection fallback on channel 1...")
                try {
                    socket?.close()
                } catch (_: Exception) {}

                // 2. Secondary Fallback: Direct RFCOMM channel 1 via reflection (vital for stubborn ELM327 clones)
                try {
                    val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                    socket = method.invoke(device, 1) as BluetoothSocket
                    socket.connect()
                    connected = true
                    Log.d(TAG, "Reflection socket connected successfully on RFCOMM channel 1")
                } catch (e2: Exception) {
                    Log.e(TAG, "All socket connection attempts failed", e2)
                    try {
                        socket?.close()
                    } catch (_: Exception) {}

                    val errMsg = "Failed to connect to $devName: ${e2.localizedMessage ?: e1.localizedMessage}"
                    _connectionState.value = BluetoothConnectionState.Error(errMsg, e2)
                    logTraffic("❌ $errMsg")
                    return@withContext Result.failure(e2)
                }
            }

            if (connected && socket != null && socket.isConnected) {
                bluetoothSocket = socket
                inputStream = socket.inputStream
                outputStream = socket.outputStream
                _connectionState.value = BluetoothConnectionState.Connected(devName, address)
                logTraffic("✅ Connected to $devName")
                return@withContext Result.success(Unit)
            } else {
                val err = "Socket connection did not complete"
                _connectionState.value = BluetoothConnectionState.Error(err)
                return@withContext Result.failure(IOException(err))
            }
        }
    }

    /**
     * Sends an AT or OBD command string and waits for the prompt '>' terminator.
     * Includes timeout handling to prevent indefinite thread freezing.
     */
    suspend fun sendCommand(command: String, timeoutMs: Long = 3500L): Result<String> = withContext(Dispatchers.IO) {
        ioMutex.withLock {
            val out = outputStream
            val ins = inputStream
            val socket = bluetoothSocket

            if (socket == null || !socket.isConnected || out == null || ins == null) {
                val err = "Socket is not connected"
                return@withContext Result.failure(IOException(err))
            }

            try {
                // Ensure command ends with carriage return
                val formattedCmd = command.trim() + "\r"
                logTraffic("> $command")

                // Drain any residual bytes in the stream before writing
                while (ins.available() > 0) {
                    ins.read()
                }

                out.write(formattedCmd.toByteArray(Charsets.US_ASCII))
                out.flush()

                val responseBuilder = StringBuilder()
                val startTime = System.currentTimeMillis()

                while (System.currentTimeMillis() - startTime < timeoutMs) {
                    if (ins.available() > 0) {
                        val byteRead = ins.read()
                        if (byteRead == -1) break
                        val char = byteRead.toChar()

                        // ELM327 signals completion of response with '>' prompt
                        if (char == '>') {
                            break
                        }
                        responseBuilder.append(char)
                    } else {
                        kotlinx.coroutines.delay(8)
                    }
                }

                val rawResponse = responseBuilder.toString().trim()
                logTraffic(if (rawResponse.isEmpty()) "<NO DATA>" else rawResponse)
                return@withContext Result.success(rawResponse)
            } catch (e: Exception) {
                Log.e(TAG, "Error executing command: $command", e)
                _connectionState.value = BluetoothConnectionState.Error("Communication error: ${e.localizedMessage}", e)
                return@withContext Result.failure(e)
            }
        }
    }

    /**
     * Disconnects the socket and releases I/O streams cleanly.
     */
    fun disconnect() {
        try {
            outputStream?.flush()
        } catch (_: Exception) {}

        try {
            outputStream?.close()
        } catch (_: Exception) {}

        try {
            inputStream?.close()
        } catch (_: Exception) {}

        try {
            bluetoothSocket?.close()
        } catch (_: Exception) {}

        bluetoothSocket = null
        inputStream = null
        outputStream = null

        _connectionState.value = BluetoothConnectionState.Disconnected
        logTraffic("Disconnected from OBD2 device")
    }

    /**
     * Checks if the socket is currently active and connected.
     */
    fun isConnected(): Boolean {
        return bluetoothSocket?.isConnected == true && _connectionState.value is BluetoothConnectionState.Connected
    }

    /**
     * Cleans up all resources, unregisters receivers, and shuts down the manager.
     */
    fun release() {
        stopDiscovery()
        disconnect()
    }

    private fun unregisterReceiverSafely() {
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(discoveryReceiver)
            } catch (e: Exception) {
                Log.w(TAG, "Receiver not registered or already unregistered", e)
            } finally {
                isReceiverRegistered = false
            }
        }
    }

    private fun logTraffic(message: String) {
        scope.launch {
            _rawTrafficFlow.emit(message)
        }
    }

    private fun isLikelyObdInterface(name: String): Boolean {
        val upper = name.uppercase(Locale.ROOT)
        return OBD_KEYWORDS.any { upper.contains(it) }
    }
}
