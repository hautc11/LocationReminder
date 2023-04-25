package com.udacity.project4.locationreminders.savereminder.selectreminderlocation

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var mGoogleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var pointMarker: Marker? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        val mapFragment = childFragmentManager.findFragmentById(R.id.mapView) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.btnSave.setOnClickListener {
            onLocationSelected()
        }

        return binding.root
    }

    private fun onLocationSelected() {
        findNavController().popBackStack()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onMapReady(googleMap: GoogleMap) {
        mGoogleMap = googleMap
        setMapStyle()
        setPoiClick()
        if (isFineLocationAndAccessCoarseLocationGranted()) {
            enableMyLocationFunction()
            moveCameraToCurrentLocationOfUser()
        } else {
            requestFineLocationAndAccessCoarseLocationPermission()
        }
    }

    private fun isFineLocationAndAccessCoarseLocationGranted(): Boolean {
        val isFineLocationGranted = checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val isAccessCoarseLocationGranted = checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return isFineLocationGranted && isAccessCoarseLocationGranted
    }


    @SuppressLint("MissingPermission")
    private fun enableMyLocationFunction() {
        mGoogleMap.isMyLocationEnabled = true
    }

    @SuppressLint("MissingPermission")
    private fun moveCameraToCurrentLocationOfUser() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val latLng = LatLng(location.latitude, location.longitude)
                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestFineLocationAndAccessCoarseLocationPermission() {
        val mPermissionArray = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        requestPermissions(mPermissionArray, REQUEST_FINE_LOCATION_AND_ACCESS_COARSE_LOCATION_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_FINE_LOCATION_AND_ACCESS_COARSE_LOCATION_CODE) {
            if (grantResults.isNotEmpty() && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                enableMyLocationFunction()
                moveCameraToCurrentLocationOfUser()
            } else {
                return
            }
        }
    }

    private fun setPoiClick() {
        mGoogleMap.setOnMapLongClickListener {
            pointMarker?.remove()

            pointMarker = mGoogleMap.addMarker(
                MarkerOptions()
                    .position(it)
                    .title("My Selected Location")
            )

            pointMarker?.showInfoWindow()

            _viewModel.apply {
                reminderSelectedLocationStr.value = "My Selected Location"
                latitude.value = it.latitude
                longitude.value = it.longitude
            }
        }

        mGoogleMap.setOnPoiClickListener { poi ->
            pointMarker?.remove()

            pointMarker = mGoogleMap.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name)
            )
            pointMarker?.showInfoWindow()

            _viewModel.apply {
                reminderSelectedLocationStr.value = poi.name
                latitude.value = poi.latLng.latitude
                longitude.value = poi.latLng.longitude
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        // TODO: Change the map type based on the user's selection.
        R.id.normal_map -> {
            mGoogleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            mGoogleMap.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            mGoogleMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            mGoogleMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun setMapStyle() {
        try {
            val success = mGoogleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    context,
                    R.raw.map_style
                )
            )

            if (!success) {
                Log.e(TAG, "Style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Can't find style. Error: ", e)
        }
    }

    companion object {
        const val TAG = "SelectLocationFragment"
        const val REQUEST_FINE_LOCATION_AND_ACCESS_COARSE_LOCATION_CODE = 1
    }
}
