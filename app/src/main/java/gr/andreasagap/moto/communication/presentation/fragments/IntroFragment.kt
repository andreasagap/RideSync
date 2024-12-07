package gr.andreasagap.moto.communication.presentation.fragments

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import gr.andreasagap.moto.communication.MotorCommunicationViewModel
import gr.andreasagap.moto.communication.R
import gr.andreasagap.moto.communication.animations.typeWriter.TypeWriterListener
import gr.andreasagap.moto.communication.databinding.FragmentIntroBinding
import gr.andreasagap.moto.communication.presentation.MainActivity


@AndroidEntryPoint
class IntroFragment : Fragment() {

    // Binding object instance corresponding to the fragment_flavor.xml layout
    // This property is non-null between the onCreateView() and onDestroyView() lifecycle callbacks,
    // when the view hierarchy is attached to the fragment.
    private lateinit var binding: FragmentIntroBinding

    // Use the 'by activityViewModels()' Kotlin property delegate from the fragment-ktx artifact
    private val sharedViewModel: MotorCommunicationViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val fragmentBinding = FragmentIntroBinding.inflate(inflater, container, false)
        binding = fragmentBinding
        return fragmentBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initLayout()
    }

    override fun onPause() {
        super.onPause()
        with(binding) {
            title.removeAnimation()
            description.removeAnimation()
        }
    }

    private fun initLayout() {
        with(binding) {
            Glide.with(requireContext()).load(R.drawable.motor_intro_bg).into(backgroundImageView)
            title.animateText(getString(R.string.welcome_title))
            description.setInitialText(getString(R.string.welcome_subtitle))
            title.setTypeWriterListener(object : TypeWriterListener {
                override fun onTypingEnd(text: String?) {
                    binding.description.animateText(getString(R.string.welcome_subtitle))
                }
            })
            startBtn.setOnClickListener {
                if (checkWifiStatus()) {
                    if (checkGPSStatus()) {
                        goToSearchList()
                    } else {
                        showDialog(getString(R.string.gps_status_msg)) {
                            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                        }
                    }
                } else {
                    showDialog(getString(R.string.wifi_status_msg)) {
                        startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                    }
                }
            }


        }
    }

    private fun showDialog(message: String, positiveAction: () -> Unit) {
        val dialogBuilder = AlertDialog.Builder(context)
        dialogBuilder.setMessage(message)
            .setCancelable(false)
            .setPositiveButton("Yes") { dialog, id
                ->
                positiveAction()
            }
            .setNegativeButton("No") { dialog, id ->
                dialog.cancel()
            }
        val alert = dialogBuilder.create()
        alert.show()
    }

    private fun checkWifiStatus(): Boolean {
        val wifi =
            this.context?.applicationContext?.getSystemService(Context.WIFI_SERVICE) as WifiManager?
        return wifi?.isWifiEnabled ?: false
    }

    private fun checkGPSStatus(): Boolean {
        val manager: LocationManager =
            this.context?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    /**
     * Navigate to the next screen to choose pickup date.
     */
    private fun goToSearchList() {
        checkPermissionRequired()
    }


    private fun checkPermissionRequired() {
        if (hasPermissions(requireContext(), MainActivity.PERMISSIONS)) {
            findNavController().navigate(R.id.action_introFragment_to_searchFragment)
        } else {
            // request to launch
            permReqLauncher.launch(
                MainActivity.PERMISSIONS
            )
        }

    }

    private fun hasPermissions(context: Context, permissions: Array<String>): Boolean =
        permissions.all {
            ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    private val permReqLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions.entries.all {
                it.value
            }
            if (granted) {
                // navigate to respective screen
            } else {
                // show custom alert
                //Previously Permission Request was cancelled with 'Dont Ask Again',
                // Redirect to Settings after showing Information about why you need the permission
                showPermissionDialog()
            }
        }

    private fun showPermissionDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Permission required")
        builder.setMessage("Some permissions are needed to be allowed to use this app without any problems.")
        builder.setPositiveButton("Grant") { dialog, which ->
            dialog.cancel()
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val uri = Uri.fromParts("package", requireActivity().packageName, null)
            intent.data = uri
            startActivity(intent)
        }
        builder.setNegativeButton("Cancel") { dialog, which ->
            dialog.dismiss()
        }
        builder.show()
    }

}