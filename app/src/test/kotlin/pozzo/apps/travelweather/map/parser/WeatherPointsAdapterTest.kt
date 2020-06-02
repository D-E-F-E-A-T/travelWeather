package pozzo.apps.travelweather.map.parser

import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.model.LatLng
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import pozzo.apps.travelweather.forecast.model.*
import pozzo.apps.travelweather.forecast.model.point.WeatherPoint
import pozzo.apps.travelweather.map.model.Address
import java.util.concurrent.CancellationException

class WeatherPointsAdapterTest {
    private val weatherPointsData = mock<MutableLiveData<Channel<WeatherPoint>>>()
    private val weatherPointsAdapter = WeatherPointsAdapter(weatherPointsData)

    @Test fun assertPostingChannel() {
        val weatherPoints = Channel<WeatherPoint>()
        val route = Route(weatherPoints = weatherPoints)

        weatherPointsAdapter.updateWeatherPoints(DayTime(Day.TOMORROW, Time.getDefault()), route)

        verify(weatherPointsData).postValue(any())
    }

    @Test fun assertMultipleRequests() {
        val weatherPoints = Channel<WeatherPoint>()
        val weatherPoints2 = Channel<WeatherPoint>()
        val route = Route(weatherPoints = weatherPoints)
        val route2 = Route(weatherPoints = weatherPoints2)
        val weatherPoint = WeatherPoint(Weather("", emptyList(), Address(LatLng(.0, .0), ""), PoweredBy(0)))

        //TODO Ta com cara que vou ter que refatorar minhas coroutines
//        val scope = TestCoroutineScope()
        weatherPointsAdapter.updateWeatherPoints(DayTime(Day.TOMORROW, Time.getDefault()), route)
        weatherPointsAdapter.updateWeatherPoints(DayTime(Day.TOMORROW, Time.getDefault()), route2)

//        scope.runBlockingTest {
        runBlocking {
            try {
                weatherPoints.send(weatherPoint)
                weatherPoints.close()
                Assert.fail("Should have been canceled")
            } catch (e: CancellationException) {
                //Expected behaviour
            }
            weatherPoints2.send(weatherPoint)
            weatherPoints2.close()
        }
    }

    @Test fun assertMultipleRequestsWithChannelCapacity() {
        val weatherPoints = Channel<WeatherPoint>(1)
        val weatherPoints2 = Channel<WeatherPoint>(1)
        val route = Route(weatherPoints = weatherPoints)
        val route2 = Route(weatherPoints = weatherPoints2)
        val weatherPoint = WeatherPoint(Weather("", emptyList(), Address(LatLng(.0, .0), ""), PoweredBy(0)))

        weatherPointsAdapter.updateWeatherPoints(DayTime(Day.TOMORROW, Time.getDefault()), route)
        weatherPointsAdapter.updateWeatherPoints(DayTime(Day.TOMORROW, Time.getDefault()), route2)

        runBlocking {
            try {
                weatherPoints.send(weatherPoint)
                weatherPoints.close()
                Assert.fail("Should have been canceled")
            } catch (e: CancellationException) {
                //Expected behaviour
            }
            weatherPoints2.send(weatherPoint)
            weatherPoints2.close()
        }
    }
}