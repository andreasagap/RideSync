package gr.andreasagap.moto.communication

import android.app.Application
import dagger.hilt.android.HiltAndroidApp


@HiltAndroidApp
class MotorApplication : Application() {

    fun isActivityVisible(): Boolean {
        return activityVisible
    }

    fun activityResumed() {
        activityVisible = true
    }

    fun activityPaused() {
        activityVisible = false
    }

    private var activityVisible = false

}
