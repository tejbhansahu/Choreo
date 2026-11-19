package io.github.tejbhansahu.choreo

/**
 * Measures the device and reports its [DeviceCapabilities].
 *
 * Create a platform instance:
 * - Android: `AndroidDeviceProfiler(context)`
 * - iOS: `IosDeviceProfiler()`
 */
interface DeviceProfiler {
    fun capabilities(): DeviceCapabilities
}
