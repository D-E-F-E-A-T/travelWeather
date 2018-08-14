package pozzo.apps.travelweather.map.parser

import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import pozzo.apps.travelweather.forecast.model.Weather
import pozzo.apps.travelweather.map.model.Address

class WeatherToMapPointParserTest {
    private lateinit var parser: WeatherToMapPointParser

    @Before fun setup() {
        parser = WeatherToMapPointParser()
    }

    @Test fun nullWhenEmpty() {
        val weather = Weather("")
        assertNull(parser.parse(weather))
    }

    @Test fun assertParsing() {
        val weather = Weather("", Address(1.0, 2.0, "addr"))

        val point = parser.parse(weather)
        assertEquals(LatLng(1.0, 2.0), point!!.position)
    }
}