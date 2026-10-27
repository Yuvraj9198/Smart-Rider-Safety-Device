package com.developerfour.lastoption

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import org.json.JSONArray
import org.json.JSONObject

class NavigationParser {

    fun parseSteps(steps: JSONArray): Pair<List<LatLng>, MutableList<Pair<LatLng, String>>> {
        val navigationSteps: MutableList<Pair<LatLng, String>> = mutableListOf()
        val currentRoute: MutableList<LatLng> = mutableListOf()

        for (i in 0 until steps.length()) {
            val step = steps.getJSONObject(i)
            val startLocation = step.getJSONObject("start_location")
            val lat = startLocation.getDouble("lat")
            val lng = startLocation.getDouble("lng")
            val instruction = step.getString("html_instructions").replace(Regex("<.*?>"), "")

            navigationSteps.add(Pair(LatLng(lat, lng), instruction))

            // Decode polyline
            val polyline = step.getJSONObject("polyline").getString("points")
            currentRoute += PolyUtil.decode(polyline)
        }

        return Pair(currentRoute, navigationSteps)
    }
}
