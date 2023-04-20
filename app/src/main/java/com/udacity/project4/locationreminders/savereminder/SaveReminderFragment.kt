package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import com.udacity.project4.utils.showSnackBarDirectToSettings
import org.koin.android.ext.android.inject

@SuppressLint("UnspecifiedImmutableFlag")
class SaveReminderFragment : BaseFragment() {

    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding

    private lateinit var geofencingClient: GeofencingClient
    private val runningQOrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel

        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this

        geofencingClient = LocationServices.getGeofencingClient(requireContext())

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value

            val reminderDataItem =
                ReminderDataItem(title, description, location, latitude, longitude)

            if (isAccessBackgroundLocationGranted()) {
                validateDataThenAddGeofencingRequestAndSaveToLocal(reminderDataItem)
            } else {
                requestAccessBackgroundLocationPermission()
            }
        }
        binding.selectLocation.setOnClickListener {
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }
    }

    private fun validateDataThenAddGeofencingRequestAndSaveToLocal(reminderDataItem: ReminderDataItem) {
        if (_viewModel.validateEnteredData(reminderDataItem)) {
            addGeofencingRequest(reminderDataItem)
        }
        _viewModel.validateAndSaveReminder(reminderDataItem)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestAccessBackgroundLocationPermission() {
        requestPermissions(
            arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
            REQUEST_ACCESS_BACKGROUND_LOCATION_PERMISSION
        )
    }

    @SuppressLint("VisibleForTests", "MissingPermission")
    private fun addGeofencingRequest(reminderDataItem: ReminderDataItem) {

        val geofence = Geofence.Builder()
            .setRequestId(reminderDataItem.id)
            .setCircularRegion(
                reminderDataItem.latitude ?: 0.0,
                reminderDataItem.longitude ?: 0.0,
                GEOFENCE_RADIUS_IN_METERS
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .addGeofence(geofence)
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .build()

        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun isAccessBackgroundLocationGranted(): Boolean {
        return if (runningQOrLater) {
            checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_ACCESS_BACKGROUND_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(requireContext(), "Please press save again!", Toast.LENGTH_SHORT).show()
            } else {
                showSnackBarDirectToSettings(binding.root, requireContext(), "You must be enable access background permission to use this function!")
                return
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    companion object {
        const val GEOFENCE_RADIUS_IN_METERS = 100f
        const val REQUEST_ACCESS_BACKGROUND_LOCATION_PERMISSION = 11
        internal const val ACTION_GEOFENCE_EVENT = "project4.geofenceEventTracking"
    }
}
