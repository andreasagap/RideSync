package gr.andreasagap.moto.communication

import android.Manifest
import android.app.ActivityManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import androidx.core.app.ActivityCompat
import gr.andreasagap.moto.communication.extensions.isServiceRunning
import gr.andreasagap.moto.communication.presentation.MainActivity
import gr.andreasagap.moto.communication.threads.CallService

class WifiDirectBroadcastReceiver(
    private var mManager: WifiP2pManager?,
    private var mChannel: WifiP2pManager.Channel?,
    private var mActivity: MainActivity?
) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION == action) {
            mManager?.apply {
                if (ActivityCompat.checkSelfPermission(
                        mActivity!!,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
                requestPeers(mChannel, mActivity!!.peerListListener)
                Log.e("DEVICE_NAME", "WIFI P2P peers changed called")
            }
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION == action) {
            mManager?.apply {
                val networkInfo =
                    intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)
                if (networkInfo != null && networkInfo.isConnectedOrConnecting) {
                    mActivity?.isConnected = true
                    requestConnectionInfo(mChannel, mActivity!!.connectionListener)
                    mActivity?.setClientDetails(intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_GROUP))
                }
                else {
                    if(context.isServiceRunning(CallService::class.java)){
                        val intentToStopService = Intent(context, CallService::class.java)
                        context.stopService(intentToStopService)
                        resetFlow(context)
                    }
                }
            }

        }
    }

    private fun resetFlow(context: Context) {
        val intent = Intent(context, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent);
        Runtime.getRuntime().exit(0);
    }
}