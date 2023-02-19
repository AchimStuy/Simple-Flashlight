package com.simplemobiletools.flashlight.helpers

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import com.simplemobiletools.commons.extensions.showErrorToast
import com.simplemobiletools.commons.helpers.isTiramisuPlus
import com.simplemobiletools.flashlight.extensions.config
import com.simplemobiletools.flashlight.models.Events
import org.greenrobot.eventbus.EventBus

internal class CameraFlash(
    private val context: Context,
    private var cameraTorchListener: CameraTorchListener? = null,
) {
    private val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var cameraId: String? = null

    private var brightness: Int = 0
    private var darkness: Int = 0

    private var enableFlashlight: Boolean = false
    val torchBrightness = Runnable {
        while (enableFlashlight) {
            manager.setTorchMode(cameraId!!, true);
            Thread.sleep(brightness.toLong())
            manager.setTorchMode(cameraId!!, false);
            Thread.sleep(darkness.toLong())
        }
    }

    init {
        try {
            cameraId = manager.cameraIdList[0] ?: "0"
        } catch (e: Exception) {
            context.showErrorToast(e)
        }
    }

    fun toggleFlashlight(enable: Boolean) {
        try {
            if (supportsBrightnessControl()) {
                enableFlashlight = enable
                if (enable) {
                    val brightnessLevel = getCurrentBrightnessLevel()
                    changeTorchBrightness(brightnessLevel)
                    Thread(torchBrightness).start()
                }
            } else {
                manager.setTorchMode(cameraId!!, enable)
            }
            cameraTorchListener?.onTorchEnabled(enable)
        } catch (e: Exception) {
            context.showErrorToast(e)
            val mainRunnable = Runnable {
                EventBus.getDefault().post(Events.CameraUnavailable())
            }
            Handler(context.mainLooper).post(mainRunnable)
        }
    }

    fun changeTorchBrightness(level: Int) {
        if (isTiramisuPlus()) {
            manager.turnOnTorchWithStrengthLevel(cameraId!!, level)
        } else {
            brightness = level
            darkness = getMaximumBrightnessLevel() - level
        }
    }

    fun getMaximumBrightnessLevel(): Int {
        return if (isTiramisuPlus()) {
            val characteristics = manager.getCameraCharacteristics(cameraId!!)
            characteristics.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: MIN_BRIGHTNESS_LEVEL
        } else {
            4
        }
    }

    fun supportsBrightnessControl(): Boolean {
        val maxBrightnessLevel = getMaximumBrightnessLevel()
        return maxBrightnessLevel > MIN_BRIGHTNESS_LEVEL
    }

    fun getCurrentBrightnessLevel(): Int {
        var brightnessLevel = context.config.brightnessLevel
        if (brightnessLevel == DEFAULT_BRIGHTNESS_LEVEL) {
            brightnessLevel = getMaximumBrightnessLevel()
        }
        return brightnessLevel
    }

    fun initialize() {
    }

    fun unregisterListeners() {
    }

    fun release() {
        cameraTorchListener = null
    }
}
