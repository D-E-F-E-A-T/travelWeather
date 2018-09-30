package pozzo.apps.travelweather.direction

import org.mockito.Mockito
import pozzo.apps.travelweather.direction.google.GoogleDirection
import pozzo.apps.travelweather.map.parser.MapPointCreator

class DirectionModuleFake : DirectionModule() {
    val directionBusiness by lazy { Mockito.mock(DirectionBusiness::class.java)!! }
    override fun directionBusiness(googleDirection: GoogleDirection, directionLineBusiness: DirectionLineBusiness,
                                   mapPointCreator: MapPointCreator) = directionBusiness

    val directionWeatherFilter by lazy { Mockito.mock(DirectionWeatherFilter::class.java)!! }
    override fun directionWeatherFilter() = directionWeatherFilter
}
