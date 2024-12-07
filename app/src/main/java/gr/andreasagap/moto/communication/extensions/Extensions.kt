package gr.andreasagap.moto.communication.extensions

import android.app.ActivityManager
import android.app.Service
import android.content.Context

fun String?.noNull(): String{
    if(this == null) return ""
    return this
}

fun Context.isServiceRunning(serviceClass: Class<out Service>) = try {
    (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
        .getRunningServices(Int.MAX_VALUE)
        .any { it.service.className == serviceClass.name }
} catch (e: Exception) {
    false
}