package pozzo.apps.travelweather.forecast.model

import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

import pozzo.apps.travelweather.forecast.ForecastHelper

class Forecast {
    var date: String? = null
    var text: String? = null//todo convert this to an enum maybe
    var high: Int = 0
    var low: Int = 0

    val icon: BitmapDescriptor?
        get() {
            return ForecastHelper.forecastIcon(this)
        }
}
