package gr.andreasagap.moto.communication

import android.net.wifi.p2p.WifiP2pDevice
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class MotorCommunicationViewModel @Inject constructor(
) : ViewModel() {

    private val _startSearch = MutableLiveData<Boolean>()
    val startSearch: LiveData<Boolean> = _startSearch

    private val _disconnect = MutableLiveData<Boolean>()
    val disconnect: LiveData<Boolean> = _disconnect

    private val _isPairing = MutableLiveData<Boolean>()
    val isPairing: LiveData<Boolean> = _isPairing

    private var isServer = false

    private val _peers = MutableLiveData<List<WifiP2pDevice>>()
    val peers: LiveData<List<WifiP2pDevice>> = _peers

    private val _clientName = MutableLiveData<String>()
    val clientName: LiveData<String> = _clientName

    private val _selectedPeer = MutableLiveData<WifiP2pDevice>()
    val selectedPeer: LiveData<WifiP2pDevice> = _selectedPeer
    fun setPeers(list: MutableCollection<WifiP2pDevice>) {
        viewModelScope.launch(Dispatchers.IO) {
            val l = mutableListOf<WifiP2pDevice>()
            l.addAll(list)
            _peers.postValue(l)
        }
    }

    fun setSelectedPeer(position: Int) {
        _selectedPeer.postValue(_peers.value?.get(position))
    }

    fun clearElements() {
        _peers.postValue(emptyList())
        _selectedPeer.postValue(WifiP2pDevice())
    }

    fun startSearch() {
        _startSearch.postValue(startSearch.value?.not() ?: true)
    }

    fun setDeviceName(clientName: String) {
        _clientName.postValue(clientName)
    }

    fun setPairing(server: Boolean) {
        _isPairing.postValue(true)
        this.isServer = server
    }

    fun closeConnection(){
        _disconnect.postValue(disconnect.value?.not() ?: true)
    }

    fun stopSearch() {
        _startSearch.postValue(false)
    }


}