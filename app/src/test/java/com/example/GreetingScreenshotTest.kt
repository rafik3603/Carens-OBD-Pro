package com.example

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.example.data.obd.ObdSensorData
import com.example.ui.dashboard.BatteryVoltageMonitorComponent
import com.example.ui.dashboard.ObdViewModel
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun battery_voltage_monitor_screenshot() {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = ObdViewModel(application)
    val testData = ObdSensorData(
      batteryVoltage = 12.4,
      batteryStateOfCharge = 85,
      isEngineRunning = false
    )

    composeTestRule.setContent {
      MyApplicationTheme(darkTheme = true, dynamicColor = false) {
        BatteryVoltageMonitorComponent(
          data = testData,
          viewModel = viewModel
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/battery_voltage_monitor.png")
  }
}
