package com.developerfour.lastoption

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.PolyUtil
import org.json.JSONObject
import java.io.IOException
import java.io.OutputStream
import java.net.URL
import java.util.*
import kotlin.concurrent.thread

class NavigationActivity2 : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var outputStream: OutputStream? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var tts: TextToSpeech
    private lateinit var instructionsTextView: TextView

    private var destination: LatLng? = null
    private var currentRoute: List<LatLng> = emptyList()
    private var navigationSteps: MutableList<Pair<LatLng, String>> = mutableListOf()
    private var currentStepIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation2)
        outputStream = MainActivity.bluetoothSocket?.outputStream


        instructionsTextView = findViewById(R.id.navigation_steps)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapView) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.US
            }
        }

        destination = intent.getParcelableExtra("destination")

        if (destination == null) {
            Toast.makeText(this, "Destination not found!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true

        destination?.let {
            mMap.addMarker(MarkerOptions().position(it).title("Destination"))
            fetchRoute(it)
        }
    }

    private fun fetchRoute(destination: LatLng) {
        val apiKey = getString(R.string.google_maps_key)

        getCurrentLocation { userLocation ->
            if (userLocation == null) {
                showToast("Current location not found!")
                return@getCurrentLocation
            }

            val origin = "${userLocation.latitude},${userLocation.longitude}"
            val dest = "${destination.latitude},${destination.longitude}"
            val url = "https://maps.googleapis.com/maps/api/directions/json?origin=$origin&destination=$dest&key=$apiKey"

            thread {
                try {
                    val response = URL(url).readText()
                    val jsonResponse = JSONObject(response)

                    val routes = jsonResponse.optJSONArray("routes")
                    if (routes == null || routes.length() == 0) {
                        showToast("No routes found.")
                        return@thread
                    }

                    val legs = routes.getJSONObject(0).optJSONArray("legs")
                    if (legs == null || legs.length() == 0) {
                        showToast("No legs found in the route.")
                        return@thread
                    }

                    val steps = legs.getJSONObject(0).optJSONArray("steps")
                    if (steps == null || steps.length() == 0) {
                        showToast("No steps found in the route.")
                        return@thread
                    }

                    val parser = NavigationParser()
                    val (parsedRoute, parsedSteps) = parser.parseSteps(steps)

                    runOnUiThread {
                        currentRoute = parsedRoute
                        navigationSteps = parsedSteps.toMutableList()
                        announceStep()
                        startLocationUpdates()
                    }
                } catch (e: Exception) {
                    Log.e("NavigationActivity2", "Error fetching route: ${e.message}")
                    showToast("Failed to fetch route. Please try again.")
                }
            }
        }
    }


    /** Handles navigation step announcements */
    private fun announceStep() {
        if (currentStepIndex < navigationSteps.size) {
            val instruction = navigationSteps[currentStepIndex].second
            instructionsTextView.text = instruction
            tts.speak(instruction, TextToSpeech.QUEUE_FLUSH, null, null)
            sendToESP32(instruction)
        } else {
            instructionsTextView.text = "You have arrived!"
            tts.speak("You have arrived!", TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    private fun sendToESP32(message: String) {
        if (outputStream == null) {
            showToast("Bluetooth not connected!")
            return
        }
        try {
            outputStream?.write((message + "\n").toByteArray())
            showToast("Sent to ESP32: $message")
        } catch (e: IOException) {
            showToast("Failed to send data to ESP32")
        }
    }


    /** Starts location updates to track the user's progress */
    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission required!", Toast.LENGTH_SHORT).show()
            return
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(3000)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val userLocation = LatLng(location.latitude, location.longitude)
                    mMap.clear()
                    mMap.addMarker(MarkerOptions().position(userLocation).title("Current Location"))
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 16f))

                    checkNavigationStatus(userLocation)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    /** Checks if the user is following the navigation route */
    private fun checkNavigationStatus(userLocation: LatLng) {
        if (currentStepIndex < navigationSteps.size) {
            val (stepLocation, instruction) = navigationSteps[currentStepIndex]

            if (PolyUtil.isLocationOnPath(userLocation, currentRoute, true, 15.0)) {
                if (distanceBetween(userLocation, stepLocation) < 20) {
                    currentStepIndex++
                    announceStep()
                }
            } else {
                instructionsTextView.text = "Recalculating route..."
                fetchRoute(destination!!) // Re-fetch the route if the user is off-track
            }
        }
    }

    /** Calculates the distance between two locations */
    private fun distanceBetween(loc1: LatLng, loc2: LatLng): Float {
        val results = FloatArray(1)
        Location.distanceBetween(loc1.latitude, loc1.longitude, loc2.latitude, loc2.longitude, results)
        return results[0]
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        tts.stop()
        tts.shutdown()

        try {
            outputStream?.close()
            showToast("Bluetooth stream closed")
        } catch (e: IOException) {
            Log.e("Bluetooth", "Error closing Bluetooth stream: ${e.message}")
        }
    }

    /** Helper function to show Toast messages safely on the UI thread */
    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun getCurrentLocation(callback: (LatLng?) -> Unit) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            callback(null)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                callback(LatLng(location.latitude, location.longitude))
            } else {
                callback(null)
            }
        }.addOnFailureListener {
            callback(null)
        }
    }

}
