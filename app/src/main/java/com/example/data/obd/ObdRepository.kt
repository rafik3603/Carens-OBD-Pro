package com.example.data.obd

import com.example.data.db.DtcDao
import com.example.data.db.DtcRecord
import com.example.data.db.FrequentlyAccessedDtc
import com.example.data.db.FrequentlyAccessedDtcDao
import com.example.data.db.HistoricalTrendPoint
import com.example.data.db.HistoricalTrendPointDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class ObdRepository(
    private val obdManager: BluetoothObdManager,
    private val dtcDao: DtcDao,
    private val frequentlyAccessedDtcDao: FrequentlyAccessedDtcDao,
    private val historicalTrendPointDao: HistoricalTrendPointDao
) {
    val connectionState: StateFlow<ObdConnectionState> = obdManager.connectionState
    val sensorData: StateFlow<ObdSensorData> = obdManager.sensorData
    val errorMessage: StateFlow<String?> = obdManager.errorMessage
    val isSimulation: StateFlow<Boolean> = obdManager.isSimulation
    val isEcoMode: StateFlow<Boolean> = obdManager.isEcoMode
    val terminalLogs: StateFlow<List<String>> = obdManager.terminalLogs

    val isOverheatingSimulated: StateFlow<Boolean> = obdManager.isOverheatingSimulated
    val isLowOilPressureSimulated: StateFlow<Boolean> = obdManager.isLowOilPressureSimulated
    val isAlternatorFailureSimulated: StateFlow<Boolean> = obdManager.isAlternatorFailureSimulated

    fun toggleOverheating(enabled: Boolean) {
        obdManager.toggleOverheating(enabled)
    }

    fun toggleLowOilPressure(enabled: Boolean) {
        obdManager.toggleLowOilPressure(enabled)
    }

    fun toggleAlternatorFailure(enabled: Boolean) {
        obdManager.toggleAlternatorFailure(enabled)
    }

    val savedDtcHistory: Flow<List<DtcRecord>> = dtcDao.getAllDtcRecords()

    val frequentlyAccessedDtcs: Flow<List<FrequentlyAccessedDtc>> = frequentlyAccessedDtcDao.getFrequentlyAccessedDtcs(30)
    val localTrendPoints: Flow<List<HistoricalTrendPoint>> = historicalTrendPointDao.getAllTrendPoints()

    suspend fun recordDtcAccess(code: String, descAr: String, descEn: String, category: String) {
        val existing = frequentlyAccessedDtcDao.getDtcByCode(code)
        if (existing != null) {
            frequentlyAccessedDtcDao.insertFrequentlyAccessedDtc(
                existing.copy(
                    accessCount = existing.accessCount + 1,
                    lastAccessTime = System.currentTimeMillis()
                )
            )
        } else {
            frequentlyAccessedDtcDao.insertFrequentlyAccessedDtc(
                FrequentlyAccessedDtc(
                    code = code,
                    descriptionAr = descAr,
                    descriptionEn = descEn,
                    category = category,
                    accessCount = 1,
                    lastAccessTime = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun saveTrendPoint(point: HistoricalTrendPoint) {
        historicalTrendPointDao.insertTrendPoint(point)
        val count = historicalTrendPointDao.getCount()
        if (count > 200) { // Limit to 200 rolling points for database size optimization
            historicalTrendPointDao.deleteOldestTrendPoints(count - 200)
        }
    }

    suspend fun clearHistory() {
        dtcDao.deleteAll()
        frequentlyAccessedDtcDao.deleteAllFrequentlyAccessed()
        historicalTrendPointDao.deleteAllTrendPoints()
    }

    fun getPairedDevices(): List<BtDevice> = obdManager.getPairedDevices()

    fun connectToDevice(address: String) {
        obdManager.connectToDevice(address)
    }

    fun setSimulationMode(enabled: Boolean) {
        obdManager.setSimulationMode(enabled)
    }

    fun setEcoMode(enabled: Boolean) {
        obdManager.setEcoMode(enabled)
    }

    fun clearTerminalLogs() {
        obdManager.clearTerminalLogs()
    }

    fun sendCustomTerminalCommand(cmd: String) {
        obdManager.sendCustomTerminalCommand(cmd)
    }

    fun applyThrottle(throttle: Float) {
        obdManager.applyThrottle(throttle)
    }

    fun toggleEnginePower() {
        obdManager.toggleEnginePower()
    }

    fun injectFaultCode(fault: DtcInfo) {
        obdManager.injectFaultCode(fault)
    }

    fun toggleDoor(doorIndex: Int) {
        obdManager.toggleDoor(doorIndex)
    }

    fun setWindowPosition(windowIndex: Int, positionPercent: Int) {
        obdManager.setWindowPosition(windowIndex, positionPercent)
    }

    fun toggleCentralLock() {
        obdManager.toggleCentralLock()
    }

    fun resetOilLife() {
        obdManager.resetOilLife()
    }

    suspend fun clearFaultCodes() {
        if (isSimulation.value) {
            obdManager.clearFaultCodesSimulator()
        } else {
            obdManager.clearObdFaultCodesReal()
        }
        // Save the clear operation to history
        dtcDao.clearActiveRecords()
    }

    suspend fun saveDtcRecord(record: DtcRecord) {
        dtcDao.insertRecord(record)
    }

    fun disconnect() {
        obdManager.disconnect()
    }

    val isBleScanning: StateFlow<Boolean> = obdManager.isBleScanning
    val discoveredBleDevices: StateFlow<List<BtDevice>> = obdManager.discoveredBleDevices

    fun startBleScan() {
        obdManager.startBleScan()
    }

    fun stopBleScan() {
        obdManager.stopBleScan()
    }

    fun connectToBleDevice(address: String) {
        obdManager.connectToBleDevice(address)
    }
}
