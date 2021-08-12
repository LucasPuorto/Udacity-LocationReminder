package com.udacity.project4.locationreminders.savereminder.selectreminderlocation

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PointOfInterest
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    companion object {
        const val TAG = "SelectLocationFragment"
        private const val REQUEST_LOCATION_PERMISSION = 1001
        private const val LOCATION_PERMISSION_INDEX = 0
    }

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding

    private lateinit var map: GoogleMap

    private var reminderSelectedLocationStr = ""
    private var selectedPOI: PointOfInterest? = null
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)
        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.mbtSaveLocation.setOnClickListener {
            onLocationSelected()
        }
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        // Change the map type based on the user's selection.
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Check if location permissions are granted and if so enable the location data layer.
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isEmpty() || grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED) {
                Snackbar
                    .make(requireView(), R.string.permission_denied_explanation, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.settings) {
                        startActivity(Intent().apply {
                            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, this@SelectLocationFragment.toString())
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    }.show()
            } else if (grantResults.isNotEmpty() && (grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_GRANTED)) {
                enableMyLocation()
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        val latitude = -23.994999483822994
        val longitude = -46.25498303770009
        val latLong = LatLng(latitude, longitude)
        val zoomLevel = 15F

        if (isLocationEnabled()) {
            val locationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())
            val lastLocation = locationProviderClient.lastLocation
            lastLocation.addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    // Set the map's camera position to the current location of the device.
                    val lastKnownLocation = task.result
                    if (lastKnownLocation != null) {
                        map.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(
                                    lastKnownLocation.latitude,
                                    lastKnownLocation.longitude
                                ), 15f
                            )
                        )
                    }
                } else {
                    map.moveCamera(
                        CameraUpdateFactory
                            .newLatLngZoom(latLong, 15f)
                    )
                    map.uiSettings.isMyLocationButtonEnabled = false
                }
            }

        } else {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLong, zoomLevel))
        }

        onTouch(googleMap)
        onLongTouch(googleMap)
        setMapStyle(googleMap)
    }

    private fun isLocationEnabled(): Boolean =
        if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            false
        } else {
            map.isMyLocationEnabled = true
            true
        }

    private fun onTouch(googleMap: GoogleMap) {
        googleMap.setOnPoiClickListener { poi ->
            /*If the user changes the decision, this line clears the map.*/
            googleMap.clear()
            latitude = poi.latLng.latitude
            longitude = poi.latLng.longitude
            reminderSelectedLocationStr = poi.name

            googleMap.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name)
            )
        }
    }

    private fun onLongTouch(googleMap: GoogleMap) {
        googleMap.setOnMapLongClickListener { latLng ->
            /*If the user changes the decision, this line clears the map.*/
            googleMap.clear()
            latitude = latLng.latitude
            longitude = latLng.longitude
            reminderSelectedLocationStr = "Custom location"

            googleMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title("Custom location")
            )
        }
    }

    private fun setMapStyle(googleMap: GoogleMap) {
        try {
            // Customize the styling of the base map using a JSON object defined in a raw resource file.
            val success = googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
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

    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        } else {
            map.isMyLocationEnabled = true
            zoomToMyLocation()
            _viewModel.showSnackBar.value = getString(R.string.select_poi)
        }
    }

    //Try get my location
    @SuppressLint("MissingPermission")
    private fun zoomToMyLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        fusedLocationClient.lastLocation.addOnCompleteListener {
            val latitude = it.result.latitude
            val longitude = it.result.longitude
            val latLong = LatLng(latitude, longitude)
            val zoomLevel = 15F
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLong, zoomLevel))
        }
    }

    private fun onLocationSelected() {
        _viewModel.latitude.value = latitude
        _viewModel.longitude.value = longitude
        _viewModel.reminderSelectedLocationStr.value = reminderSelectedLocationStr
        _viewModel.navigationCommand.value = NavigationCommand.Back
    }
}
