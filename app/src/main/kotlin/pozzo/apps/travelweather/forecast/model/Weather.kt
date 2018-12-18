package pozzo.apps.travelweather.forecast.model

import com.google.android.gms.maps.model.LatLng
import pozzo.apps.tools.Log
import pozzo.apps.travelweather.map.model.Address

data class Weather(
    val url: String,
    var address: Address? = null) {

    lateinit var forecasts: List<Forecast>

    val latLng: LatLng
        get() = LatLng(address!!.latitude, address!!.longitude)

    fun getForecast(day: Day): Forecast {
        val index = day.index
        return if (index < 0 || index >= forecasts.size) {
            Log.e(ArrayIndexOutOfBoundsException("Forecast out of range, tried: $index, but size was ${forecasts.size}"))
            forecasts.last()
        } else {
            forecasts.getOrNull(day.index) ?: forecasts.last()
        }
    }
}
