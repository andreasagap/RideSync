package gr.andreasagap.moto.communication.presentation.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import gr.andreasagap.moto.communication.MotorCommunicationViewModel
import gr.andreasagap.moto.communication.databinding.FragmentCallBinding

@AndroidEntryPoint
class CallFragment : Fragment() {

    private lateinit var binding: FragmentCallBinding
    private val sharedViewModel: MotorCommunicationViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val fragmentBinding = FragmentCallBinding.inflate(inflater, container, false)
        binding = fragmentBinding
        return fragmentBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        startAnimationDevices()
        initLayout()
    }

    private fun initLayout() {
        binding.closeCallView.setOnClickListener{
            sharedViewModel.closeConnection()
        }
    }

    private fun startAnimationDevices() {
        binding.waveVisualizerPanel.apply {
            updateSpeaking(true)
            updateViewColor(Color.GREEN)
            updateAmplitude(0.5f)
            updateSpeed(-0.1f)
        }

    }
}
