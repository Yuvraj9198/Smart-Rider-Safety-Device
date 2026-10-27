package com.developerfour.lastoption

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentContainerView
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.maps.android.PolyUtil
import org.json.JSONObject
import java.io.OutputStream
import java.util.*

class NavigationActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var tts: TextToSpeech
    private var userCurrentLocation: LatLng? = null
    private lateinit var destinationInput: AutoCompleteTextView
    private lateinit var startNavigationBtn: Button
    private lateinit var instructionsTextView: TextView
    private var currentRoute: List<LatLng> = emptyList()
    private var selectedDestination: LatLng? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation)
        // Get the Bluetooth output stream from MainActivity

        // Initialize UI components
        destinationInput = findViewById(R.id.destination_input)
        startNavigationBtn = findViewById(R.id.start_navigation)
        instructionsTextView = findViewById(R.id.navigation_steps)

        // Initialize Google Map
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapView) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Initialize Fused Location Client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Check and request location permissions
        checkLocationPermissions()

        // Initialize Text-to-Speech
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.US
            }
        }

        // Initialize Google Places API securely
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, getString(R.string.google_maps_key))
        }

        // Setup autocomplete for destination input
        setupAutocomplete()

        // Set up button click listener for navigation
        startNavigationBtn.setOnClickListener {
            if (userCurrentLocation == null) {
                getUserLocation()
                Toast.makeText(this, "Fetching location... Please try again.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedDestination == null) {
                Toast.makeText(this, "Please select a destination", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, NavigationActivity2::class.java).apply {
                putExtra("destination", selectedDestination)
            }
            startActivity(intent)
        }

    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true
    }

    private fun checkLocationPermissions() {
        if (isLocationPermissionGranted()) {
            if (isLocationEnabled()) {
                getUserLocation()
            } else {
                Toast.makeText(this, "Please enable location services", Toast.LENGTH_SHORT).show()
            }
        } else {
            requestLocationPermissions()
        }
    }

    private fun getUserLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    userCurrentLocation = LatLng(location.latitude, location.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userCurrentLocation!!, 14f))
                    mMap.addMarker(MarkerOptions().position(userCurrentLocation!!).title("Current Location"))
                } else {
                    requestNewLocationData() // Fetch a new location if lastLocation is null
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to get last location", Toast.LENGTH_SHORT).show()
                requestNewLocationData()
            }
        } else {
            requestLocationPermissions()
        }
    }

    private fun requestNewLocationData() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(3000)
            .setMaxUpdates(1)
            .build()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult.lastLocation?.let { location ->
                        userCurrentLocation = LatLng(location.latitude, location.longitude)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userCurrentLocation!!, 14f))
                        mMap.addMarker(MarkerOptions().position(userCurrentLocation!!).title("Current Location"))
                    }
                }
            }, Looper.getMainLooper())
        }
    }


    private fun isLocationPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun setupAutocomplete() {
        destinationInput.setOnClickListener {
            val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)
            val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields).build(this)
            startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE)
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == AUTOCOMPLETE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            val place = Autocomplete.getPlaceFromIntent(data)
            destinationInput.setText(place.name)
            selectedDestination = place.latLng
            selectedDestination?.let {
                mMap.addMarker(MarkerOptions().position(it).title("Destination"))
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(it, 14f))
            }
        }
    }

    private fun startNavigation(destination: LatLng) {
        if (userCurrentLocation == null) {
            getUserLocation()
            Toast.makeText(this, "Fetching location... Please try again.", Toast.LENGTH_SHORT).show()
            return
        }

        // Ensure fragment_container exists before replacing it
        val fragmentContainer = findViewById<FrameLayout>(R.id.fragment_container)
        if (fragmentContainer == null) {
            Toast.makeText(this, "Fragment container not found!", Toast.LENGTH_SHORT).show()
            return
        }

        // Make fragment container visibl
        // Replace fragment_container with NavigationFragment


    }



    private fun fetchRoute(url: String) {
        val requestQueue = Volley.newRequestQueue(this)
        val request = StringRequest(Request.Method.GET, url, { response ->
            val jsonObject = JSONObject(response)
            val routes = jsonObject.getJSONArray("routes")

            if (routes.length() == 0) {
                Toast.makeText(this, "No route found", Toast.LENGTH_SHORT).show()
                return@StringRequest
            }

            val polylineOptions = PolylineOptions().width(10f).color(Color.BLUE).geodesic(true)
            val decodedPath = mutableListOf<LatLng>()

            val steps = routes.getJSONObject(0).getJSONArray("legs").getJSONObject(0).getJSONArray("steps")
            for (i in 0 until steps.length()) {
                val points = steps.getJSONObject(i).getJSONObject("polyline").getString("points")
                val decodedPoints = PolyUtil.decode(points)
                polylineOptions.addAll(decodedPoints)
                decodedPath.addAll(decodedPoints)
            }

            // Clear previous polyline before drawing a new one
            mMap.clear()
            mMap.addPolyline(polylineOptions)
            currentRoute = decodedPath

        }, { error ->
            Toast.makeText(this, "Error fetching route: ${error.message}", Toast.LENGTH_SHORT).show()
        })

        requestQueue.add(request)
    }


    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val AUTOCOMPLETE_REQUEST_CODE = 2
    }
}
