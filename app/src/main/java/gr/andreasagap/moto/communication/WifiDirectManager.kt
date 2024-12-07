package gr.andreasagap.moto.communication

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import gr.andreasagap.moto.communication.extensions.noNull
import gr.andreasagap.moto.communication.presentation.MainActivity
import gr.andreasagap.moto.communication.threads.CallService


class WiFiDirectManager(private val context: Context) {
    private val wifiP2pManager: WifiP2pManager =
        context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    private val channel: WifiP2pManager.Channel =
        wifiP2pManager.initialize(context, context.mainLooper, null)
    private var broadcastReceiver: WifiDirectBroadcastReceiver? = null

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun startPeerDiscovery() {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.NEARBY_WIFI_DEVICES
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        wifiP2pManager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                showToast("Discovery started")
            }

            override fun onFailure(reason: Int) {
                showToast("Discovery failed")
            }
        })
    }

    fun disconnect() {

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        wifiP2pManager.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d("tag", "removeGroup onSuccess")
            }

            override fun onFailure(reason: Int) {
                Log.d("tag", "removeGroup onFailure -$reason")
            }
        })


    }

    fun connectToPeer(device: WifiP2pDevice?) {
        if (device?.deviceAddress.noNull().isEmpty()) return
        val config = WifiP2pConfig().apply {
            deviceAddress = device?.deviceAddress
            wps.setup = WpsInfo.PBC
        }

        channel.also { channel ->
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            wifiP2pManager.connect(channel, config, object : WifiP2pManager.ActionListener {

                override fun onSuccess() {

                }

                override fun onFailure(reason: Int) {
                    //failure logic
                }
            }
            )
        }
    }

    fun stopPeerDiscovery() {
        wifiP2pManager.stopPeerDiscovery(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
            }

            override fun onFailure(reason: Int) {
            }
        })
    }

    fun cleanup() {
        broadcastReceiver?.let {
            context.unregisterReceiver(it)
        }
    }

    fun getDeviceName(info: WifiP2pInfo, device: WifiP2pDevice?, callback: (String) -> Unit) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        if ((info.groupFormed || info.isGroupOwner) && device == null) {
            try {
                if (info.groupFormed && info.isGroupOwner) {

                    wifiP2pManager.requestGroupInfo(channel) { group ->
                        if (group?.clientList?.isNotEmpty() == true) {
                            val clientDevice = group.clientList.first()
                            val clientName = clientDevice.deviceName
                            callback(clientName.noNull())
                        }
                    }
                } else {
                    wifiP2pManager.requestGroupInfo(channel) { group ->
                        callback(group?.owner?.deviceName.noNull())
                    }
                }
            } catch (_: SecurityException) {

            }
        }
    }

    fun initListener() {
        val filter = IntentFilter()

        // Add desired Wi-Fi Direct broadcasts
        filter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        filter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        channel.also { channel ->
            broadcastReceiver =
                WifiDirectBroadcastReceiver(wifiP2pManager, channel, context as MainActivity)
        }
        broadcastReceiver?.also { receiver ->
            (context as MainActivity).registerReceiver(receiver, filter)
        }
    }

    fun init(connectionListener: WifiP2pManager.ConnectionInfoListener) {
        if (broadcastReceiver == null)
            wifiP2pManager.requestConnectionInfo(channel, connectionListener)
    }
}