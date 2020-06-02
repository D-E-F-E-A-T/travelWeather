package pozzo.apps.travelweather.map.parser

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import pozzo.apps.travelweather.core.CoroutineSettings
import pozzo.apps.travelweather.forecast.model.DayTime
import pozzo.apps.travelweather.forecast.model.Route
import pozzo.apps.travelweather.forecast.model.point.WeatherPoint
import java.util.*
import java.util.concurrent.CancellationException
import kotlin.collections.ArrayList

//TODO any readability improvement I can make here?
class WeatherPointsAdapter(private val weatherPointsData: MutableLiveData<Channel<WeatherPoint>>) {
    companion object {
        private const val TWO_HOURS = 2L * 60L * 60L * 1000L
    }

    private var cachedWeatherPoints: ArrayList<WeatherPoint>? = null
    private var job: Job? = null
    private lateinit var date: Calendar

    fun updateWeatherPoints(dayTime: DayTime, route: Route) {
        job?.cancel(CancellationException("Die!"))
        job = GlobalScope.launch(CoroutineSettings.background) {
            val weatherPoints = ArrayList<WeatherPoint>()
            val weatherPointsChannel = setup(dayTime)

            try {
                weatherPointsData.postValue(weatherPointsChannel)
                for (it in route.weatherPoints) {
                    inLoop(it, weatherPointsChannel)
                    weatherPoints.add(it)
                }
                cachedWeatherPoints = weatherPoints
            } catch (e: CancellationException) {
                route.weatherPoints.cancel()
            } finally {
                cleanup(weatherPointsChannel)
            }
        }
    }

    private fun setup(dayTime: DayTime): Channel<WeatherPoint> {
        date = dayTime.toCalendar()
        return Channel<WeatherPoint>(1)
    }

    private suspend fun inLoop(weatherPoint: WeatherPoint, weatherPointsChannel: Channel<WeatherPoint>) {
        weatherPoint.date = date
        date = GregorianCalendar().apply {
            timeInMillis = date.timeInMillis + TWO_HOURS
        }
        weatherPointsChannel.send(weatherPoint)
    }

    private fun cleanup(weatherPointsChannel: Channel<WeatherPoint>) {
        weatherPointsChannel.close()
    }

    fun refreshRoute(dayTime: DayTime) {
        GlobalScope.launch(CoroutineSettings.background) {
            if (job?.isActive == true) job?.join()
            if (cachedWeatherPoints == null) return@launch

            job?.cancel()
            job = GlobalScope.launch(CoroutineSettings.background) {
                val weatherPointsChannel = setup(dayTime)

                try {
                    weatherPointsData.postValue(weatherPointsChannel)
                    cachedWeatherPoints?.forEach {
                        inLoop(it, weatherPointsChannel)
                    }
                } finally {
                    cleanup(weatherPointsChannel)
                }
            }
        }
    }
}