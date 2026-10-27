package com.developerfour.lastoption

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.Filterable
import com.google.android.gms.tasks.Tasks
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient

class PlacesAutoCompleteAdapter(
    context: Context,
    private val placesClient: PlacesClient
) : ArrayAdapter<String>(context, android.R.layout.simple_dropdown_item_1line), Filterable {

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val filterResults = FilterResults()

                if (constraint == null || constraint.isEmpty()) {
                    return filterResults
                }

                // Make the API request to get autocomplete predictions
                val autocompletePredictions = getAutocompletePredictions(constraint.toString())
                filterResults.values = autocompletePredictions
                filterResults.count = autocompletePredictions.size

                return filterResults
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                if (results != null && results.count > 0) {
                    clear()
                    addAll(results.values as List<String>)
                    notifyDataSetChanged()
                }
            }
        }
    }

    private fun getAutocompletePredictions(query: String): List<String> {
        val predictionList = mutableListOf<String>()
        val placeFields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS)

        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .setTypeFilter(TypeFilter.ESTABLISHMENT)
            .build()

        try {
            val response = Tasks.await(placesClient.findAutocompletePredictions(request))
            val predictions = response.autocompletePredictions

            for (prediction in predictions) {
                predictionList.add(prediction.getPrimaryText(null).toString())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return predictionList
    }
}
