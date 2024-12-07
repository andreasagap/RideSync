package gr.andreasagap.moto.communication.broadcasters

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import gr.andreasagap.moto.communication.threads.CallService


class HungUpBroadcaster : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val intentToStopService = Intent(context, CallService::class.java)
        context?.stopService(intentToStopService)
    }
}