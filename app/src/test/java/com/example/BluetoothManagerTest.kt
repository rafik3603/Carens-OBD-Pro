package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.bluetooth.BluetoothConnectionState
import com.example.bluetooth.BluetoothManager
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BluetoothManagerTest {

    private lateinit var context: Context
    private lateinit var bluetoothManager: BluetoothManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        bluetoothManager = BluetoothManager(context)
    }

    @Test
    fun testInitialConnectionState_isDisconnected() {
        assertEquals(BluetoothConnectionState.Disconnected, bluetoothManager.connectionState.value)
        assertFalse(bluetoothManager.isConnected())
        assertFalse(bluetoothManager.isDiscovering.value)
    }

    @Test
    fun testGetPairedDevices_doesNotThrow() {
        val devices = bluetoothManager.getPairedDevices()
        assertNotNull(devices)
    }

    @Test
    fun testDiscoveryLifecycle() {
        bluetoothManager.startDiscovery()
        bluetoothManager.stopDiscovery()
        assertFalse(bluetoothManager.isDiscovering.value)
    }

    @Test
    fun testDisconnectAndRelease() = runTest {
        bluetoothManager.disconnect()
        assertEquals(BluetoothConnectionState.Disconnected, bluetoothManager.connectionState.value)
        bluetoothManager.release()
    }
}
