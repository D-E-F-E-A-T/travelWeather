package pozzo.apps.travelweather

import android.app.Application
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import pozzo.apps.travelweather.core.CoroutineSettings
import pozzo.apps.travelweather.core.bugtracker.Bug
import pozzo.apps.travelweather.core.bugtracker.LogBug
import pozzo.apps.travelweather.core.bugtracker.MintBug
import pozzo.apps.travelweather.core.injection.AppComponent
import pozzo.apps.travelweather.core.injection.AppModule
import pozzo.apps.travelweather.core.injection.DaggerAppComponent
import pozzo.apps.travelweather.core.injection.NetworkModule
import pozzo.apps.travelweather.forecast.yahoo.ForecastModuleYahoo

/**
 * TODO
 *
 * Minor bug: select random start position, and then press my current position fab, it will show a
 *  route not found dialog, which it should not.
 *
 * Minor bug: Multiples clicks on curret location make it request multiple times even if the last one
 *  has not finished yet
 *
 * Minor bug: Channel keep flowing after changing route destination.
 *
 * Feedback Lisa: Developer, please add departure times to this app
 * Feedback Paulo: O App podia permitir destinos múltiplos, como o maps permite... assim ficaria mais completo. Estender a precisão para mais dias também seria interessante.
 *
 * Animar as flags voltando para a lateral quando apertar o clear
 * I might need to better hide some keys? (Google maps, Firebase, Mint)
 * I need to reflect about the business layer, does this naming makes sense? It somehow fits too much, I feel like I need more specific namings.
 * Add more days, after after tomorrow, but I need to think in a proper way ot display it
 * Build route if it was triggered when no connection was available (job schedule?)
 * Agendar uma viagem
 * Notificar quando o tempo mudar apos ter agendado uma viagem
 * A distancia entre previsao deve ser dinamica, em uma distancia maior eu nao precio de tantas previsoes.
 * Realizar a separacao early, late...
 * Outra fonte para busca de previsao do tempo
 * Finish Dagger refactoring
 * Increase test coverage
 */
class App : Application() {
    companion object {
        private var appComponent: AppComponent? = null

        fun component(): AppComponent {
            return appComponent!!//It should be initiated
        }

        fun setComponent(component: AppComponent) {
            appComponent = component
        }
    }

    override fun onCreate() {
        super.onCreate()
        initBugTracker()
        initComponent()
        initCoroutines()
    }

    private fun initBugTracker() {
        val bugInstance = if (BuildConfig.DEBUG) {
            LogBug()
        } else {
            MintBug("c315b759")
        }
        Bug.setInstance(bugInstance)
    }

    private fun initComponent() {
        setComponent(DaggerAppComponent.builder()
                .appModule(AppModule(this))
                .networkModule(NetworkModule())
                .forecastModule(ForecastModuleYahoo())
                .build()
        )
    }

    private fun initCoroutines() {
        CoroutineSettings.background = CommonPool
        CoroutineSettings.ui = UI
    }
}

/**
 * Yahoo weather, 2,000 requisicoes por dia.
 * https://developer.yahoo.com/weather/
 * Google direction = 2,500 free directions requests per day and $0.50 USD / 1000 additional requests, up to 100,000 daily.
 * https://developers.google.com/maps/documentation/directions/usage-limits
 */