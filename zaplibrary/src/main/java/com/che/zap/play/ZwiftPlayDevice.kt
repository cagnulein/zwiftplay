package com.che.zap.play

import android.util.Log
import com.che.zap.device.ZapConstants.BATTERY_LEVEL_TYPE
import com.che.zap.device.ZapConstants.CONTROLLER_NOTIFICATION_MESSAGE_TYPE
import com.che.zap.device.ZapConstants.EMPTY_MESSAGE_TYPE
import com.che.zap.device.AbstractZapDevice
import com.che.zap.proto.BatteryStatus
import com.che.zap.proto.ControllerNotification
import com.che.zap.utils.Logger
import com.che.zap.utils.toHexString
import timber.log.Timber

class ZwiftPlayDevice : AbstractZapDevice() {

    // you get battery level in a BLE characteristic and via a ZAP message.
    private var batteryLevel = 0

    private var lastButtonState: ControllerNotification? = null
    var ZwiftPlayDevice: String = "ZwiftPlayDevice"

    override fun processEncryptedData(bytes: ByteArray) {
        try {

            Log.d(ZwiftPlayDevice, "Decrypted: ${bytes.toHexString()}")

            val counter = bytes.copyOfRange(0, Int.SIZE_BYTES)
            val payload = bytes.copyOfRange(Int.SIZE_BYTES, bytes.size)

            val data = zapEncryption.decrypt(counter, payload)
            val type = data[0]
            val message = data.copyOfRange(1, data.size)

            when (type) {
                CONTROLLER_NOTIFICATION_MESSAGE_TYPE -> processButtonNotification(ControllerNotification(message))
                EMPTY_MESSAGE_TYPE -> if (LOG_RAW) Log.d(ZwiftPlayDevice,"Empty Message") // expected when nothing happening
                BATTERY_LEVEL_TYPE -> {
                    val notification = BatteryStatus(message)
                    if (batteryLevel != notification.level) {
                        batteryLevel = notification.level
                        Log.d(ZwiftPlayDevice,"Battery level update: $batteryLevel")
                    }
                }
                else -> Log.d(ZwiftPlayDevice,"Unprocessed - Type: ${type.toUByte().toHexString()} Data: ${data.toHexString()}")
            }

        } catch (ex: Exception) {
            Log.d(ZwiftPlayDevice,"Decrypt failed: " + ex.message)
        }
    }

    private fun processButtonNotification(notification: ControllerNotification) {
        if (lastButtonState == null)
            Logger.d(notification.toString())
        else {  
            val diff = notification.diff(lastButtonState!!)
            if (!diff.isNullOrBlank()) // get repeats of the same state
                Logger.d(diff)
        }
        lastButtonState = notification
    }
}

