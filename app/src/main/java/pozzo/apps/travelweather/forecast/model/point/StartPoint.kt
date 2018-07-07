package pozzo.apps.travelweather.forecast.model.point

import android.content.res.Resources
import com.google.android.gms.maps.model.LatLng
import pozzo.apps.travelweather.R
import pozzo.apps.travelweather.common.android.BitmapCreator

class StartPoint(res: Resources, position: LatLng) : MapPoint(
        BitmapCreator.get().fromResource(R.drawable.start_flag),
        R.string.startPosition, position, null,true, false)
