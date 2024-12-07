package gr.andreasagap.moto.communication.presentation

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import dagger.hilt.android.AndroidEntryPoint
import gr.andreasagap.moto.communication.MotorApplication
import gr.andreasagap.moto.communication.MotorCommunicationViewModel
import gr.andreasagap.moto.communication.R
import gr.andreasagap.moto.communication.WiFiDirectManager
import gr.andreasagap.moto.communication.databinding.ActivityMainBinding
import gr.andreasagap.moto.communication.extensions.isServiceRunning
import gr.andreasagap.moto.communication.extensions.noNull
import gr.andreasagap.moto.communication.threads.CallService
import java.net.InetAddress


const val PORT_USED = 9584

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    var isConnected = false

    private var wifiDirectManager: WiFiDirectManager? = null


    private val viewModel: MotorCommunicationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_MotorCommunication)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        wifiDirectManager = WiFiDirectManager(this)
        wifiDirectManager?.init(connectionListener)
        observerViewModel()
    }

    private fun observerViewModel() {
        val lifecycleOwner = this
        viewModel.run {
            selectedPeer.observe(lifecycleOwner, Observer(::connectToPeer))
            startSearch.observe(lifecycleOwner, Observer(::discoverDevices))
            disconnect.observe(lifecycleOwner, Observer(::disconnect))
        }
    }

    private fun disconnect(flag: Boolean) {
        wifiDirectManager?.disconnect()
    }
    private fun discoverDevices(startSearch: Boolean) {
        if(startSearch)
            wifiDirectManager?.startPeerDiscovery()
        else
            wifiDirectManager?.stopPeerDiscovery()
    }


    private fun connectToPeer(device: WifiP2pDevice?) {
        wifiDirectManager?.connectToPeer(device)
    }

    public override fun onResume() {
        super.onResume()
        MotorApplication().activityResumed()
    }

    public override fun onDestroy() {
        super.onDestroy()
        disconnect(true)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
       // wifiDirectManager?.cleanup()
    }


    val peerListListener = WifiP2pManager.PeerListListener { peerList ->
        val refreshedPeers = peerList.deviceList
        if (viewModel.selectedPeer.value != null) return@PeerListListener

        refreshedPeers?.let {
            if (it != viewModel.peers) {
                viewModel.setPeers(it)
            }
        }
    }


    val connectionListener = WifiP2pManager.ConnectionInfoListener { info ->

        wifiDirectManager?.getDeviceName(info,viewModel.selectedPeer.value){
            viewModel.setDeviceName(it)
        }
        if (info.groupFormed && info.isGroupOwner) {
            viewModel.setPairing(true)
            if (!this.isServiceRunning(CallService::class.java)) {
                val serviceIntent = Intent(this, CallService::class.java)
                serviceIntent.putExtra(CallService.Bundle_isServer, true)
                startService(serviceIntent)
            }
        } else if (info.groupFormed) {
            viewModel.setPairing(false)
            if (!this.isServiceRunning(CallService::class.java)) {
                val serviceIntent = Intent(this, CallService::class.java)
                // String from WifiP2pInfo struct
                val groupOwnerAddress: InetAddress = info.groupOwnerAddress
                serviceIntent.putExtra(CallService.Bundle_Address, groupOwnerAddress.address)
                serviceIntent.putExtra(CallService.Bundle_isServer, false)
                startService(serviceIntent)
            }
        } else {
            wifiDirectManager?.initListener()
        }
    }
    override fun onPause() {
        super.onPause()
        // Unregister since the activity is not visible
        MotorApplication().activityPaused()
    }
    companion object {
        var PERMISSIONS = if (Build.VERSION.SDK_INT >= 33) {
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.NEARBY_WIFI_DEVICES
            )
        } else {
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
            )
        }
    }

    fun setClientDetails(parcelableExtra: WifiP2pGroup?) {
        if (parcelableExtra == null) return
        if(parcelableExtra.clientList.isEmpty()){
            viewModel.setDeviceName(parcelableExtra.owner.deviceName)
        }else {
            viewModel.setDeviceName(parcelableExtra.clientList.toMutableList()[0].deviceName.noNull())
        }
    }
}

