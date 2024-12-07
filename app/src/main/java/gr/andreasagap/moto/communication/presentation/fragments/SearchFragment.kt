package gr.andreasagap.moto.communication.presentation.fragments

import android.net.wifi.p2p.WifiP2pDevice
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import gr.andreasagap.moto.communication.MotorCommunicationViewModel
import gr.andreasagap.moto.communication.R
import gr.andreasagap.moto.communication.databinding.FragmentSearchDeviceBinding
import gr.andreasagap.moto.communication.presentation.adapters.ListDeviceActions
import gr.andreasagap.moto.communication.presentation.adapters.ListDeviceAdapter


@AndroidEntryPoint
class SearchFragment : Fragment(), ListDeviceActions {

    private lateinit var binding: FragmentSearchDeviceBinding
    private lateinit var mAdapter: ListDeviceAdapter
    private val sharedViewModel: MotorCommunicationViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val fragmentBinding = FragmentSearchDeviceBinding.inflate(inflater, container, false)
        binding = fragmentBinding
        return fragmentBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        startAnimationDevices()
        initLayout()
        observerViewModel()
        sharedViewModel.startSearch()
    }

    private fun initLayout() {
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        mAdapter = ListDeviceAdapter(emptyList(), this)
        binding.recyclerview.layoutManager = layoutManager
        binding.recyclerview.adapter = mAdapter
        binding.backButton.setOnClickListener{
            sharedViewModel.stopSearch()
            findNavController().popBackStack()
        }
    }

    private fun observerViewModel() {
        sharedViewModel.run {
            peers.observe(viewLifecycleOwner, Observer(::updatePeers))
            isPairing.observe(viewLifecycleOwner, Observer(::isPairing))
        }
    }

    private fun isPairing(isPairing: Boolean) {
        if(isPairing)
            findNavController().navigate(R.id.action_searchFragment_to_callFragment)
    }

    private fun updatePeers(arrayList: List<WifiP2pDevice>) {
        mAdapter.updateList(arrayList)
    }

    private fun getAnimation(s: Long): AlphaAnimation {
        val animation = AlphaAnimation(0.2f, 1f).apply {
            interpolator = DecelerateInterpolator() //add this
            duration = 1000
            startOffset = s
            repeatCount = Animation.INFINITE
            repeatMode = Animation.REVERSE
        }
        return animation
    }

    private fun startAnimationDevices() {
        binding.device1.animation = getAnimation(1000)
        binding.device2.animation = getAnimation(800)
        binding.device3.animation = getAnimation(400)

    }

    override fun onClick(position: Int) {
        sharedViewModel.setSelectedPeer(position)
    }
}
