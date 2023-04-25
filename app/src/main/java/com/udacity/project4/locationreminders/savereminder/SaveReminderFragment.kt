package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
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

			if (isForegroundAndBackgroundLocationPermissionApproved()) {
				checkDeviceLocationSettingsAndStartGeofence()
			} else {
				requestForegroundAndBackgroundLocationPermissions()
			}
		}

		binding.selectLocation.setOnClickListener {
			_viewModel.navigationCommand.value =
				NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
		}
	}

	@RequiresApi(Build.VERSION_CODES.Q)
	private fun requestForegroundAndBackgroundLocationPermissions() {
		if (isForegroundAndBackgroundLocationPermissionApproved()) return

		var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

		val resultCode = when {
			runningQOrLater -> {
				// this provides the result[BACKGROUND_LOCATION_PERMISSION_INDEX]
				permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
				REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
			}
			else            -> REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
		}

		requestPermissions(permissionsArray, resultCode)
	}

	@RequiresApi(Build.VERSION_CODES.Q)
	private fun isForegroundAndBackgroundLocationPermissionApproved(): Boolean {
		val foregroundLocationApproved = (
				PackageManager.PERMISSION_GRANTED == checkSelfPermission(
					requireContext(),
					Manifest.permission.ACCESS_FINE_LOCATION
				))
		val backgroundPermissionApproved =
			if (runningQOrLater) {
				PackageManager.PERMISSION_GRANTED == checkSelfPermission(
					requireContext(),
					Manifest.permission.ACCESS_BACKGROUND_LOCATION
				)
			} else {
				true
			}
		return foregroundLocationApproved && backgroundPermissionApproved
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)
		if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
			// We don't rely on the result code, but just check the location setting again
			checkDeviceLocationSettingsAndStartGeofence(isResolved = false)
		}
	}

	@SuppressLint("VisibleForTests")
	private fun checkDeviceLocationSettingsAndStartGeofence(isResolved: Boolean = true) {
		val locationRequest = LocationRequest.create().apply {
			priority = LocationRequest.PRIORITY_LOW_POWER
		}
		val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
		val settingsClient = LocationServices.getSettingsClient(requireActivity())
		val locationSettingsResponseTask =
			settingsClient.checkLocationSettings(builder.build())
		locationSettingsResponseTask.addOnFailureListener { exception ->
			if (exception is ResolvableApiException && isResolved) {
				// Location settings are not satisfied, but this can be fixed
				// by showing the user a dialog.
				try {
					// Show the dialog by calling startResolutionForResult(),
					// and check the result in onActivityResult().
					exception.startResolutionForResult(
						requireActivity(),
						REQUEST_TURN_DEVICE_LOCATION_ON
					)
				} catch (sendEx: IntentSender.SendIntentException) {
					Log.d("xxx", "Error getting location settings resolution: " + sendEx.message)
				}
			} else {
				Snackbar.make(
					binding.root,
					R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
				).setAction(android.R.string.ok) {
					checkDeviceLocationSettingsAndStartGeofence()
				}.show()
			}
		}
		locationSettingsResponseTask.addOnCompleteListener {
			if (it.isSuccessful) {
				val reminderDataItem = ReminderDataItem(
					title = _viewModel.reminderTitle.value,
					description = _viewModel.reminderDescription.value,
					location = _viewModel.reminderSelectedLocationStr.value,
					latitude = _viewModel.latitude.value,
					longitude = _viewModel.longitude.value
				)
				validateDataThenAddGeofencingRequestAndSaveToLocal(reminderDataItem)
			}
		}
	}

	override fun onRequestPermissionsResult(
		requestCode: Int,
		permissions: Array<String>,
		grantResults: IntArray
	) {
		Log.d("xxxx", "onRequestPermissionResult")

		if (
			grantResults.isEmpty() ||
			grantResults[0] == PackageManager.PERMISSION_DENIED ||
			(requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
					grantResults[1] ==
					PackageManager.PERMISSION_DENIED)
		) {
			// Permission denied.
			Snackbar.make(
				binding.root,
				R.string.permission_denied_explanation, Snackbar.LENGTH_INDEFINITE
			)
				.setAction(R.string.settings) {
					// Displays App settings screen.
					startActivity(Intent().apply {
						action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
						data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
						flags = Intent.FLAG_ACTIVITY_NEW_TASK
					})
				}.show()
		} else {
			checkDeviceLocationSettingsAndStartGeofence()
		}
	}

	private fun validateDataThenAddGeofencingRequestAndSaveToLocal(reminderDataItem: ReminderDataItem) {
		if (_viewModel.validateEnteredData(reminderDataItem)) {
			addGeofencingRequest(reminderDataItem)
		}
		_viewModel.validateAndSaveReminder(reminderDataItem)
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

	override fun onDestroy() {
		super.onDestroy()
		//make sure to clear the view model after destroy, as it's a single view model.
		_viewModel.onClear()
	}

	companion object {
		const val GEOFENCE_RADIUS_IN_METERS = 100f
		const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 11
		const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 12
		const val REQUEST_TURN_DEVICE_LOCATION_ON = 13
		internal const val ACTION_GEOFENCE_EVENT = "project4.geofenceEventTracking"
	}
}
