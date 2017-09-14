package pozzo.apps.travelweather.map.ui

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import com.google.android.gms.maps.model.LatLng
import pozzo.apps.travelweather.core.BaseViewModel
import pozzo.apps.travelweather.location.LocationBusiness
import pozzo.apps.travelweather.location.LocationLiveData

/**
 * @since 13/08/17.
 */
class MapViewModel(application: Application) : BaseViewModel(application) {
    private val locationBusiness: LocationBusiness = LocationBusiness()

    var startPosition: MutableLiveData<LatLng?> = MutableLiveData()
    var finishPosition: MutableLiveData<LatLng?> = MutableLiveData()

    fun currentLocationFabClick() {
        finishPosition.postValue(null)
        startPosition.postValue(getCurrentLocation())
    }

    fun getLiveLocation(): LocationLiveData {
        return LocationLiveData.get(getApplication())
    }

    fun getCurrentLocation(): LatLng? {
        val location = locationBusiness.getCurrentLocation(getApplication())
        if (location != null) {
            return LatLng(location.latitude, location.longitude)
        }

        return null
    }
}
